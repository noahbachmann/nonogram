package com.trainpaths.nonogram.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.DrawNonogram
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.navigation.AppBarMode
import com.trainpaths.nonogram.navigation.NonogramAppBar
import com.trainpaths.nonogram.screens.viewModel.GenViewModel

@Composable
fun GenListScreen(
    genViewModel: GenViewModel,
    onSwap: () -> Unit,
    onNewClick: () -> Unit,
    onEditClick: (Nonogram) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NonogramAppBar(
            showSettings = true,
            mode = AppBarMode.GENERATOR,
            onSwapMode = { onSwap() },
        )

        Button(
            onClick = onNewClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("+ New", style = MaterialTheme.typography.titleMedium)
        }

        if (genViewModel.isLoadingMine) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        } else if (genViewModel.myNonograms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "You haven't created any nonograms yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
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
                items(genViewModel.myNonograms) { nonogram ->
                    MyNonogramCard(
                        nonogram = nonogram,
                        onClick = { onEditClick(nonogram) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MyNonogramCard(nonogram: Nonogram, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nonogram #${nonogram.id}",
                    style = MaterialTheme.typography.titleMedium,
                )
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
                if (nonogram.solution.isNotEmpty() && nonogram.solution.first().isNotEmpty()) {
                    DrawNonogram(nonogram.solution)
                }
            }
        }
    }
}
