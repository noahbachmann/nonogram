package com.trainpaths.nonogram.classes

import kotlin.test.Test
import kotlin.test.assertEquals

class TileTest {

    @Test
    fun initialState_isNone() {
        val tile = Tile()
        assertEquals(TileState.NONE, tile.state)
    }

    @Test
    fun click_defaultsToFillAndIsIdempotent() {
        val tile = Tile()
        tile.click()
        assertEquals(TileState.FILLED, tile.state)
        repeat(3) { tile.click() }
        assertEquals(TileState.FILLED, tile.state)
    }

    @Test
    fun click_withExplicitMode_writesThatStateFromAnyStartingState() {
        val targets = mapOf(
            DrawMode.FILL to TileState.FILLED,
            DrawMode.CROSS to TileState.CROSSED,
            DrawMode.ERASE to TileState.NONE,
        )

        for ((mode, target) in targets) {
            for (start in TileState.entries) {
                val tile = Tile().apply { state = start }
                tile.click(mode)
                assertEquals(target, tile.state, "$mode from $start")
            }
        }
    }

    @Test
    fun click_repeatedInSameMode_neverLeavesThatState() {
        for (mode in DrawMode.entries) {
            val tile = Tile()
            repeat(3) { tile.click(mode) }
            assertEquals(mode.target, tile.state, "$mode")
        }
    }

    @Test
    fun toInts_countsOnlyFilled() {
        val tiles = listOf(
            listOf(TileState.FILLED, TileState.CROSSED, TileState.NONE),
            listOf(TileState.NONE, TileState.FILLED, TileState.CROSSED),
        ).map { row -> row.map { Tile().apply { state = it } } }

        assertEquals(
            listOf(listOf(1, 0, 0), listOf(0, 1, 0)),
            tiles.toInts(),
        )
    }

    @Test
    fun toInts_preservesShapeOfAnEmptyOrRaggedBoard() {
        assertEquals(emptyList<List<Int>>(), emptyList<List<Tile>>().toInts())
        assertEquals(
            listOf(listOf(0, 0), listOf(0)),
            listOf(List(2) { Tile() }, List(1) { Tile() }).toInts(),
        )
    }
}
