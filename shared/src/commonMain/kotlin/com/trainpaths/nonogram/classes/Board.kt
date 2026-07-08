package com.trainpaths.nonogram.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable


@Composable
fun Board(
    nonogram: Nonogram,
    tiles: List<List<Tile>>,
    onTileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .safeContentPadding().fillMaxWidth().fillMaxHeight(0.7f),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.Bottom
        ) {
            for (row in 0 until nonogram.height) {
                Row(
                    modifier = Modifier.height(48.dp).fillMaxWidth().padding(1.dp).background(Color.White),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    for (hint in nonogram.rowClues[row].indices) {
                        Text(
                            modifier = Modifier.padding(horizontal = 2.dp),
                            text = nonogram.rowClues[row][hint].toString(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Column(
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Max)) {
                for (column in 0 until nonogram.width) {
                    Column(
                        modifier = Modifier.width(48.dp).fillMaxHeight().padding(1.dp).background(Color.White),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (hint in nonogram.colClues[column].indices) {
                            Text(
                                modifier = Modifier.padding(start = 2.dp, end = 2.dp),
                                text = nonogram.colClues[column][hint].toString(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            for (r in 0 until nonogram.height) {
                Row {
                    for (c in 0 until nonogram.width) {
                        TileButton(tiles[r][c], onTileClick)
                    }
                }
            }
        }
    }
}