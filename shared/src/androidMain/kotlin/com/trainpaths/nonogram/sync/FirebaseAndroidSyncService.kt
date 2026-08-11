package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.util.toBoolean
import com.trainpaths.nonogram.util.toLong
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.json.Json

class FirebaseAndroidSyncService(private val sdk: AppSDK) : SyncService {

    private val firestore = Firebase.firestore
    private val json = Json

    private fun progressCollection(firebaseUid: String) =
        firestore.collection("users").document(firebaseUid).collection("progress")

    private fun nonogramsCollection() = firestore.collection("nonograms")

    override suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long) {
        try {
            progressCollection(firebaseUid)
                .document(nonogramId.toString())
                .set(mapOf("boardState" to boardState, "updatedAt" to updatedAt))
        } catch (e: Exception) {
            println("FirestoreSync: push failed for nonogram $nonogramId: ${e.message}")
        }
    }

    override suspend fun hasRemoteProgress(firebaseUid: String): Boolean {
        return try {
            val docs = progressCollection(firebaseUid).get()
            docs.documents.isNotEmpty()
        } catch (e: Exception) {
            println("FirestoreSync: check remote failed: ${e.message}")
            false
        }
    }

    override suspend fun uploadAllLocalProgress(firebaseUid: String, localUserId: Long) {
        try {
            val allProgress = sdk.getProgressForUserWithTimestamp(localUserId)
            for ((nonogramId, boardState, updatedAt) in allProgress) {
                pushProgress(firebaseUid, nonogramId, boardState, updatedAt)
            }
        } catch (e: Exception) {
            println("FirestoreSync: upload all failed: ${e.message}")
        }
    }

    override suspend fun pullAllProgress(firebaseUid: String, localUserId: Long) {
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

    override suspend fun pullAndMergeAllProgress(firebaseUid: String, localUserId: Long) {
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

    override suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram) {
        try {
            nonogramsCollection().document(nonogram.id.toString()).set(
                mapOf(
                    "difficulty" to nonogram.difficulty.toString(),
                    // Solution stays a JSON string: Firestore rejects nested arrays.
                    "solution" to json.encodeToString(nonogram.solution),
                    "authorUid" to firebaseUid,
                    "status" to nonogram.isPublic.toLong(),
                    "updatedAt" to nonogram.updatedAt,
                )
            )
        } catch (e: Exception) {
            println("FirestoreSync: push nonogram ${nonogram.id} failed: ${e.message}")
        }
    }

    override suspend fun uploadAllLocalNonograms(firebaseUid: String, localUserId: Long) {
        try {
            for (nonogram in sdk.getNonogramsByAuthor(localUserId)) {
                pushNonogram(firebaseUid, nonogram)
            }
        } catch (e: Exception) {
            println("FirestoreSync: upload all nonograms failed: ${e.message}")
        }
    }

    override suspend fun pullPublicNonogramsSince(
        firebaseUid: String,
        localUserId: Long,
        since: Long,
    ): Long? = try {
        val documents = nonogramsCollection()
            .where { "status" equalTo 1L }
            .where { "updatedAt" greaterThan since }
            .get().documents
        mergeRemoteNonograms(
            sdk, firebaseUid, localUserId, since,
            parseNonograms(documents, firebaseUid, localUserId),
        )
    } catch (e: Exception) {
        println("FirestoreSync: pull public nonograms for puzzle list failed: ${e.message}")
        null
    }

    override suspend fun pullOwnedNonograms(
        firebaseUid: String,
        localUserId: Long,
        since: Long,
    ): Long? = try {
        val documents = nonogramsCollection()
            .where { "authorUid" equalTo firebaseUid }
            .where { "updatedAt" greaterThan since }
            .get().documents
        mergeRemoteNonograms(
            sdk, firebaseUid, localUserId, since,
            parseNonograms(documents, firebaseUid, localUserId),
        )
    } catch (e: Exception) {
        println("FirestoreSync: pull owned nonograms for Generator failed: ${e.message}")
        null
    }

    private fun parseNonograms(
        documents: List<DocumentSnapshot>,
        firebaseUid: String,
        localUserId: Long,
    ): List<Nonogram> = buildList {
        for (doc in documents) {
            val nonogramId = doc.id.toLongOrNull() ?: continue
            try {
                add(
                    Nonogram(
                        id = nonogramId,
                        difficulty = Difficulty.valueOf(doc.get("difficulty")),
                        solution = json.decodeFromString(doc.get<String>("solution")),
                        authorId = if (doc.get<String>("authorUid") == firebaseUid) localUserId else 0,
                        isPublic = doc.get<Long>("status").toBoolean(),
                        updatedAt = doc.get("updatedAt"),
                    )
                )
            } catch (e: Exception) {
                println("FirestoreSync: skipping malformed nonogram doc ${doc.id}: ${e.message}")
            }
        }
    }
}
