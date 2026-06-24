package com.trainpaths.nonogram

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.Tile
import com.trainpaths.nonogram.classes.TileState

@Composable
fun Game(
    nonogram: Nonogram) {
    Column {
        for (r in 0 until nonogram.height) {
            Row {
                for (c in 0 until nonogram.width) {
                    TileButton(nonogram.tileAt(r, c)) { nonogram.onTileClick(r, c) }
                }
            }
        }
    }
}

@Composable
fun TileButton(tile: Tile, onClick: () -> Unit) {
    val color = when (tile.state) {          // reading tile.state subscribes this composable
        TileState.NONE  -> MaterialTheme.colorScheme.surfaceVariant
        TileState.FILLED -> Color.Black
        TileState.CROSSED -> MaterialTheme.colorScheme.errorContainer
    }
    Button(
        onClick = onClick,
        modifier = Modifier.size(48.dp).padding(1.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        if (tile.state == TileState.CROSSED) Text("✕")
    }
}