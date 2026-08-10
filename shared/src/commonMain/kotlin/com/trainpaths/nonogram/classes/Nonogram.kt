package com.trainpaths.nonogram.classes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

enum class Difficulty { EASY, MEDIUM, HARD, HARDCORE }

@Serializable
data class Nonogram(
    val id: Long,
    val difficulty: Difficulty,
    val solution: List<List<Int>>,
    val authorId: Long = 0,
    val isValid: Boolean = false,
    val isPublic: Boolean = false,
    val updatedAt: Long = 0
) {
    val height: Int get() = solution.size
    val width: Int get() = solution.firstOrNull()?.size ?: 0

    val rowClues: List<List<Int>>
        get() = solution.map { row -> computeLineClues(row) }

    val solvedRowClues: List<MutableSet<Int>> =
        List(height) { mutableSetOf() }

    val colClues: List<List<Int>>
        get() = (0 until width).map { col ->
            computeLineClues((0 until height).map { row -> solution[row][col] })
        }

    val solvedColClues: List<MutableSet<Int>> =
        List(width) { mutableSetOf() }
}

private fun computeLineClues(line: List<Int>): List<Int> {
    val clues = mutableListOf<Int>()
    var run = 0
    for (cell in line) {
        if (cell == 1) run++
        else if (run > 0) {
            clues.add(run)
            run = 0
        }
    }
    if (run > 0) clues.add(run)

    if (clues.isEmpty())
        return listOf(0)
    return clues
}

@Composable
fun DrawNonogram(progress: List<List<Int>>) {
    Canvas(Modifier.fillMaxSize()) {
        val colWidth = this.size.width / progress.size
        val colHeight = this.size.height / progress[0].size
        var offsetWidth = 0f
        var offsetHeight = 0f

        for (row in progress) {
            for (tile in row) {
                drawRect(
                    color = if (tile == 0) Color.White else Color.Black,
                    topLeft = Offset(offsetWidth, offsetHeight),
                    size = Size(colWidth, colHeight),
                )
                offsetWidth += colWidth
            }
            offsetWidth = 0f
            offsetHeight += colHeight
        }
    }
}
