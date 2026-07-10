package com.trainpaths.nonogram.sync

class NoOpSyncService : SyncService {
    override suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long) {}
    override suspend fun hasRemoteProgress(firebaseUid: String): Boolean = false
    override suspend fun uploadAllLocalProgress(firebaseUid: String, localUserId: Long) {}
    override suspend fun pullAllProgress(firebaseUid: String, localUserId: Long) {}
    override suspend fun pullAndMergeAllProgress(firebaseUid: String, localUserId: Long) {}
}
