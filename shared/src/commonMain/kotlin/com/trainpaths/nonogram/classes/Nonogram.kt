package com.trainpaths.nonogram.classes

import kotlinx.serialization.Serializable

enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    HARDCORE("Hardcore"),
}

const val MAX_NONOGRAM_NAME_LENGTH = 30
const val UNNAMED_NONOGRAM_TITLE = "???"

fun normalizeNonogramName(value: String): String? =
    value
        .replace('\n', ' ')
        .replace('\r', ' ')
        .trim()
        .take(MAX_NONOGRAM_NAME_LENGTH)
        .takeIf { it.isNotEmpty() }

@Serializable
data class Nonogram(
    val id: Long,
    val difficulty: Difficulty,
    val solution: List<List<Int>>,
    val name: String? = null,
    val authorId: Long = 0,
    var isPublic: Boolean = false,
    val updatedAt: Long = 0
) {
    val height: Int get() = solution.size
    val width: Int get() = solution.firstOrNull()?.size ?: 0

    val rowClues: List<List<Int>> by lazy {
        solution.map { row -> computeLineClues(row) }
    }
    val colClues: List<List<Int>> by lazy {
        (0 until width).map { col ->
            computeLineClues((0 until height).map { row -> solution[row][col] })
        }
    }
    val isValid: Boolean by lazy {
        Solver(this).solveNonogram().map { row -> row.toList() } == solution
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
        return emptyList()
    return clues
}
