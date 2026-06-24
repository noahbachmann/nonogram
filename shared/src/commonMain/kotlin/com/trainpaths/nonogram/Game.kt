package com.trainpaths.nonogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.TileButton

@Composable
fun Game(
    nonogram: Nonogram
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (r in 0 until nonogram.height) {
            Row {
                for (c in 0 until nonogram.width) {
                    TileButton(nonogram.tileAt(r, c))
                }
            }
        }
    }
}