package com.trainpaths.nonogram.classes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class TileState { NONE, FILLED, CROSSED }

class Tile {
    var state by mutableStateOf(TileState.NONE)

    fun click() {
        state = when (state) {
            TileState.FILLED -> TileState.CROSSED
            TileState.CROSSED -> TileState.NONE
            TileState.NONE -> TileState.FILLED
        }
    }
}