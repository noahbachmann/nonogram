package com.trainpaths.nonogram.filter

import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram

object NonogramFilters {

    val DIFFICULTY = FilterAttribute(
        id = "difficulty",
        label = "Difficulty",
        options = Difficulty.entries.map { difficulty ->
            FilterOption(difficulty.name, difficulty.label) { it.difficulty == difficulty }
        },
        ascending = compareBy<Nonogram> { it.difficulty.ordinal },
    )

    val ALL = listOf(DIFFICULTY)
}
