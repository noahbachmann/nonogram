package com.trainpaths.nonogram.classes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class TileState {
    NONE,
    FILLED,
    CROSSED;

    fun next(): TileState = when (this) {
        NONE -> FILLED
        FILLED -> CROSSED
        CROSSED -> NONE
    }
}

enum class DrawMode {
    TOGGLE,
    FILL,
    CROSS,
    ERASE;

    fun next(): DrawMode = when (this) {
        TOGGLE -> FILL
        FILL -> CROSS
        CROSS -> ERASE
        ERASE -> TOGGLE
    }

    fun apply(current: TileState): TileState = when (this) {
        TOGGLE -> current.next()
        FILL -> TileState.FILLED
        CROSS -> TileState.CROSSED
        ERASE -> TileState.NONE
    }
}

class Tile {
    private var _state by mutableStateOf(TileState.NONE)

    var wrong by mutableStateOf(false)

    var state: TileState
        get() = _state
        set(value) {
            if (value == _state) return
            _state = value
            wrong = false
        }

    fun click(mode: DrawMode = DrawMode.TOGGLE) {
        state = mode.apply(state)
    }
}

fun List<List<Tile>>.toInts(): List<List<Int>> =
    map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }
