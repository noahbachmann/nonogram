package com.trainpaths.nonogram.scan

import com.trainpaths.nonogram.classes.MAX_NONOGRAM_SIDE
import com.trainpaths.nonogram.classes.MIN_NONOGRAM_SIDE
import com.trainpaths.nonogram.classes.isWellFormedGrid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue

private fun opaque(gray: Int): Int = argb(255, gray, gray, gray)

/** Builds a map directly, bypassing the decoder, which is the part tests cannot reach. */
private fun lumaMapOf(width: Int, height: Int, value: (x: Int, y: Int) -> Int): LumaMap =
    LumaMap(width, height, ByteArray(width * height) { value(it % width, it / width).toByte() })

class ImageToGridTest {

    @Test
    fun lumaOverWhite_compositesAlpha() {
        assertEquals(0, lumaOverWhite(opaque(0)))
        assertEquals(255, lumaOverWhite(opaque(255)))
        // A fully transparent pixel is background, not black — otherwise every transparent PNG
        // would scan as a solid filled grid.
        assertEquals(255, lumaOverWhite(argb(0, 0, 0, 0)))
        // Half-transparent black over white lands halfway.
        assertEquals(127, lumaOverWhite(argb(128, 0, 0, 0)))
    }

    @Test
    fun scaledSize_preservesAspectAndNeverUpscales() {
        assertEquals(64 to 32, scaledSize(64, 32, maxSide = 256))
        assertEquals(256 to 128, scaledSize(1024, 512, maxSide = 256))
        assertEquals(1 to 256, scaledSize(2, 4000, maxSide = 256))
    }

    @Test
    fun accumulator_boxAveragesTheSourcePixels() {
        val pixels = IntArray(16) { index ->
            val x = index % 4
            val y = index / 4
            opaque(if ((x + y) % 2 == 0) 0 else 255)
        }

        val map = LumaAccumulator(sourceWidth = 4, sourceHeight = 4, maxSide = 2)
            .apply { addRows(pixels, startY = 0, rowCount = 4) }
            .build()

        assertEquals(2, map.width)
        assertEquals(2, map.height)
        for (y in 0 until 2) {
            for (x in 0 until 2) assertEquals(127, map[x, y])
        }
    }

    @Test
    fun accumulator_matchesWhenFedInBands() {
        val pixels = IntArray(8 * 8) { index -> opaque((index * 37) % 256) }

        val wholeImage = LumaAccumulator(8, 8, maxSide = 4)
            .apply { addRows(pixels, startY = 0, rowCount = 8) }
            .build()

        val banded = LumaAccumulator(8, 8, maxSide = 4).apply {
            var y = 0
            while (y < 8) {
                val rows = minOf(3, 8 - y)
                addRows(pixels.copyOfRange(y * 8, (y + rows) * 8), startY = y, rowCount = rows)
                y += rows
            }
        }.build()

        assertEquals(wholeImage.luma.toList(), banded.luma.toList())
    }

    @Test
    fun otsuThreshold_splitsABimodalImage() {
        val map = lumaMapOf(8, 8) { _, y -> if (y < 4) 40 else 220 }

        val threshold = map.otsuThreshold()

        assertTrue(threshold in 41..220, "threshold $threshold did not fall between the two modes")
    }

    @Test
    fun toGrid_fillsDarkCellsAndInvertFlipsThem() {
        val dark = lumaMapOf(8, 8) { _, _ -> 10 }
        val options = ScanOptions(rows = 5, cols = 5, threshold = 128)

        assertTrue(dark.toGrid(options).all { row -> row.all { it == 1 } })
        assertTrue(dark.toGrid(options.copy(invert = true)).all { row -> row.all { it == 0 } })

        val light = lumaMapOf(8, 8) { _, _ -> 240 }
        assertTrue(light.toGrid(options).all { row -> row.all { it == 0 } })
    }

    @Test
    fun toGrid_reproducesTheSourceShape() {
        // Left half dark, right half light, sampled at exactly the source resolution.
        val map = lumaMapOf(10, 10) { x, _ -> if (x < 5) 0 else 255 }

        val grid = map.toGrid(ScanOptions(rows = 10, cols = 10, threshold = 128))

        assertEquals(10, grid.size)
        grid.forEach { row ->
            assertEquals(listOf(1, 1, 1, 1, 1, 0, 0, 0, 0, 0), row)
        }
    }

    @Test
    fun defaultDimensions_preservesAspectAndClamps() {
        assertEquals(15 to 30, defaultDimensions(width = 100, height = 50))
        // The side is the longest one, so it lands on the rows for a portrait image.
        assertEquals(30 to 15, defaultDimensions(width = 50, height = 100))
        assertEquals(30 to 30, defaultDimensions(width = 10, height = 10))
        // A panorama would round down to nothing, so the short side clamps up instead.
        assertEquals(MIN_NONOGRAM_SIDE to DEFAULT_SCAN_SIDE, defaultDimensions(width = 4000, height = 2))
    }

    @Test
    fun defaultDimensions_scalesToTheRequestedSide() {
        assertEquals(10 to 20, defaultDimensions(width = 100, height = 50, side = 20))
        // The requested side is clamped before the other one is derived from it.
        assertEquals(25 to MAX_NONOGRAM_SIDE, defaultDimensions(width = 100, height = 50, side = 99))
        assertEquals(
            MIN_NONOGRAM_SIDE to MIN_NONOGRAM_SIDE,
            defaultDimensions(width = 100, height = 50, side = 0),
        )
        assertEquals(12 to 12, defaultDimensions(width = 0, height = 0, side = 12))
    }

    @Test
    fun toGrid_alwaysProducesAWellFormedGrid() {
        val map = lumaMapOf(7, 13) { x, y -> (x * 31 + y * 17) % 256 }
        val sizes = listOf(0, 1, MIN_NONOGRAM_SIDE, 17, MAX_NONOGRAM_SIDE, 99, Int.MAX_VALUE)

        for (rows in sizes) {
            for (cols in sizes) {
                val grid = map.toGrid(ScanOptions(rows, cols, threshold = 128))
                assertTrue(
                    grid.isWellFormedGrid(),
                    "rows=$rows cols=$cols produced ${grid.size}x${grid.firstOrNull()?.size}",
                )
            }
        }
    }
}
