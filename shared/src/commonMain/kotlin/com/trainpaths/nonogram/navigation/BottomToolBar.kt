package com.trainpaths.nonogram.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.icons.lockClosed
import com.trainpaths.nonogram.icons.lockOpen
import com.trainpaths.nonogram.icons.moreHorizontal
import com.trainpaths.nonogram.icons.save

@Composable
fun BottomToolBar(
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onPlaceholderClick: () -> Unit,
    showSave: Boolean = false,
    saveEnabled: Boolean = false,
    onSave: () -> Unit = {},
) {
    val iconColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
    )

    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        IconButton(onClick = onLockToggle, colors = iconColors) {
            Icon(
                imageVector = if (isLocked) lockClosed else lockOpen,
                contentDescription = if (isLocked) "Unlock board" else "Lock board",
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(onClick = onPlaceholderClick, colors = iconColors) {
            Icon(
                imageVector = moreHorizontal,
                contentDescription = "Placeholder tool",
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        if (showSave) {
            IconButton(onClick = onSave, enabled = saveEnabled, colors = iconColors) {
                Icon(
                    imageVector = save,
                    contentDescription = "Save nonogram",
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
