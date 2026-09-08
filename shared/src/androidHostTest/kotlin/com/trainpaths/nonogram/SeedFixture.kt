package com.trainpaths.nonogram

import com.trainpaths.nonogram.cache.SeedPuzzle

/**
 * Stand-in for the shipped seeds. `SEED_PUZZLES` is generated from dev Firestore and may be empty between
 * exports, so tests that need built-in puzzles to exist pin their own instead of leaning on the build's.
 */
internal val SEED_FIXTURE: List<SeedPuzzle> = listOf(
    SeedPuzzle(
        id = 4_215_003_001L,
        name = "Plus",
        difficulty = "EASY",
        solution = listOf(
            listOf(0, 0, 1, 0, 0),
            listOf(0, 0, 1, 0, 0),
            listOf(1, 1, 1, 1, 1),
            listOf(0, 0, 1, 0, 0),
            listOf(0, 0, 1, 0, 0),
        ),
    ),
    SeedPuzzle(
        id = 4_215_003_002L,
        name = null,
        difficulty = "MEDIUM",
        solution = listOf(
            listOf(1, 1, 1, 1, 1),
            listOf(1, 0, 0, 0, 0),
            listOf(1, 1, 1, 0, 0),
            listOf(1, 0, 0, 0, 0),
            listOf(1, 1, 1, 1, 1),
        ),
    ),
)
