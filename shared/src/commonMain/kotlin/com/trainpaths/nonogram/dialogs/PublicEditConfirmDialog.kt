package com.trainpaths.nonogram.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PublicEditConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Change a public nonogram?") },
        text = {
            Text(
                "You are about to change a public nonogram. This will make it private " +
                        "and it has to be reviewed again. Do you want to proceed?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Proceed") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
