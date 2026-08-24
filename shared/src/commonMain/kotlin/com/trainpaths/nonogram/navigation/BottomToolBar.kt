package com.trainpaths.nonogram.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.MAX_CONTENT_WIDTH
import com.trainpaths.nonogram.classes.BoardHistory
import com.trainpaths.nonogram.classes.DrawMode
import com.trainpaths.nonogram.icons.expand_content
import com.trainpaths.nonogram.icons.lockClosed
import com.trainpaths.nonogram.icons.lockOpen
import com.trainpaths.nonogram.icons.redo
import com.trainpaths.nonogram.icons.save
import com.trainpaths.nonogram.icons.stylus
import com.trainpaths.nonogram.icons.tileCross
import com.trainpaths.nonogram.icons.tileErase
import com.trainpaths.nonogram.icons.tileFill
import com.trainpaths.nonogram.icons.undo

@Composable
fun BottomToolBar(
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    drawMode: DrawMode,
    onDrawModeToggle: () -> Unit,
    resetZoom: (() -> Unit)? = null,
    history: BoardHistory? = null,
    showSave: Boolean = false,
    saveEnabled: Boolean = false,
    onSave: () -> Unit = {},
) {
    Box(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondary),
        contentAlignment = Alignment.BottomCenter,
    ) {
        BottomAppBar(
            modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ) {
            BottomBarItem(
                label = if (isLocked) "Locked" else "Unlocked",
                imageVector = if (isLocked) lockClosed else lockOpen,
                contentDescription = if (isLocked) "Locked" else "Unlocked",
                onClick = onLockToggle,
            )

            val drawModeLabel = when (drawMode) {
                DrawMode.TOGGLE -> "Draw"
                DrawMode.FILL -> "Fill"
                DrawMode.CROSS -> "Cross"
                DrawMode.ERASE -> "Erase"
            }
            BottomBarItem(
                label = drawModeLabel,
                imageVector = when (drawMode) {
                    DrawMode.TOGGLE -> stylus
                    DrawMode.FILL -> tileFill
                    DrawMode.CROSS -> tileCross
                    DrawMode.ERASE -> tileErase
                },
                contentDescription = "Draw mode: $drawModeLabel",
                onClick = onDrawModeToggle,
            )

            if (resetZoom != null) {
                BottomBarItem(
                    label = "Zoom out",
                    imageVector = expand_content,
                    contentDescription = "Zoom out to fit",
                    onClick = resetZoom,
                )
            }

            if (history != null) {
                BottomBarItem(
                    label = "Undo",
                    imageVector = undo,
                    contentDescription = "Undo",
                    onClick = { history.undo() },
                    enabled = history.canUndo,
                )
                BottomBarItem(
                    label = "Redo",
                    imageVector = redo,
                    contentDescription = "Redo",
                    onClick = { history.redo() },
                    enabled = history.canRedo,
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
}

@Composable
private fun BottomBarItem(
    label: String,
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.50f)
        else -> MaterialTheme.colorScheme.onSecondary
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
