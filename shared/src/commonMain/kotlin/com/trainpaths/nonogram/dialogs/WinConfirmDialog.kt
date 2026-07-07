package com.trainpaths.nonogram.dialogs

import com.trainpaths.nonogram.icons.home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.trainpaths.nonogram.icons.refresh

@Composable
fun WinConfirmDialog(onConfirm: () -> Unit, onRestart: () -> Unit) {
    AlertDialog(
        onDismissRequest = onRestart,
        title = { Text("You solved it!") },
        text = { Text("Difficulty: idk\nBest Time: 12:12") },

        confirmButton = {
            IconButton(onClick = onConfirm) {
                Icon(
                    imageVector = home,
                    contentDescription = "Home"
                )
            }
        },
        dismissButton = {
            IconButton(onClick = onRestart) {
                Icon(
                    imageVector = refresh,
                    contentDescription = "Restart"
                )
            }
        }
    )
}