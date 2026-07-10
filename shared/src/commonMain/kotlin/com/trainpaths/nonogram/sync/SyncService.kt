package com.trainpaths.nonogram.sync

interface SyncService {
    suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long)
    suspend fun hasRemoteProgress(firebaseUid: String): Boolean
    suspend fun uploadAllLocalProgress(firebaseUid: String, localUserId: Long)
    suspend fun pullAllProgress(firebaseUid: String, localUserId: Long)
    suspend fun pullAndMergeAllProgress(firebaseUid: String, localUserId: Long)
}
