package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.classes.Nonogram

interface SyncService {
    suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long)
    suspend fun hasRemoteProgress(firebaseUid: String): Boolean
    suspend fun uploadAllLocalProgress(firebaseUid: String, localUserId: Long)
    suspend fun pullAllProgress(firebaseUid: String, localUserId: Long)
    suspend fun pullAndMergeAllProgress(firebaseUid: String, localUserId: Long)

    /** Writes the puzzle to the shared `nonograms` collection, authored by [firebaseUid]. */
    suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram)

    /** Pushes every locally authored puzzle; used once when an account first signs in. */
    suspend fun uploadAllLocalNonograms(firebaseUid: String, localUserId: Long)

    /**
     * Pulls puzzles visible to this user (own + public) whose remote updatedAt is after [since]
     * and merges them into the local db. Returns the new sync cursor (max updatedAt seen), or
     * [since] when nothing new / on failure.
     */
    suspend fun pullNonogramsSince(firebaseUid: String, localUserId: Long, since: Long): Long
}

/**
 * Merge policy for pulled nonograms, shared by both platform implementations: remote newer →
 * upsert locally, local newer and locally authored → push back. Returns the advanced cursor.
 */
internal suspend fun SyncService.mergeRemoteNonograms(
    sdk: AppSDK,
    firebaseUid: String,
    localUserId: Long,
    since: Long,
    remotes: List<Nonogram>,
): Long {
    var cursor = since
    for (remote in remotes) {
        if (remote.updatedAt > cursor) cursor = remote.updatedAt
        val local = sdk.getNonogramById(remote.id)
        if (local == null || local.updatedAt < remote.updatedAt) {
            sdk.upsertNonogramFromRemote(remote)
        } else if (local.updatedAt > remote.updatedAt && local.authorId == localUserId) {
            pushNonogram(firebaseUid, local)
        }
    }
    return cursor
}
