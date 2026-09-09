package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.cache.ProgressWithTimestamp

/** A `users/{uid}/progress/{nonogramId}` document; the same three fields on both platforms. */
internal data class RemoteProgress(
    val nonogramId: Long,
    val boardState: String?,
    val updatedAt: Long,
)

/**
 * Merge policy for pulled progress, shared by both platform implementations: remote newer →
 * overwrite locally, local newer → push it back. Rows the remote has never seen are untouched;
 * [uploadAllProgress] is what carries those up.
 *
 * The local side is read once and the winners written in one batch, rather than a query and a write
 * per row: on web every one of those is a round-trip to the database worker.
 */
internal suspend fun SyncService.mergeRemoteProgress(
    sdk: AppSDK,
    firebaseUid: String,
    remotes: List<RemoteProgress>,
) {
    if (remotes.isEmpty()) return
    val locals = sdk.getProgressForUserWithTimestamp(firebaseUid).associateBy { it.nonogramId }
    val incoming = mutableListOf<ProgressWithTimestamp>()
    val outgoing = mutableListOf<ProgressWithTimestamp>()
    for ((nonogramId, boardState, updatedAt) in remotes) {
        val local = locals[nonogramId]
        if (local == null || local.updatedAt < updatedAt) {
            incoming += ProgressWithTimestamp(nonogramId, boardState, updatedAt)
        } else if (local.updatedAt > updatedAt) {
            outgoing += local
        }
    }
    sdk.saveProgressBatch(firebaseUid, incoming)
    for ((nonogramId, boardState, updatedAt) in outgoing) {
        pushProgress(firebaseUid, nonogramId, boardState, updatedAt)
    }
}

/** Remote wins outright, no comparison: the first pull onto a device that has no local rows yet. */
internal suspend fun applyRemoteProgress(
    sdk: AppSDK,
    firebaseUid: String,
    remotes: List<RemoteProgress>,
) {
    sdk.saveProgressBatch(
        firebaseUid,
        remotes.map { ProgressWithTimestamp(it.nonogramId, it.boardState, it.updatedAt) },
    )
}

/** The mirror of [applyRemoteProgress]: every local row up, for an account with nothing remote. */
internal suspend fun SyncService.uploadAllProgress(sdk: AppSDK, firebaseUid: String) {
    for ((nonogramId, boardState, updatedAt) in sdk.getProgressForUserWithTimestamp(firebaseUid)) {
        pushProgress(firebaseUid, nonogramId, boardState, updatedAt)
    }
}
