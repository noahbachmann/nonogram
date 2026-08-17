package com.trainpaths.nonogram.classes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardHistoryTest {

    @Test
    fun undoRestoresTheBeforeStateAndRedoRestoresAfter() {
        val tiles = grid(1, 1)
        val history = BoardHistory().apply { reset(tiles) }

        tiles[0][0].state = TileState.FILLED
        history.record(listOf(TileEdit(0, 0, before = TileState.NONE, after = TileState.FILLED)))

        assertTrue(history.undo())
        assertEquals(TileState.NONE, tiles[0][0].state)

        assertTrue(history.redo())
        assertEquals(TileState.FILLED, tiles[0][0].state)
    }

    @Test
    fun undoAndRedoAreNoOpsOnEmptyStacks() {
        val tiles = grid(1, 1)
        val history = BoardHistory().apply { reset(tiles) }

        assertFalse(history.undo())
        assertFalse(history.redo())
        assertEquals(TileState.NONE, tiles[0][0].state)
    }

    @Test
    fun recordingAfterUndoClearsRedo() {
        val tiles = grid(1, 1)
        val history = BoardHistory().apply { reset(tiles) }

        history.record(listOf(TileEdit(0, 0, before = TileState.NONE, after = TileState.FILLED)))
        history.undo()
        assertTrue(history.canRedo)

        history.record(listOf(TileEdit(0, 0, before = TileState.NONE, after = TileState.CROSSED)))
        assertFalse(history.canRedo)
        assertFalse(history.redo())
    }

    @Test
    fun historyIsCappedAtMaxSteps() {
        val tiles = grid(1, 1)
        val history = BoardHistory(maxSteps = 10).apply { reset(tiles) }

        // Push 11 moves; the oldest must fall off, leaving exactly 10 available undos.
        repeat(11) { i ->
            val before = tiles[0][0].state
            val after = if (i % 2 == 0) TileState.FILLED else TileState.NONE
            tiles[0][0].state = after
            history.record(listOf(TileEdit(0, 0, before = before, after = after)))
        }

        var undoCount = 0
        while (history.undo()) undoCount++
        assertEquals(10, undoCount)
    }

    @Test
    fun emptyEditListIsIgnored() {
        val history = BoardHistory().apply { reset(grid(1, 1)) }
        history.record(emptyList())
        assertFalse(history.canUndo)
    }

    @Test
    fun multiEditMoveUndoesAndRedoesAsOneStep() {
        val tiles = grid(1, 2)
        val history = BoardHistory().apply { reset(tiles) }

        tiles[0][0].state = TileState.FILLED
        tiles[0][1].state = TileState.FILLED
        history.record(
            listOf(
                TileEdit(0, 0, before = TileState.NONE, after = TileState.FILLED),
                TileEdit(0, 1, before = TileState.NONE, after = TileState.FILLED),
            ),
        )

        assertTrue(history.undo())
        assertEquals(listOf(TileState.NONE, TileState.NONE), tiles[0].map { it.state })
        assertFalse(history.undo())

        assertTrue(history.redo())
        assertEquals(listOf(TileState.FILLED, TileState.FILLED), tiles[0].map { it.state })
    }

    @Test
    fun resetOnANewBoardDropsThePreviousBoardsHistoryAndTarget() {
        val firstBoard = grid(1, 1)
        val history = BoardHistory().apply { reset(firstBoard) }
        history.record(listOf(TileEdit(0, 0, before = TileState.NONE, after = TileState.FILLED)))
        assertTrue(history.canUndo)

        val secondBoard = grid(1, 1)
        history.reset(secondBoard)

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertFalse(history.undo())
        // Undoing must never reach back into the board history was reset away from.
        assertEquals(TileState.NONE, firstBoard[0][0].state)
    }

    private fun grid(rows: Int, cols: Int): List<List<Tile>> =
        List(rows) { List(cols) { Tile() } }
}
