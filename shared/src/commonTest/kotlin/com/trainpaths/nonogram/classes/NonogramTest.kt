package com.trainpaths.nonogram.classes

import kotlin.test.Test
import kotlin.test.assertEquals

class NonogramTest {

    @Test
    fun rowClues_allZeros_returnsListOfZero() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(listOf(0, 0, 0))
        )
        assertEquals(listOf(listOf(0)), nonogram.rowClues)
    }

    @Test
    fun rowClues_allOnes_returnsSingleCount() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(listOf(1, 1, 1))
        )
        assertEquals(listOf(listOf(3)), nonogram.rowClues)
    }

    @Test
    fun rowClues_mixedRuns() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(listOf(1, 1, 0, 1, 1))
        )
        assertEquals(listOf(listOf(2, 2)), nonogram.rowClues)
    }

    @Test
    fun rowClues_singleFilledCell() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(listOf(1))
        )
        assertEquals(listOf(listOf(1)), nonogram.rowClues)
    }

    @Test
    fun rowClues_singleEmptyCell() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(listOf(0))
        )
        assertEquals(listOf(listOf(0)), nonogram.rowClues)
    }

    @Test
    fun rowClues_leadingAndTrailingZeros() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(listOf(0, 1, 1, 0))
        )
        assertEquals(listOf(listOf(2)), nonogram.rowClues)
    }

    @Test
    fun rowClues_multipleRows() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(1, 0, 1),
                listOf(0, 0, 0),
                listOf(1, 1, 1)
            )
        )
        assertEquals(
            listOf(listOf(1, 1), listOf(0), listOf(3)),
            nonogram.rowClues
        )
    }

    @Test
    fun colClues_computedCorrectly() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(1, 0),
                listOf(1, 0),
                listOf(0, 1)
            )
        )
        assertEquals(listOf(listOf(2), listOf(1)), nonogram.colClues)
    }

    @Test
    fun colClues_crossPattern() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(0, 0, 1, 0, 0),
                listOf(0, 0, 1, 0, 0),
                listOf(1, 1, 1, 1, 1),
                listOf(0, 0, 1, 0, 0),
                listOf(0, 0, 1, 0, 0)
            )
        )
        assertEquals(listOf(listOf(1), listOf(1), listOf(5), listOf(1), listOf(1)), nonogram.colClues)
        assertEquals(listOf(listOf(1), listOf(1), listOf(5), listOf(1), listOf(1)), nonogram.rowClues)
    }

    @Test
    fun height_matchesRowCount() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(
                listOf(1, 0),
                listOf(0, 1),
                listOf(1, 1)
            )
        )
        assertEquals(3, nonogram.height)
    }

    @Test
    fun width_matchesColumnCount() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(listOf(1, 0, 1, 0))
        )
        assertEquals(4, nonogram.width)
    }

    @Test
    fun width_emptySolution_returnsZero() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = emptyList()
        )
        assertEquals(0, nonogram.height)
        assertEquals(0, nonogram.width)
    }

    @Test
    fun alternatingCells_eachCountsAsOne() {
        val nonogram = Nonogram(
            id = 1, difficulty = Difficulty.EASY,
            solution = listOf(listOf(1, 0, 1, 0, 1))
        )
        assertEquals(listOf(listOf(1, 1, 1)), nonogram.rowClues)
    }
}
