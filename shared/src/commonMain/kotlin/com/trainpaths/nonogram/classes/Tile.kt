package com.trainpaths.nonogram.classes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class TileState {
    NONE,
    FILLED,
    CROSSED,
}

enum class DrawMode {
    FILL,
    CROSS,
    ERASE;

    /** The state this mode writes. Constant per mode — an edit never depends on the cell's current state. */
    val target: TileState
        get() = when (this) {
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

    fun click(mode: DrawMode = DrawMode.FILL) {
        state = mode.target
    }
}

fun List<List<Tile>>.toInts(): List<List<Int>> =
    map { row -> row.map { if (it.state == TileState.FILLED) 1 else 0 } }
