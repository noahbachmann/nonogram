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

    suspend fun pullPublicNonogramsSince(
        firebaseUid: String,
        localUserId: Long,
        since: Long,
    ): Long?

    suspend fun pullOwnedNonograms(
        firebaseUid: String,
        localUserId: Long,
        since: Long,
    ): Long?
}

/**
 * Merge policy for pulled nonograms, shared by both platform implementations: remote newer →
 * upsert locally, local newer and locally authored → push back. Returns the newest received
 * `updatedAt` timestamp for the next incremental fetch.
 */
internal suspend fun SyncService.mergeRemoteNonograms(
    sdk: AppSDK,
    firebaseUid: String,
    localUserId: Long,
    lastSyncedAt: Long,
    remotes: List<Nonogram>,
): Long {
    var newestReceivedAt = lastSyncedAt
    for (remote in remotes) {
        if (remote.updatedAt > newestReceivedAt) newestReceivedAt = remote.updatedAt
        val local = sdk.getNonogramById(remote.id)
        if (local == null || local.updatedAt < remote.updatedAt) {
            sdk.upsertNonogramFromRemote(remote)
        } else if (local.updatedAt > remote.updatedAt && local.authorId == localUserId) {
            pushNonogram(firebaseUid, local)
        }
    }
    return newestReceivedAt
}
