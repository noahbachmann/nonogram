package com.trainpaths.nonogram.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.trainpaths.nonogram.navigation.AppBarMode
import com.trainpaths.nonogram.navigation.NonogramAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.DrawNonogram

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    onNonogramClick: (Long) -> Unit,
    onGenClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NonogramAppBar(
            showSettings = true,
            mode = AppBarMode.PUZZLE,
            onSwapMode = { onGenClick() },
        )

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),

                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

@Composable
private fun NonogramCard(nonogram: Nonogram, progress: List<List<Int>>, beatCount: Long, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (beatCount > 0) BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary) else null
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Nonogram #${nonogram.id}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (beatCount > 0) {
                        Text(
                            text = "beat: $beatCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Box(
                    modifier = Modifier.padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (nonogram.difficulty) {
                                Difficulty.EASY -> Color.Green
                                Difficulty.MEDIUM -> Color.Yellow
                                Difficulty.HARD -> Color.Red
                                else -> Color.Black
                            },
                        )
                    ) {
                        Text(
                            text = nonogram.difficulty.name.first().toString(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxSize()) {
                DrawNonogram(progress)
            }
        }
    }
}
