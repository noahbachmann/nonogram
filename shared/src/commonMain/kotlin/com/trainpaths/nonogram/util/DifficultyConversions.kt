package com.trainpaths.nonogram.util

import com.trainpaths.nonogram.classes.Difficulty

/**
 * Unknown or absent names read as [Difficulty.EASY], so an older writer can never crash a newer
 * reader — the same guarantee `String?.toPublishStatus` already makes.
 */
internal fun String?.toDifficulty(): Difficulty =
    Difficulty.entries.firstOrNull { it.name == this } ?: Difficulty.EASY
