package com.trainpaths.nonogram.classes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class NonogramTestData(
    val desc: String,
    val nonogram: Nonogram,
    val expectedRowClues: List<List<Int>>? = null,
    val expectedColClues: List<List<Int>>? = null,
    val verifySolver: Boolean = false,
)

private val nonogramData = arrayOf(
    NonogramTestData(
        desc = "solver sample",
        nonogram = Nonogram(
            id = 1,
            difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0),
                listOf(0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0),
                listOf(0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0),
                listOf(0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0),
                listOf(0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
                listOf(0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 0),
                listOf(0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0),
                listOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0),
                listOf(1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1),
                listOf(1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1),
                listOf(1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1),
                listOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1),
                listOf(0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1),
                listOf(0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1),
                listOf(0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1),
            ),
        ),
        verifySolver = true,
    ),
    NonogramTestData(
        desc = "all-zero row",
        nonogram = Nonogram(
            id = 2,
            difficulty = Difficulty.EASY,
            solution = listOf(listOf(0, 0, 0)),
        ),
        expectedRowClues = listOf(emptyList()),
    ),
    NonogramTestData(
        desc = "all-filled row",
        nonogram = Nonogram(
            id = 3,
            difficulty = Difficulty.EASY,
            solution = listOf(listOf(1, 1, 1)),
        ),
        expectedRowClues = listOf(listOf(3)),
    ),
    NonogramTestData(
        desc = "multiple filled runs",
        nonogram = Nonogram(
            id = 4,
            difficulty = Difficulty.EASY,
            solution = listOf(listOf(1, 1, 0, 1, 1)),
        ),
        expectedRowClues = listOf(listOf(2, 2)),
    ),
    NonogramTestData(
        desc = "single filled cell",
        nonogram = Nonogram(
            id = 5,
            difficulty = Difficulty.EASY,
            solution = listOf(listOf(1)),
        ),
        expectedRowClues = listOf(listOf(1)),
    ),
    NonogramTestData(
        desc = "single empty cell",
        nonogram = Nonogram(
            id = 6,
            difficulty = Difficulty.EASY,
            solution = listOf(listOf(0)),
        ),
        expectedRowClues = listOf(emptyList()),
    ),
    NonogramTestData(
        desc = "filled run with surrounding empty cells",
        nonogram = Nonogram(
            id = 7,
            difficulty = Difficulty.EASY,
            solution = listOf(listOf(0, 1, 1, 0)),
        ),
        expectedRowClues = listOf(listOf(2)),
    ),
    NonogramTestData(
        desc = "multiple rows",
        nonogram = Nonogram(
            id = 8,
            difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(1, 0, 1),
                listOf(0, 0, 0),
                listOf(1, 1, 1),
            ),
        ),
        expectedRowClues = listOf(listOf(1, 1), emptyList(), listOf(3)),
    ),
    NonogramTestData(
        desc = "column runs",
        nonogram = Nonogram(
            id = 9,
            difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(1, 0),
                listOf(1, 0),
                listOf(0, 1),
            ),
        ),
        expectedColClues = listOf(listOf(2), listOf(1)),
    ),
    NonogramTestData(
        desc = "cross pattern",
        nonogram = Nonogram(
            id = 10,
            difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(0, 0, 1, 0, 0),
                listOf(0, 0, 1, 0, 0),
                listOf(1, 1, 1, 1, 1),
                listOf(0, 0, 1, 0, 0),
                listOf(0, 0, 1, 0, 0),
            ),
        ),
        expectedRowClues = listOf(listOf(1), listOf(1), listOf(5), listOf(1), listOf(1)),
        expectedColClues = listOf(listOf(1), listOf(1), listOf(5), listOf(1), listOf(1)),
    ),
    NonogramTestData(
        desc = "alternating cells",
        nonogram = Nonogram(
            id = 11,
            difficulty = Difficulty.EASY,
            solution = listOf(listOf(1, 0, 1, 0, 1)),
        ),
        expectedRowClues = listOf(listOf(1, 1, 1)),
    ),
    NonogramTestData(
        desc = "empty solution",
        nonogram = Nonogram(
            id = 12,
            difficulty = Difficulty.EASY,
            solution = emptyList(),
        ),
        expectedRowClues = emptyList(),
        expectedColClues = emptyList(),
    ),
)

class NonogramTest {

    @Test
    fun solveNonogram_returnsExpectedSolutions() {
        nonogramData.filter { it.verifySolver }.forEach { data ->
            val solver = Solver(data.nonogram)
            val actualSolution = solver.solveNonogram().map { row -> row.toList() }

            assertEquals(
                expected = data.nonogram.solution,
                actual = actualSolution,
                message = "Unexpected solution for ${data.desc}",
            )
        }
    }

    @Test
    fun isValid_isDerivedFromSolverOutput() {
        val valid = nonogramData.first { it.verifySolver }.nonogram
        val invalid = Nonogram(
            id = 13,
            difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(1, 0),
                listOf(0, 1),
            ),
        )

        assertTrue(valid.isValid)
        assertFalse(invalid.isValid)
    }

    @Test
    fun solverAndDerivedValidity_areRepeatable() {
        val nonogram = nonogramData.first { it.verifySolver }.nonogram

        val first = Solver(nonogram).solveNonogram().map { row -> row.toList() }
        val second = Solver(nonogram).solveNonogram().map { row -> row.toList() }

        assertEquals(first, second)
        assertTrue(nonogram.isValid)
        assertTrue(nonogram.isValid)
    }

    @Test
    fun clues_areComputedCorrectly() {
        nonogramData.forEach { data ->
            data.expectedRowClues?.let { expected ->
                assertEquals(
                    expected = expected,
                    actual = data.nonogram.rowClues,
                    message = "Unexpected row clues for ${data.desc}",
                )
            }
            data.expectedColClues?.let { expected ->
                assertEquals(
                    expected = expected,
                    actual = data.nonogram.colClues,
                    message = "Unexpected column clues for ${data.desc}",
                )
            }
        }
    }

    @Test
    fun dimensions_matchSolutions() {
        nonogramData.forEach { data ->
            assertEquals(
                expected = data.nonogram.solution.size,
                actual = data.nonogram.height,
                message = "Unexpected height for ${data.desc}",
            )
            assertEquals(
                expected = data.nonogram.solution.firstOrNull()?.size ?: 0,
                actual = data.nonogram.width,
                message = "Unexpected width for ${data.desc}",
            )
        }
    }

    @Test
    fun blankNameNormalizesToNull() {
        assertNull(normalizeNonogramName("   "))
    }

    @Test
    fun nameIsTrimmedAndLineBreaksBecomeSpaces() {
        assertEquals("blue whale", normalizeNonogramName("  blue\nwhale  "))
    }

    @Test
    fun nameIsLimitedToThirtyCharacters() {
        assertEquals("x".repeat(MAX_NONOGRAM_NAME_LENGTH), normalizeNonogramName("x".repeat(31)))
    }

    @Test
    fun isOwnedMatchesTheAuthor_butNeverTheUnauthoredZero() {
        val mine = Nonogram(id = 1, difficulty = Difficulty.EASY, solution = listOf(listOf(1)), authorId = 7)
        val seeded = Nonogram(id = 2, difficulty = Difficulty.EASY, solution = listOf(listOf(1)))

        assertTrue(mine.isOwned(7))
        assertFalse(mine.isOwned(8))
        assertFalse(mine.isOwned(null))
        assertFalse(seeded.isOwned(0))
        assertFalse(seeded.isOwned(7))
    }
}
