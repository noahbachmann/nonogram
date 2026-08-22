package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.classes.Nonogram

/** Consecutive denials that cost a user the right to request publication. */
const val DENIAL_BAN_THRESHOLD = 5

/** A user's standing in the publish-review flow, mirrored from `users/{uid}` in Firestore. */
data class ModerationGate(
    val denialStreak: Int = 0,
    val banned: Boolean = false,
)

/** One pending request as the admin queue sees it: the puzzle plus who authored it. */
data class PendingReview(
    val nonogram: Nonogram,
    val authorUid: String,
)

fun nextDenialStreak(current: Int, approved: Boolean): Int =
    if (approved) 0 else current + 1

fun isPublishBanned(streak: Int): Boolean = streak >= DENIAL_BAN_THRESHOLD
