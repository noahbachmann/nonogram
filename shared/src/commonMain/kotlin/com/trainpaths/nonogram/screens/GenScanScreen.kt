package com.trainpaths.nonogram.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.BUTTON_SHAPE
import com.trainpaths.nonogram.MAX_CONTENT_WIDTH
import com.trainpaths.nonogram.classes.normalizeNonogramName
import com.trainpaths.nonogram.icons.scan
import com.trainpaths.nonogram.navigation.TopAppBar
import com.trainpaths.nonogram.outlinedFieldColors
import com.trainpaths.nonogram.screens.viewModel.GenViewModel
import com.trainpaths.nonogram.screens.viewModel.ScanViewModel
import com.trainpaths.nonogram.switchColors
import kotlin.math.min

/** How tall the preview is allowed to be, whatever the grid's aspect ratio. */
private val PREVIEW_HEIGHT = 300.dp

/**
 * Turns a picked image into a grid the user can tune, then hands it to the generator board.
 *
 * Goes straight to `GeneratorRoute` rather than through `GenConfScreen`: that screen's "new" branch
 * calls `setNonogram`, which rebuilds the board from scratch and would wipe the scan.
 */
@Composable
fun GenScanScreen(
    genViewModel: GenViewModel,
    scanViewModel: ScanViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val pickImage = rememberImagePicker(
        onPicked = scanViewModel::onImagePicked,
        onError = scanViewModel::onPickFailed,
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            titleIcon = scan,
            onBack = onBack,
            backArrow = true,
            showSettings = true,
        )

        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            scanViewModel.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }

            if (scanViewModel.hasImage) {
                ScanControls(
                    genViewModel = genViewModel,
                    scanViewModel = scanViewModel,
                    onPickAnother = pickImage,
                    onDone = onDone,
                )
            } else {
                ScanIntro(scanViewModel = scanViewModel, onPick = pickImage)
            }
        }
    }
}

@Composable
private fun ScanIntro(scanViewModel: ScanViewModel, onPick: () -> Unit) {
    Text(
        text = "Pick an image and it becomes a nonogram you can edit.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.padding(vertical = 24.dp),
    )
    ScanButton(
        text = if (scanViewModel.isProcessing) "Reading..." else "Choose image",
        onClick = onPick,
        enabled = !scanViewModel.isProcessing,
    )
    if (scanViewModel.isProcessing) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun ScanControls(
    genViewModel: GenViewModel,
    scanViewModel: ScanViewModel,
    onPickAnother: () -> Unit,
    onDone: () -> Unit,
) {
    val textFieldColors = outlinedFieldColors()

    GridPreview(
        grid = scanViewModel.previewGrid,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )

    OutlinedTextField(
        value = scanViewModel.name,
        onValueChange = scanViewModel::setName,
        label = { Text("Name") },
        placeholder = { Text("...") },
        singleLine = true,
        colors = textFieldColors,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SizeField(
            value = scanViewModel.rowsInput,
            onValueChange = scanViewModel::setRows,
            label = "Rows",
            colors = textFieldColors,
            modifier = Modifier.weight(1f),
        )
        SizeField(
            value = scanViewModel.colsInput,
            onValueChange = scanViewModel::setCols,
            label = "Columns",
            colors = textFieldColors,
            modifier = Modifier.weight(1f),
        )
    }

    Text(
        text = "Threshold",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Slider(
        value = scanViewModel.threshold.toFloat(),
        onValueChange = { scanViewModel.setThreshold(it.toInt()) },
        valueRange = 1f..255f,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.onPrimary,
            activeTrackColor = MaterialTheme.colorScheme.onPrimary,
            inactiveTrackColor = MaterialTheme.colorScheme.secondary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Invert",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Switch(
            checked = scanViewModel.invert,
            onCheckedChange = scanViewModel::setInvert,
            colors = switchColors(),
        )
    }

    ScanButton(
        text = "Generate",
        onClick = {
            scanViewModel.normalizeSizeInputs()
            genViewModel.loadScanned(
                grid = scanViewModel.previewGrid,
                name = normalizeNonogramName(scanViewModel.name),
            )
            onDone()
        },
        enabled = scanViewModel.previewGrid.isNotEmpty(),
        modifier = Modifier.padding(top = 20.dp),
    )
    ScanButton(
        text = "Choose another image",
        onClick = onPickAnother,
        enabled = !scanViewModel.isProcessing,
        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
    )
}

/** A static render of the grid as it currently stands. **/
@Composable
private fun GridPreview(grid: List<List<Int>>, modifier: Modifier = Modifier) {
    val rows = grid.size
    val cols = grid.firstOrNull()?.size ?: 0
    if (rows == 0 || cols == 0) return

    Canvas(modifier = modifier.height(PREVIEW_HEIGHT)) {
        val cell = min(size.width / cols, size.height / rows)
        val originX = (size.width - cell * cols) / 2f
        val originY = (size.height - cell * rows) / 2f
        drawRect(
            color = Color.White,
            topLeft = Offset(originX, originY),
            size = Size(cell * cols, cell * rows),
        )
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (grid[row][col] != 1) continue
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(originX + col * cell, originY + row * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

@Composable
private fun ScanButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = BUTTON_SHAPE,
        modifier = modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onPrimary,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
