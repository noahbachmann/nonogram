package com.trainpaths.nonogram.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trainpaths.nonogram.NonogramAppBar
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.classes.Game

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onWin: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NonogramAppBar(
            onBack = onBack,
            showSettings = true,
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val nonogram = viewModel.nonogram
            if (nonogram == null) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Game(nonogram = nonogram, tiles = viewModel.tiles, onWin = onWin)
            }
        }
    }
}
