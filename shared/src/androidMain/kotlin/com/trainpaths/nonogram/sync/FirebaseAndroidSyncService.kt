package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.PublishStatus
import com.trainpaths.nonogram.util.toPublishStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class FirebaseAndroidSyncService(private val sdk: AppSDK) : SyncService {

    private val firestore = Firebase.firestore
    private val json = Json

    private fun progressCollection(firebaseUid: String) =
        firestore.collection("users").document(firebaseUid).collection("progress")

    private fun nonogramsCollection() = firestore.collection("nonograms")

    private fun userDocument(firebaseUid: String) =
        firestore.collection("users").document(firebaseUid)

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

    override suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram, resetPublishStatus: Boolean) {
        try {
            val fields = buildMap<String, Any?> {
                put("difficulty", nonogram.difficulty.toString())
                // Solution stays a JSON string: Firestore rejects nested arrays.
                put("solution", json.encodeToString(nonogram.solution))
                put("name", nonogram.name)
                put("authorUid", firebaseUid)
                put("updatedAt", nonogram.updatedAt)
                if (resetPublishStatus) put("publishStatus", PublishStatus.NONE.name)
            }
            // Merge, so an ordinary save never clobbers a pending or approved publish status.
            nonogramsCollection().document(nonogram.id.toString()).set(fields, merge = true)
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
            .where { "publishStatus" equalTo PublishStatus.APPROVED.name }
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

    override suspend fun requestPublish(firebaseUid: String, nonogram: Nonogram): Boolean = try {
        nonogramsCollection().document(nonogram.id.toString()).set(
            mapOf(
                "publishStatus" to PublishStatus.PENDING.name,
                "updatedAt" to nonogram.updatedAt,
            ),
            merge = true,
        )
        true
    } catch (e: Exception) {
        println("FirestoreSync: publish request for nonogram ${nonogram.id} rejected: ${e.message}")
        false
    }

    override suspend fun fetchModerationGate(firebaseUid: String): ModerationGate? = try {
        val snapshot = userDocument(firebaseUid).get()
        if (!snapshot.exists) {
            ModerationGate()
        } else {
            val streak = snapshot.get<Long?>("denialStreak")?.toInt() ?: 0
            ModerationGate(streak, snapshot.get<Boolean?>("publishBanned") ?: isPublishBanned(streak))
        }
    } catch (e: Exception) {
        println("FirestoreSync: moderation gate read failed: ${e.message}")
        null
    }

    override suspend fun isAdmin(firebaseUid: String): Boolean = try {
        firestore.collection("admins").document(firebaseUid).get().exists
    } catch (e: Exception) {
        println("FirestoreSync: admin check failed: ${e.message}")
        false
    }

    override suspend fun pullPendingReviews(firebaseUid: String, limit: Int): List<PendingReview> = try {
        nonogramsCollection()
            .where { "publishStatus" equalTo PublishStatus.PENDING.name }
            .orderBy("updatedAt")
            .limit(limit)
            .get().documents
            .mapNotNull(::parseReview)
    } catch (e: Exception) {
        println("FirestoreSync: pending review fetch failed: ${e.message}")
        emptyList()
    }

    override suspend fun decideReview(
        firebaseUid: String,
        review: PendingReview,
        approve: Boolean,
    ): Boolean = try {
        nonogramsCollection().document(review.nonogram.id.toString()).set(
            mapOf(
                "publishStatus" to (if (approve) PublishStatus.APPROVED else PublishStatus.DENIED).name,
                "updatedAt" to Clock.System.now().toEpochMilliseconds(),
            ),
            merge = true,
        )
        val streak = nextDenialStreak(
            current = fetchModerationGate(review.authorUid)?.denialStreak ?: 0,
            approved = approve,
        )
        userDocument(review.authorUid).set(
            mapOf("denialStreak" to streak.toLong(), "publishBanned" to isPublishBanned(streak)),
            merge = true,
        )
        true
    } catch (e: Exception) {
        println("FirestoreSync: decision on nonogram ${review.nonogram.id} failed: ${e.message}")
        false
    }

    private fun parseReview(doc: DocumentSnapshot): PendingReview? = try {
        val nonogramId = doc.id.toLongOrNull()
        if (nonogramId == null) null else PendingReview(
            nonogram = Nonogram(
                id = nonogramId,
                difficulty = Difficulty.valueOf(doc.get("difficulty")),
                solution = json.decodeFromString(doc.get<String>("solution")),
                name = doc.get<String?>("name"),
                updatedAt = doc.get("updatedAt"),
                publishStatus = PublishStatus.PENDING,
            ),
            authorUid = doc.get("authorUid"),
        )
    } catch (e: Exception) {
        println("FirestoreSync: skipping malformed pending doc ${doc.id}: ${e.message}")
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
                        name = doc.get<String?>("name"),
                        authorId = if (doc.get<String>("authorUid") == firebaseUid) localUserId else 0,
                        updatedAt = doc.get("updatedAt"),
                        publishStatus = doc.publishStatus(),
                    )
                )
            } catch (e: Exception) {
                println("FirestoreSync: skipping malformed nonogram doc ${doc.id}: ${e.message}")
            }
        }
    }

    /**
     * Docs written before review existed carry no `publishStatus`, only the old numeric `status`; a
     * public one was implicitly approved. Drop this fallback once those docs are backfilled.
     */
    private fun DocumentSnapshot.publishStatus(): PublishStatus =
        get<String?>("publishStatus")?.toPublishStatus()
            ?: if (get<Long?>("status") == 1L) PublishStatus.APPROVED else PublishStatus.NONE
}
