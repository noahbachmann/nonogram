package com.trainpaths.nonogram.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.icons.expand_content
import com.trainpaths.nonogram.icons.lockClosed
import com.trainpaths.nonogram.icons.lockOpen
import com.trainpaths.nonogram.icons.moreHorizontal
import com.trainpaths.nonogram.icons.save

@Composable
fun BottomToolBar(
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onPlaceholderClick: () -> Unit,
    resetZoom: (() -> Unit)? = null,
    showSave: Boolean = false,
    saveEnabled: Boolean = false,
    onSave: () -> Unit = {},
) {
    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        BottomBarItem(
            label = if (isLocked) "Locked" else "Unlocked",
            imageVector = if (isLocked) lockClosed else lockOpen,
            contentDescription = if (isLocked) "Locked" else "Unlocked",
            onClick = onLockToggle,
        )

        BottomBarItem(
            label = "Color",
            imageVector = moreHorizontal,
            contentDescription = "Color",
            onClick = onPlaceholderClick,
        )

        if (resetZoom != null) {
            BottomBarItem(
                label = "Reset Zoom",
                imageVector = expand_content,
                contentDescription = "Zoom out to fit",
                onClick = resetZoom,
            )
        }

        Spacer(Modifier.weight(1f))

        if (showSave) {
            BottomBarItem(
                label = "Save",
                imageVector = save,
                contentDescription = "Save nonogram",
                onClick = onSave,
                enabled = saveEnabled,
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    }

    Column(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
