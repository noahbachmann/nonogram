package com.trainpaths.nonogram.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.imageResource

@Composable
fun PlayConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Play?") },
        text = { Text("Difficulty: idk\nBest Time: 12:12") },
        
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Yes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No") }
        }
    )
}
