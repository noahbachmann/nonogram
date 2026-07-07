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
    fun click_cyclesNoneToFilled() {
        val tile = Tile()
        tile.click()
        assertEquals(TileState.FILLED, tile.state)
    }

    @Test
    fun click_cyclesFilledToCrossed() {
        val tile = Tile()
        tile.click() // NONE -> FILLED
        tile.click() // FILLED -> CROSSED
        assertEquals(TileState.CROSSED, tile.state)
    }

    @Test
    fun click_cyclesCrossedToNone() {
        val tile = Tile()
        tile.click() // NONE -> FILLED
        tile.click() // FILLED -> CROSSED
        tile.click() // CROSSED -> NONE
        assertEquals(TileState.NONE, tile.state)
    }

    @Test
    fun click_fullCycleTwice() {
        val tile = Tile()
        repeat(3) { tile.click() }
        assertEquals(TileState.NONE, tile.state)
        tile.click()
        assertEquals(TileState.FILLED, tile.state)
        tile.click()
        assertEquals(TileState.CROSSED, tile.state)
        tile.click()
        assertEquals(TileState.NONE, tile.state)
    }
}
