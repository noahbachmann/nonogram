package com.trainpaths.nonogram.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.trainpaths.nonogram.navigation.AppBarMode
import com.trainpaths.nonogram.navigation.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import com.trainpaths.nonogram.classes.NonogramCard
import com.trainpaths.nonogram.classes.NonogramGrid

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    onNonogramClick: (Long) -> Unit,
    onGenClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            showSettings = true,
            mode = AppBarMode.PUZZLE,
            onSwapMode = { onGenClick() },
        )

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            NonogramGrid {
                items(viewModel.nonograms) { nonogram ->
                    NonogramCard(
                        nonogram = nonogram,
                        progress = viewModel.getProgress(nonogram.id, nonogram.height, nonogram.width),
                        beatCount = viewModel.getBeatCount(nonogram.id),
                        onClick = { onNonogramClick(nonogram.id) })
                }
            }
        }
    }
}
