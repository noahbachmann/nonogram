package com.trainpaths.nonogram.classes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlin.math.min

enum class Difficulty { EASY, MEDIUM, HARD, HARDCORE }

@Serializable
data class Nonogram(
    val id: Long,
    val difficulty: Difficulty,
    val solution: List<List<Int>>,
    val authorId: Long = 0,
    val valid: Long = 0,
    val status: Long = 0,
    val updatedAt: Long = 0
) {
    val height: Int get() = solution.size
    val width: Int get() = solution.firstOrNull()?.size ?: 0

    val rowClues: List<List<Int>>
        get() = solution.map { row -> computeLineClues(row) }

    val colClues: List<List<Int>>
        get() = (0 until width).map { col ->
            computeLineClues((0 until height).map { row -> solution[row][col] })
        }
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

fun solveNonogram(ng: Nonogram): Array<Array<Int>> {
    val solution: Array<Array<Int>> = Array(ng.height) { Array(ng.width) { 0 } }
    for (row in 0 until ng.height) {
        val clues: List<Int> = ng.rowClues[row]
        val max = clues.sum() + clues.size - 1

        if (max == ng.width) {
            var l = 0
            for (clue in clues) {
                val r = l + clue
                solution[row].fill(1, l, r)

                if (r >= ng.width) continue
                solution[row][r] = 2
                l = r + 1
            }
            continue
        } else if (max > ng.width - max) {
            var l = 0
            var r = ng.width - max
            for (clue in clues) {
                l += clue
                if (l > r && l - r < clue) {
                    solution[row].fill(1, r, l)
                }
                r = ++l
            }
        }
    }

    for (col in 0 until ng.width) {
        val clues: List<Int> = ng.colClues[col]
        val max = clues.sum() + clues.size - 1

        if (max == ng.height) {
            var l = 0
            for (clue in clues) {
                val r = l + clue
                for (row in l until r) {
                    solution[row][col] = 1
                }

                if (r >= ng.height) continue
                solution[r][col] = 2
                l = r + 1
            }
            continue
        } else if (max > ng.height - max) {
            var l = 0
            var r = ng.height - max
            for (clue in clues) {
                l += clue
                if (l > r && l - r < clue) {
                    for (row in r until l) {
                        solution[row][col] = 1
                    }
                }
                r += ++l
            }
        }

        val top = clues[0] + 1
        for (row in 0 until top) {
            if (solution[row][col] == 1) {
                for (r in row until top) {
                    solution[r][col] = 1
                }
                break
            }
        }

        val bottom = ng.height - clues.last()
        if (solution[bottom][col] == 1) {
            solution[bottom - 1][col] = 2
        }
        for (row in bottom until ng.height) {
            if (solution[row][col] == 1) {
                for (r in bottom until row) {
                    solution[r][col] = 1
                }
                break
            }
        }
    }
    return solution
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