package com.trainpaths.nonogram.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.icons.close

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

@Composable
fun TileButton(tile: Tile) {
    val color = when (tile.state) {
        TileState.FILLED -> Color.Black
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(1.dp)
            .background(color)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null   // no ripple at all
            ) { tile.click() },
        contentAlignment = Alignment.Center
    ) {
        if (tile.state == TileState.CROSSED) Icon(
            imageVector = close,
            tint = Color.Black,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}