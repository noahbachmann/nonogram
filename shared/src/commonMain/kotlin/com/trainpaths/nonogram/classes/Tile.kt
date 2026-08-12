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

class Tile {
    var state by mutableStateOf(TileState.NONE)

    fun click() {
        state = state.next()
    }
}
