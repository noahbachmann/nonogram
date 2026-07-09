package com.trainpaths.nonogram.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.trainpaths.nonogram.navigation.AppBarMode
import com.trainpaths.nonogram.navigation.NonogramAppBar
import com.trainpaths.nonogram.classes.Board
import com.trainpaths.nonogram.dialogs.GenSaveConfirmDialog
import com.trainpaths.nonogram.screens.viewModel.GenViewModel

@Composable
fun GenScreen(
    genViewModel: GenViewModel,
    onConfig: () -> Unit,
    onExitToList: () -> Unit,
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    val attemptLeave = { if (genViewModel.isDirty) showSaveDialog = true else onExitToList() }
    
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = backState) { attemptLeave() }

    Column(modifier = Modifier.fillMaxSize()) {
        NonogramAppBar(
            onBack = onConfig,
            showSettings = true,
            mode = AppBarMode.GENERATOR,
            onSwapMode = { attemptLeave() },
        )

        val nonogram = genViewModel.nonogram
        if (genViewModel.tiles.isNotEmpty()) {
            Board(
                nonogram = nonogram,
                tiles = genViewModel.tiles,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onTileClick = { genViewModel.updateNonogram() },
            )
        } else {
            // Keep the Save button anchored to the bottom while the board is empty.
            Spacer(modifier = Modifier.weight(1f))
        }

        Button(
            onClick = { genViewModel.onSave { onExitToList() } },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("Save", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showSaveDialog) {
        GenSaveConfirmDialog(
            onSave = {
                showSaveDialog = false
                genViewModel.onSave { onExitToList() }
            },
            onDiscard = {
                showSaveDialog = false
                onExitToList()
            },
            onCancel = { showSaveDialog = false },
        )
    }
}
