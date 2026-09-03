package com.trainpaths.nonogram.classes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolutionCodecTest {

    private val grid = List(5) { row -> List(5) { col -> if (row == col) 1 else 0 } }

    @Test
    fun encodeThenDecode_roundTrips() {
        assertEquals(grid, grid.toSolutionJson().toSolutionOrNull())
    }

    @Test
    fun encode_isStableForTheSameGrid() {
        // hasPublishConflict compares the encoded strings, so equal grids must encode identically.
        assertEquals(grid.toSolutionJson(), grid.map { it.toList() }.toSolutionJson())
    }

    @Test
    fun decode_returnsNullAndReportsWhyOnGarbage() {
        var reason: String? = null
        assertNull("not json at all".toSolutionOrNull { reason = it })
        assertTrue(reason!!.isNotEmpty())
    }

    @Test
    fun decode_returnsNullOnAWellFormedButWrongShape() {
        assertNull("[1,0,1]".toSolutionOrNull())
        assertNull("""{"rows":[]}""".toSolutionOrNull())
    }

    @Test
    fun decode_acceptsEmptyAndRaggedGrids_shapeIsTheCallersProblem() {
        assertEquals(emptyList<List<Int>>(), "[]".toSolutionOrNull())
        assertEquals(listOf(listOf(1, 0), listOf(1)), "[[1,0],[1]]".toSolutionOrNull())
    }
}
