package com.trainpaths.nonogram.scan

import com.trainpaths.nonogram.classes.MAX_NONOGRAM_SIDE
import com.trainpaths.nonogram.classes.MIN_NONOGRAM_SIDE

/** The longest side a freshly scanned image is proposed at, before the user changes it. */
internal const val DEFAULT_SCAN_SIDE = 30

internal data class ScanOptions(
    val rows: Int,
    val cols: Int,
    /** A cell is filled when its average luma is below this, `0..255`. */
    val threshold: Int,
    val invert: Boolean = false,
)

/**
 * Otsu's method: the luma that best separates the image into two classes. Returned as a threshold
 * for "filled when luma < threshold", so it sits one above Otsu's own level.
 */
internal fun LumaMap.otsuThreshold(): Int {
    val histogram = IntArray(256)
    for (index in luma.indices) histogram[luma[index].toInt() and 0xFF]++
    val total = width.toLong() * height
    if (total == 0L) return 128

    var sum = 0L
    for (value in 0..255) sum += value.toLong() * histogram[value]

    var backgroundWeight = 0L
    var backgroundSum = 0L
    var best = 0.0
    var level = 127
    for (value in 0..255) {
        backgroundWeight += histogram[value]
        if (backgroundWeight == 0L) continue
        val foregroundWeight = total - backgroundWeight
        if (foregroundWeight == 0L) break
        backgroundSum += value.toLong() * histogram[value]
        val backgroundMean = backgroundSum.toDouble() / backgroundWeight
        val foregroundMean = (sum - backgroundSum).toDouble() / foregroundWeight
        val delta = backgroundMean - foregroundMean
        val variance = backgroundWeight.toDouble() * foregroundWeight * delta * delta
        if (variance > best) {
            best = variance
            level = value
        }
    }
    return (level + 1).coerceIn(1, 255)
}

/** Rows and columns that keep the image's aspect ratio, longest side [side]. */
internal fun defaultDimensions(
    width: Int,
    height: Int,
    side: Int = DEFAULT_SCAN_SIDE,
): Pair<Int, Int> {
    val longest = maxOf(width, height)
    val target = side.clampToGridSide()
    if (longest == 0) return target to target
    val cols = (width.toLong() * target / longest).toInt()
    val rows = (height.toLong() * target / longest).toInt()
    return rows.clampToGridSide() to cols.clampToGridSide()
}

internal fun Int.clampToGridSide(): Int = coerceIn(MIN_NONOGRAM_SIDE, MAX_NONOGRAM_SIDE)

/**
 * Quantizes the image into a nonogram grid: every output cell averages the source pixels under it
 * and is filled when that average falls below [ScanOptions.threshold].
 *
 * The sides are clamped, so the result always satisfies `isWellFormedGrid()` however the caller was
 * configured — a ragged or out-of-range grid makes `Nonogram.colClues` throw.
 */
internal fun LumaMap.toGrid(options: ScanOptions): List<List<Int>> {
    val rows = options.rows.clampToGridSide()
    val cols = options.cols.clampToGridSide()

    return List(rows) { row ->
        val top = (row.toLong() * height / rows).toInt()
        val bottom = ((row + 1).toLong() * height / rows).toInt()
            .coerceAtLeast(top + 1)
            .coerceAtMost(height)
        List(cols) { col ->
            val left = (col.toLong() * width / cols).toInt()
            val right = ((col + 1).toLong() * width / cols).toInt()
                .coerceAtLeast(left + 1)
                .coerceAtMost(width)
            val dark = averageLuma(left, top, right, bottom) < options.threshold
            if (dark != options.invert) 1 else 0
        }
    }
}

private fun LumaMap.averageLuma(left: Int, top: Int, right: Int, bottom: Int): Int {
    var total = 0L
    var count = 0
    for (y in top until bottom) {
        for (x in left until right) {
            total += this[x, y]
            count++
        }
    }
    return if (count == 0) 255 else (total / count).toInt()
}
