package com.trainpaths.nonogram.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.navigation.AppBarMode
import com.trainpaths.nonogram.navigation.NonogramAppBar
import com.trainpaths.nonogram.classes.Board
import com.trainpaths.nonogram.screens.viewModel.GenViewModel


@Composable
fun GenScreen(
    genViewModel: GenViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSwapMode: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NonogramAppBar(
            onBack = onBack,
            showSettings = true,
            mode = AppBarMode.GENERATOR,
            onSwapMode = onSwapMode,
        )

        val nonogram = genViewModel.nonogram
        if (nonogram != null && genViewModel.tiles.isNotEmpty()) {
            Board(
                nonogram = nonogram,
                tiles = genViewModel.tiles,
                onTileClick = { genViewModel.updateNonogram() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = { onSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Save", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
