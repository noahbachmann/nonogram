package com.trainpaths.nonogram.classes

import kotlin.test.Test
import kotlin.test.assertEquals

/** `#` filled, `X` crossed, `.` untouched. */
private fun String.toCells(): List<TileState> = map { char ->
    when (char) {
        '#' -> TileState.FILLED
        'X' -> TileState.CROSSED
        else -> TileState.NONE
    }
}

private data class ClueProgressTestData(
    val desc: String,
    val clues: List<Int>,
    val line: String,
    /** Indices of the clues expected to be struck out. */
    val struck: List<Int>,
)

private val clueProgressData = arrayOf(
    ClueProgressTestData(
        desc = "a full line is sealed by both edges",
        clues = listOf(5),
        line = "#####",
        struck = listOf(0),
    ),
    ClueProgressTestData(
        desc = "one cell short leaves the run open",
        clues = listOf(5),
        line = "####.",
        struck = emptyList(),
    ),
    ClueProgressTestData(
        desc = "a run with blanks around it is not sealed",
        clues = listOf(3),
        line = ".###.",
        struck = emptyList(),
    ),
    ClueProgressTestData(
        desc = "the same run, sealed by crosses",
        clues = listOf(3),
        line = "X###X",
        struck = listOf(0),
    ),
    ClueProgressTestData(
        desc = "a sealed 1 takes the 1, not a 3",
        clues = listOf(3, 1, 3, 2),
        line = "....X#X........",
        struck = listOf(1),
    ),
    ClueProgressTestData(
        desc = "a sealed 1 then a sealed 3 takes the 1 and the second 3",
        clues = listOf(3, 1, 3, 2),
        line = "....X#X###X....",
        struck = listOf(1, 2),
    ),
    ClueProgressTestData(
        desc = "a sealed 3 at the start takes the first 3",
        clues = listOf(3, 1, 3, 2),
        line = "X###X..........",
        struck = listOf(0),
    ),
    ClueProgressTestData(
        // The first 3 cannot be here — 1, 3, 2 no longer fit after it — but the second 3 can:
        // 3 and 1 fit exactly in the five cells to its left.
        desc = "a sealed 3 too far right for the first 3 takes the second",
        clues = listOf(3, 1, 3, 2),
        line = ".....X###X.....",
        struck = listOf(2),
    ),
    ClueProgressTestData(
        desc = "a sealed 3 no clue can reach strikes nothing",
        clues = listOf(3, 1, 3, 2),
        line = "..........X###X",
        struck = emptyList(),
    ),
    ClueProgressTestData(
        desc = "an open run consumes its clue without striking it",
        clues = listOf(3, 1),
        line = "##...X#X",
        struck = listOf(1),
    ),
    ClueProgressTestData(
        desc = "a sealed run matching nothing stops the line",
        clues = listOf(3, 1),
        line = "X##X....",
        struck = emptyList(),
    ),
    ClueProgressTestData(
        desc = "a completed line with untouched gaps strikes nothing",
        clues = listOf(2, 2),
        line = "##.##",
        struck = emptyList(),
    ),
    ClueProgressTestData(
        desc = "the same line with the gap crossed strikes everything",
        clues = listOf(2, 2),
        line = "##X##",
        struck = listOf(0, 1),
    ),
    ClueProgressTestData(
        desc = "an empty line",
        clues = listOf(2, 2),
        line = ".....",
        struck = emptyList(),
    ),
    ClueProgressTestData(
        desc = "a line with no clues",
        clues = emptyList(),
        line = "XXXXX",
        struck = emptyList(),
    ),
)

class ClueProgressTest {
    @Test
    fun solvedClueMask_strikesOnlyCertainlyDrawnClues() {
        clueProgressData.forEach { data ->
            val expected = data.struck.fold(0L) { mask, index -> mask or (1L shl index) }

            assertEquals(
                expected = expected,
                actual = solvedClueMask(data.clues, data.line.toCells()),
                message = "Unexpected mask for ${data.desc}",
            )
        }
    }

    @Test
    fun solvedClueMask_strikesEveryClueOfASolvedLine() {
        val puzzle = Nonogram(
            id = 1,
            difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(1, 1, 0, 1, 0),
                listOf(0, 0, 0, 0, 0),
                listOf(1, 0, 1, 0, 1),
                listOf(0, 1, 1, 1, 0),
                listOf(1, 1, 1, 1, 1),
            ),
        )

        // A player who crosses every blank has sealed every run in the line.
        puzzle.solution.forEachIndexed { row, cells ->
            val clues = puzzle.rowClues[row]
            val marks = cells.map { if (it == 1) TileState.FILLED else TileState.CROSSED }
            val all = (0 until clues.size).fold(0L) { mask, index -> mask or (1L shl index) }

            assertEquals(
                expected = all,
                actual = solvedClueMask(clues, marks),
                message = "Unexpected mask for solved row $row",
            )
        }
    }
}
