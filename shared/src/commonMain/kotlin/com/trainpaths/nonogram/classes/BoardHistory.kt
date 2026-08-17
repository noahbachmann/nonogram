package com.trainpaths.nonogram.classes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One tile's state change within a move, so it can be replayed forwards or backwards. */
data class TileEdit(val row: Int, val col: Int, val before: TileState, val after: TileState)

/**
 * Undo/redo journal of moves on a board, capped at [maxSteps]. A move is a list of [TileEdit]s — one
 * drag stroke or one tap — recorded as a single step so one undo reverses the whole gesture.
 *
 * Writes states back into the caller's own [Tile] objects rather than replacing them, so [Board]'s
 * single-Canvas draw invalidation still applies (see `docs/board-rendering.md`).
 */
class BoardHistory(private val maxSteps: Int = 10) {
    private val undoStack = ArrayDeque<List<TileEdit>>()
    private val redoStack = ArrayDeque<List<TileEdit>>()
    private var tiles: List<List<Tile>> = emptyList()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    /** Points the journal at a freshly loaded board, dropping whatever history the previous board had. */
    fun reset(tiles: List<List<Tile>>) {
        this.tiles = tiles
        undoStack.clear()
        redoStack.clear()
        syncFlags()
    }

    /** Records a completed move. Ignored if empty. Clears the redo stack — a fresh move invalidates it. */
    fun record(edits: List<TileEdit>) {
        if (edits.isEmpty()) return
        undoStack.addLast(edits)
        while (undoStack.size > maxSteps) undoStack.removeFirst()
        redoStack.clear()
        syncFlags()
    }

    /** Reverts the most recent move by writing each edit's [TileEdit.before] back into the board. */
    fun undo(): Boolean {
        val edits = undoStack.removeLastOrNull() ?: return false
        for ((row, col, before) in edits) tiles.getOrNull(row)?.getOrNull(col)?.state = before
        redoStack.addLast(edits)
        syncFlags()
        return true
    }

    /** Reapplies the most recently undone move by writing each edit's [TileEdit.after] into the board. */
    fun redo(): Boolean {
        val edits = redoStack.removeLastOrNull() ?: return false
        for ((row, col, _, after) in edits) tiles.getOrNull(row)?.getOrNull(col)?.state = after
        undoStack.addLast(edits)
        syncFlags()
        return true
    }

    private fun syncFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }
}
