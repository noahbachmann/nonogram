package com.trainpaths.nonogram.classes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Game(
    nonogram: Nonogram,
    tiles: List<List<Tile>>,
    isLocked: Boolean,
    onWin: () -> Unit,
) {
    fun checkSolved() {
        val values: List<List<Int>> = tiles.map { row ->
            row.map { tile ->
                when (tile.state) {
                    TileState.FILLED -> 1
                    else -> 0
                }
            }
        }

        if (values == nonogram.solution) onWin()
    }

    Board(
        nonogram = nonogram,
        tiles = tiles,
        isLocked = isLocked,
        modifier = Modifier.fillMaxSize(),
        onTilesChanged = ::checkSolved,
    )
}
