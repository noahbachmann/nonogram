package com.trainpaths.nonogram.classes

import kotlinx.serialization.Serializable

enum class Difficulty { EASY, MEDIUM, HARD, HARDCORE }

@Serializable
data class Nonogram(
    val id: Long,
    val difficulty: Difficulty,
    val solution: List<List<Int>>
) {
    val height: Int get() = solution.size
    val width: Int get() = solution.firstOrNull()?.size ?: 0
}