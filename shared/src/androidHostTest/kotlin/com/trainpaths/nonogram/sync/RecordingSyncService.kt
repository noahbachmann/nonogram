package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram

internal data class Push(
    val firebaseUid: String,
    val nonogramId: Long,
    val boardState: String?,
    val updatedAt: Long,
)

/**
 * Records the two writes the shared merges are allowed to make; everything else is unreachable.
 *
 * Both platform services contribute only a fetch and a document write, so this stands in for either
 * of them when the merge policy in `SyncService.kt` / `RemoteProgress.kt` is what is under test.
 */
internal class RecordingSyncService : SyncService {

    val pushed = mutableListOf<Push>()
    val pushedNonograms = mutableListOf<Nonogram>()

    override suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long) {
        pushed += Push(firebaseUid, nonogramId, boardState, updatedAt)
    }

    override suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram, writePublishStatus: Boolean) {
        pushedNonograms += nonogram
    }

    override suspend fun hasRemoteProgress(firebaseUid: String) = unused()
    override suspend fun uploadAllLocalProgress(firebaseUid: String) = unused()
    override suspend fun pullAllProgress(firebaseUid: String) = unused()
    override suspend fun pullAndMergeAllProgress(firebaseUid: String) = unused()
    override suspend fun uploadAllLocalNonograms(firebaseUid: String) = unused()
    override suspend fun pullPublicNonogramsSince(firebaseUid: String?, since: Long) = unused()
    override suspend fun pullOwnedNonograms(firebaseUid: String, since: Long) = unused()
    override suspend fun requestPublish(firebaseUid: String, nonogram: Nonogram) = unused()
    override suspend fun fetchModerationGate(firebaseUid: String) = unused()
    override suspend fun isAdmin(firebaseUid: String) = unused()
    override suspend fun pullPendingReviews(firebaseUid: String, limit: Int) = unused()
    override suspend fun decideReview(
        firebaseUid: String,
        nonogram: Nonogram,
        approve: Boolean,
        difficulty: Difficulty,
    ) = unused()

    private fun unused(): Nothing = error("not part of the merge under test")
}
