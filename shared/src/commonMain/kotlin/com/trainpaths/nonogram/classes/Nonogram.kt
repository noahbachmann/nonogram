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

data class Cell(
    var state: Int = 0,
    val posRowClues: MutableList<Int> = mutableListOf(),
    val posColClues: MutableList<Int> = mutableListOf(),
)

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
    // first time ROWS
    for (row in 0 until ng.height) {
        val clues: List<Int> = ng.rowClues[row]
        val max = clues.sum() + clues.size - 1
        val diff = ng.width - max

        if (diff == 0) {
            var l = 0
            for ((index, clue) in clues.withIndex()) {
                val r = l + clue

                drawRowTiles(l, r, row, index)

                if (r >= ng.width) continue
                solution[row][r] = 2
                l = r + 1
            }
            continue
        } else if (max > diff && clues.max() > diff) {
            var l = 0
            var r = diff
            for ((index, clue) in clues.withIndex()) {
                l += clue
                if (l > r && l - r < clue) {
                    drawRowTiles(r, l, row, index)
                }
                r += clue + 1
                l++
            }
        }
    }

    // first time COLS
    for (col in 0 until ng.width) {
        val clues: List<Int> = ng.colClues[col]
        val max = clues.sum() + clues.size - 1
        val diff = ng.height - max

        if (diff == 0) {
            var l = 0
            for ((index, clue) in clues.withIndex()) {
                val r = l + clue

                drawColTiles(l, r, col, index)

                if (r >= ng.height) continue
                solution[r][col] = 2
                l = r + 1
            }
            continue
        } else if (max > diff && clues.max() > diff) {
            var l = 0
            var r = diff
            for ((index, clue) in clues.withIndex()) {
                l += clue
                if (l > r && l - r < clue) {
                    drawColTiles(r, l, col, index)
                }
                r += clue + 1
                l++
            }
        }
    }

    for (col in trackingCols) {
        val clues: List<Int> = ng.colClues[col]

        //top check for constraints
        var top = clues[0]
        var topCount = 0
        while (solution[top + topCount][col] == 1) {
            trackingRows.add(topCount)
            solution[topCount++][col] = 2
        }
        top += topCount
        for (row in topCount until top) {
            if (solution[row][col] == 1) {
                var len = 0
                for (r in row until top) {
                    solution[r][col] = 1
                    len++
                }
                if (len == clues[0]) {
                    solution[top][col] = 2
                    trackingRows.addAll(row until top + 1)
                } else {
                    trackingRows.addAll(row until top)
                }
                break
            }
        }

        //bot check for constraints
        var bot = ng.height - clues.last()
        var botCount = 1

        while (solution[bot - botCount][col] == 1) {
            val newBot = ng.height - botCount

            trackingRows.add(newBot)
            solution[newBot][col] = 2

            botCount++
        }
        bot -= --botCount

        val botEnd = ng.height - botCount
        for (row in bot until botEnd) {
            if (solution[row][col] == 1) {
                var len = 1
                for (r in bot until row) {
                    solution[r][col] = 1
                    len++
                }
                if (len == clues.last()) {
                    solution[bot--][col] = 2
                    trackingRows.addAll(bot until ng.height)
                } else {
                    trackingRows.addAll(bot until row)
                }
                break
            }
        }
    }
    trackingCols.clear()

    return solution
}

private fun drawRowTiles(l: Int, r: Int, row: Int, index: Int) {
    for (col in l until r) {
        val cell = solving[row][col]
        cell.state = 1
        cell.posRowClues.add(index)

        solution[row][col] = 1
        trackingCols.add(col)
    }
}

private fun drawColTiles(l: Int, r: Int, col: Int, index: Int) {
    for (row in l until r) {
        val cell = solving[row][col]
        cell.state = 1
        cell.posColClues.add(index)

        solution[row][col] = 1
        trackingRows.add(row)
    }
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
