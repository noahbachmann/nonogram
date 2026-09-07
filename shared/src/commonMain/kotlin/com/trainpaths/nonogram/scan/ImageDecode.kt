package com.trainpaths.nonogram.scan

import androidx.compose.ui.graphics.decodeToImageBitmap

/** Ints per read-back buffer — a 4 MB window, so one band of a very wide image still fits. */
private const val BAND_BUDGET = 1 shl 20

/**
 * Decodes an encoded image and reduces it to a [LumaMap] of at most [maxSide] on its longest side.
 *
 * Pixels are read back a band of rows at a time into one reused buffer rather than all at once: a
 * 12 MP photo is 48 MB as a single `IntArray`, which is a problem on WASM in particular.
 */
internal fun ByteArray.decodeToLumaMap(maxSide: Int = WORKING_SIDE): LumaMap {
    val bitmap = decodeToImageBitmap()
    val width = bitmap.width
    val height = bitmap.height
    require(width > 0 && height > 0) { "decoded image is empty" }

    val accumulator = LumaAccumulator(width, height, maxSide)
    val bandRows = (BAND_BUDGET / width).coerceIn(1, height)
    val buffer = IntArray(width * bandRows)

    var y = 0
    while (y < height) {
        val rows = minOf(bandRows, height - y)
        bitmap.readPixels(buffer, startX = 0, startY = y, width = width, height = rows)
        accumulator.addRows(buffer, startY = y, rowCount = rows, stride = width)
        y += rows
    }
    return accumulator.build()
}
