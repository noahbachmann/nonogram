package com.trainpaths.nonogram.filter

import com.trainpaths.nonogram.classes.Difficulty

object NonogramFilters {

    const val PERSONAL = "Personal"

    val DIFFICULTY = FilterAttribute(
        label = "Difficulty",
        options = Difficulty.entries.map { difficulty ->
            FilterOption(difficulty.label) { it.difficulty == difficulty }
        },
        ascending = compareBy { it.difficulty.ordinal },
    )

    fun forUser(userId: Long?): List<FilterEntry> = listOf(
        DIFFICULTY,
        FilterToggle(PERSONAL) { it.isOwned(userId) },
    )
}
