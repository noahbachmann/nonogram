package com.trainpaths.nonogram.sync

/** Consecutive denials that cost a user the right to request publication. */
const val DENIAL_BAN_THRESHOLD = 5

/** A user's standing in the publish-review flow, mirrored from `users/{uid}` in Firestore. */
data class ModerationGate(
    val denialStreak: Int = 0,
    val banned: Boolean = false,
)

fun nextDenialStreak(current: Int, approved: Boolean): Int =
    if (approved) 0 else current + 1

fun isPublishBanned(streak: Int): Boolean = streak >= DENIAL_BAN_THRESHOLD
