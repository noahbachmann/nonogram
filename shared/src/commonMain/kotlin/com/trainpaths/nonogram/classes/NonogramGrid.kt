package com.trainpaths.nonogram.classes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NonogramGrid(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun NonogramCard(
    nonogram: Nonogram,
    progress: List<List<Int>> = emptyList(),
    beatCount: Long = -1,
    onClick: () -> Unit
) {
    val accent = if (beatCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
    val shape = RoundedCornerShape(6.dp)

    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(color = accent, shape = shape)
        )
        Card(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.outline,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.fillMaxHeight().padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nonogram.name ?: UNNAMED_NONOGRAM_TITLE,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (beatCount > 0) {
                            Text(
                                text = "beat: $beatCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp, top = 4.dp)
                            .size(12.dp)
                            .background(
                                color = when (nonogram.difficulty) {
                                    Difficulty.EASY -> MaterialTheme.colorScheme.onTertiary
                                    Difficulty.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                    Difficulty.HARD -> MaterialTheme.colorScheme.tertiaryFixed
                                    Difficulty.HARDCORE -> Color.Black
                                },
                                shape = CircleShape,
                            )
                    )
                }
                Row(Modifier.fillMaxSize()) {
                    DrawNonogram(if (beatCount != 0L && progress.all { row -> row.all { it == 0 } }) nonogram.solution else progress)
                }
            }
        }
    }
}

@Composable
fun DrawNonogram(progress: List<List<Int>>) {
    val rows = progress.size
    val cols = progress.firstOrNull()?.size ?: 0
    if (rows == 0 || cols == 0) return

    Canvas(Modifier.fillMaxSize()) {
        val cell = minOf(size.width / cols, size.height / rows)
        val originX = (size.width - cell * cols) / 2f
        val originY = (size.height - cell * rows) / 2f

        progress.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, tile ->
                drawRect(
                    color = if (tile == 0) Color.White else Color.Black,
                    topLeft = Offset(originX + colIndex * cell, originY + rowIndex * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}
