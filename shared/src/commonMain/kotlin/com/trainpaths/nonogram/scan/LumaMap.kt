package com.trainpaths.nonogram.scan

/** The working resolution a picked image is reduced to before anything else touches it. */
internal const val WORKING_SIDE = 256

/**
 * A greyscale copy of a picked image. [luma] is row-major, one byte per pixel read back as `0..255`,
 * with alpha already composited over white — a transparent PNG of a black icon therefore reads as
 * black on white rather than black on black.
 */
internal class LumaMap(val width: Int, val height: Int, val luma: ByteArray) {

    init {
        require(width > 0 && height > 0) { "empty image: ${width}x$height" }
        require(luma.size == width * height) { "luma is ${luma.size}, expected ${width * height}" }
    }

    operator fun get(x: Int, y: Int): Int = luma[y * width + x].toInt() and 0xFF
}

/** The size [width] x [height] is reduced to so its longest side is at most [maxSide]. Never upscales. */
internal fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    val longest = maxOf(width, height)
    if (longest <= maxSide) return width to height
    val outWidth = (width.toLong() * maxSide / longest).toInt().coerceAtLeast(1)
    val outHeight = (height.toLong() * maxSide / longest).toInt().coerceAtLeast(1)
    return outWidth to outHeight
}

/**
 * Box-downsamples ARGB pixels into a [LumaMap], fed either all at once or a band of rows at a time.
 *
 * The band form is what makes a phone photo survivable: a 12 MP image is 48 MB as one `IntArray`, so
 * the decoder streams it through a buffer of a few rows while the accumulator holds only the (tiny)
 * output.
 */
internal class LumaAccumulator(
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    maxSide: Int,
) {
    private val outSize = scaledSize(sourceWidth, sourceHeight, maxSide)
    val outWidth: Int get() = outSize.first
    val outHeight: Int get() = outSize.second

    private val sums = LongArray(outWidth * outHeight)
    private val counts = IntArray(outWidth * outHeight)

    /** Adds [rowCount] rows of ARGB pixels starting at source row [startY]. */
    fun addRows(pixels: IntArray, startY: Int, rowCount: Int, stride: Int = sourceWidth) {
        for (row in 0 until rowCount) {
            val y = startY + row
            if (y >= sourceHeight) break
            val outY = (y.toLong() * outHeight / sourceHeight).toInt().coerceAtMost(outHeight - 1)
            val rowOffset = row * stride
            for (x in 0 until sourceWidth) {
                val outX = (x.toLong() * outWidth / sourceWidth).toInt().coerceAtMost(outWidth - 1)
                val index = outY * outWidth + outX
                sums[index] += lumaOverWhite(pixels[rowOffset + x]).toLong()
                counts[index]++
            }
        }
    }

    fun build(): LumaMap {
        val luma = ByteArray(sums.size)
        for (i in sums.indices) {
            // An output cell no source pixel landed in can only happen on an upscale, which scaledSize never asks for
            luma[i] = if (counts[i] == 0) 255.toByte() else (sums[i] / counts[i]).toInt().toByte()
        }
        return LumaMap(outWidth, outHeight, luma)
    }
}

/** Rec. 601 luminance of one ARGB pixel, composited over a white background. */
internal fun lumaOverWhite(argb: Int): Int {
    val alpha = (argb ushr 24) and 0xFF
    val red = (argb ushr 16) and 0xFF
    val green = (argb ushr 8) and 0xFF
    val blue = argb and 0xFF
    val luma = (299 * red + 587 * green + 114 * blue) / 1000
    if (alpha == 255) return luma
    return (luma * alpha + 255 * (255 - alpha)) / 255
}
