package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import kotlin.time.Clock

interface SyncService {
    suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long)
    suspend fun hasRemoteProgress(firebaseUid: String): Boolean
    suspend fun uploadAllLocalProgress(firebaseUid: String)
    suspend fun pullAllProgress(firebaseUid: String)
    suspend fun pullAndMergeAllProgress(firebaseUid: String)

    /**
     * Writes the puzzle to the shared `nonograms` collection, authored by [firebaseUid].
     */
    suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram, writePublishStatus: Boolean = false)

    /** Pushes every locally authored puzzle; used once when an account first signs in. */
    suspend fun uploadAllLocalNonograms(firebaseUid: String)

    /** Runs for guests too ([firebaseUid] null): approved puzzles are readable unauthenticated. */
    suspend fun pullPublicNonogramsSince(firebaseUid: String?, since: Long): Long?

    suspend fun pullOwnedNonograms(firebaseUid: String, since: Long): Long?

    /** Moves the puzzle to `PENDING`. Returns false when the rules reject it (e.g. banned author). */
    suspend fun requestPublish(firebaseUid: String, nonogram: Nonogram): Boolean

    /** Reads the author's denial streak / ban flag; null when the read failed. */
    suspend fun fetchModerationGate(firebaseUid: String): ModerationGate?

    suspend fun isAdmin(firebaseUid: String): Boolean

    /** Admin only: the oldest pending requests, oldest first. */
    suspend fun pullPendingReviews(firebaseUid: String, limit: Int): List<Nonogram>

    /**
     * Admin only: accepts [nonogram] at [difficulty] or denies it, and updates its author's denial
     * streak. Difficulty is the reviewer's call — authors never rate their own puzzles — so it is
     * written only when approving.
     */
    suspend fun decideReview(
        firebaseUid: String,
        nonogram: Nonogram,
        approve: Boolean,
        difficulty: Difficulty,
    ): Boolean
}

/**
 * Merge policy for pulled nonograms, shared by both platform implementations: remote newer →
 * upsert locally, local newer and locally authored → push back. Returns the newest received
 * `updatedAt` timestamp for the next incremental fetch. A null [firebaseUid] is a guest's
 * unauthenticated public pull: merge in, never push back.
 *
 * The local side comes from one stub read and the winners go back in one batch, rather than a query
 * and a write per puzzle: on web every one of those is a round-trip to the database worker, and the
 * stubs carry no solution to decode.
 */
internal suspend fun SyncService.mergeRemoteNonograms(
    sdk: AppSDK,
    firebaseUid: String?,
    lastSyncedAt: Long,
    remotes: List<Nonogram>,
): Long {
    if (remotes.isEmpty()) return lastSyncedAt
    var newestReceivedAt = lastSyncedAt
    val now = Clock.System.now().toEpochMilliseconds()
    val locals = sdk.getNonogramStubs()
    val incoming = mutableListOf<Nonogram>()
    val outgoing = mutableListOf<Long>()
    for (remote in remotes) {
        if (remote.updatedAt in (newestReceivedAt + 1)..now) newestReceivedAt = remote.updatedAt
        val local = locals[remote.id]
        if (local != null && local.authorUid.isNotEmpty() && local.authorUid != remote.authorUid) {
            continue
        }
        if (local == null || local.updatedAt < remote.updatedAt) {
            incoming += remote
        } else if (
            firebaseUid != null &&
            local.updatedAt > remote.updatedAt &&
            local.authorUid == firebaseUid
        ) {
            outgoing += local.id
        }
    }
    sdk.upsertNonogramsFromRemote(incoming)
    if (firebaseUid != null) {
        for (id in outgoing) {
            // Read late: only the puzzles being pushed back need their full solution.
            val local = sdk.getNonogramById(id) ?: continue
            pushNonogram(firebaseUid, local)
        }
    }
    return newestReceivedAt
}

/**
 * Pulls the approved puzzles changed since this device's last public pull and advances the cursor.
 * False when the pull failed, leaving the cursor untouched so the next attempt retries the range.
 */
internal suspend fun SyncService.syncPublicNonograms(
    authRepository: AuthRepository,
    firebaseUid: String?,
): Boolean {
    val lastSyncedAt = authRepository.getLastPublicNonogramSyncTimestamp()
    val newestReceivedAt = pullPublicNonogramsSince(firebaseUid, lastSyncedAt) ?: return false
    if (newestReceivedAt != lastSyncedAt) {
        authRepository.setLastPublicNonogramSyncTimestamp(newestReceivedAt)
    }
    return true
}
