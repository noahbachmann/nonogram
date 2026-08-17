package com.trainpaths.nonogram.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trainpaths.nonogram.navigation.AppBarMode
import com.trainpaths.nonogram.navigation.BottomToolBar
import com.trainpaths.nonogram.navigation.TopAppBar
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.classes.BoardTransformState
import com.trainpaths.nonogram.classes.Game

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onWin: () -> Unit,
    onSwapMode: () -> Unit,
) {
    var isLocked by remember { mutableStateOf(true) }

    val nonogram = viewModel.nonogram
    val boardState = remember(nonogram?.width, nonogram?.height) { BoardTransformState() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            onBack = onBack,
            showSettings = true,
            mode = AppBarMode.PUZZLE,
            onSwapMode = onSwapMode,
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (nonogram == null) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Game(
                    nonogram = nonogram,
                    tiles = viewModel.tiles,
                    isLocked = isLocked,
                    state = boardState,
                    onWin = onWin,
                )
            }
        }

        BottomToolBar(
            isLocked = isLocked,
            onLockToggle = { isLocked = !isLocked },
            onPlaceholderClick = {},
            resetZoom = { boardState.reset() },
        )
    }
}
