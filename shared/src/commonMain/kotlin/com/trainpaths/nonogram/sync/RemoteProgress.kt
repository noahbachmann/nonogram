package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK

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
 */
internal suspend fun SyncService.mergeRemoteProgress(
    sdk: AppSDK,
    firebaseUid: String,
    remotes: List<RemoteProgress>,
) {
    for ((nonogramId, boardState, updatedAt) in remotes) {
        val local = sdk.getSingleProgress(firebaseUid, nonogramId)
        if (local == null || local.updatedAt < updatedAt) {
            sdk.saveProgressWithTimestamp(firebaseUid, nonogramId, boardState, updatedAt)
        } else if (local.updatedAt > updatedAt) {
            pushProgress(firebaseUid, nonogramId, local.boardState, local.updatedAt)
        }
    }
}

/** Remote wins outright, no comparison: the first pull onto a device that has no local rows yet. */
internal suspend fun applyRemoteProgress(
    sdk: AppSDK,
    firebaseUid: String,
    remotes: List<RemoteProgress>,
) {
    for ((nonogramId, boardState, updatedAt) in remotes) {
        sdk.saveProgressWithTimestamp(firebaseUid, nonogramId, boardState, updatedAt)
    }
}

/** The mirror of [applyRemoteProgress]: every local row up, for an account with nothing remote. */
internal suspend fun SyncService.uploadAllProgress(sdk: AppSDK, firebaseUid: String) {
    for ((nonogramId, boardState, updatedAt) in sdk.getProgressForUserWithTimestamp(firebaseUid)) {
        pushProgress(firebaseUid, nonogramId, boardState, updatedAt)
    }
}
