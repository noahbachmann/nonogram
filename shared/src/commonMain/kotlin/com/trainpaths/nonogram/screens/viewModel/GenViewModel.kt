package com.trainpaths.nonogram.screens.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileState

class GenViewModel() : ViewModel() {

    var height: Int = 0
        private set
    var width: Int = 0
        private set

    var tiles by mutableStateOf<List<List<Tile>>>(emptyList())
        private set

    var nonogram by mutableStateOf<Nonogram?>(null)
        private set

    fun setNonogram(h: Int, w: Int) {
        height = h
        width = w
        tiles = List(h) { List(w) { Tile() } }
        nonogram = buildNonogram()
    }

    fun updateNonogram() {
        nonogram = buildNonogram()
    }

    private fun buildNonogram(): Nonogram {
        val solution = tiles.map { row ->
            row.map { tile ->
                when (tile.state) {
                    TileState.FILLED -> 1
                    else -> 0
                }
            }
        }
        return Nonogram(
            id = 0,
            difficulty = Difficulty.EASY,
            solution = solution
        )
    }
}
