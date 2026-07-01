package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

class FirestoreSyncService(private val sdk: AppSDK) {

    private val firestore = Firebase.firestore

    private fun progressCollection(firebaseUid: String) =
        firestore.collection("users").document(firebaseUid).collection("progress")

    suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long) {
        try {
            progressCollection(firebaseUid)
                .document(nonogramId.toString())
                .set(mapOf("boardState" to boardState, "updatedAt" to updatedAt))
        } catch (e: Exception) {
            println("FirestoreSync: push failed for nonogram $nonogramId: ${e.message}")
        }
    }

    suspend fun hasRemoteProgress(firebaseUid: String): Boolean {
        return try {
            val docs = progressCollection(firebaseUid).get()
            docs.documents.isNotEmpty()
        } catch (e: Exception) {
            println("FirestoreSync: check remote failed: ${e.message}")
            false
        }
    }

    suspend fun uploadAllLocalProgress(firebaseUid: String, localUserId: Long) {
        try {
            val allProgress = sdk.getProgressForUserWithTimestamp(localUserId)
            for (progress in allProgress) {
                pushProgress(firebaseUid, progress.nonogramId, progress.boardState, progress.updatedAt)
            }
        } catch (e: Exception) {
            println("FirestoreSync: upload all failed: ${e.message}")
        }
    }

    suspend fun pullAllProgress(firebaseUid: String, localUserId: Long) {
        try {
            val remoteDocuments = progressCollection(firebaseUid).get()
            for (doc in remoteDocuments.documents) {
                val nonogramId = doc.id.toLongOrNull() ?: continue
                val remoteBoardState = doc.get<String?>("boardState")
                val remoteUpdatedAt = doc.get<Long>("updatedAt")
                sdk.saveProgressWithTimestamp(localUserId, nonogramId, remoteBoardState, remoteUpdatedAt)
            }
        } catch (e: Exception) {
            println("FirestoreSync: pull all failed: ${e.message}")
        }
    }

    suspend fun pullAndMergeAllProgress(firebaseUid: String, localUserId: Long) {
        try {
            val remoteDocuments = progressCollection(firebaseUid).get()
            for (doc in remoteDocuments.documents) {
                val nonogramId = doc.id.toLongOrNull() ?: continue
                val remoteBoardState = doc.get<String?>("boardState")
                val remoteUpdatedAt = doc.get<Long>("updatedAt")

                val local = sdk.getSingleProgress(localUserId, nonogramId)

                if (local == null || local.updatedAt < remoteUpdatedAt) {
                    sdk.saveProgressWithTimestamp(localUserId, nonogramId, remoteBoardState, remoteUpdatedAt)
                } else if (local.updatedAt > remoteUpdatedAt) {
                    pushProgress(firebaseUid, nonogramId, local.boardState, local.updatedAt)
                }
            }
        } catch (e: Exception) {
            println("FirestoreSync: pull and merge failed: ${e.message}")
        }
    }
}
