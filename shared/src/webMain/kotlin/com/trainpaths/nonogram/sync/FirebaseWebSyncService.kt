@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.PublishStatus
import com.trainpaths.nonogram.classes.isWellFormedGrid
import com.trainpaths.nonogram.firebase.FirebaseWeb
import com.trainpaths.nonogram.firebase.NonogramQuerySnapshot
import com.trainpaths.nonogram.firebase.collection
import com.trainpaths.nonogram.firebase.doc
import com.trainpaths.nonogram.firebase.getExistsDoc
import com.trainpaths.nonogram.firebase.getNonogramDocs
import com.trainpaths.nonogram.firebase.getProgressDocs
import com.trainpaths.nonogram.firebase.getUserGateDoc
import com.trainpaths.nonogram.firebase.limit as limitTo
import com.trainpaths.nonogram.firebase.orderBy
import com.trainpaths.nonogram.firebase.query
import com.trainpaths.nonogram.firebase.query3
import com.trainpaths.nonogram.firebase.setDoc
import com.trainpaths.nonogram.firebase.setDocMerged
import com.trainpaths.nonogram.firebase.where
import com.trainpaths.nonogram.util.toPublishStatus
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsNumber
import kotlin.js.toJsString
import kotlin.time.Clock

class FirebaseWebSyncService(private val sdk: AppSDK) : SyncService {

    private data class Remote(val nonogramId: Long, val boardState: String?, val updatedAt: Long)

    private val json = Json

    // Firestore rejects ops until the JS SDK restores its indexedDB session, so gate on it.
    private suspend fun sessionMatches(firebaseUid: String): Boolean {
        val liveUid = FirebaseWeb.awaitSignedInUid()
        if (liveUid != firebaseUid) {
            println("FirestoreSync(web): no live session for $firebaseUid (have $liveUid)")
        }
        return liveUid == firebaseUid
    }

    private suspend fun fetchAll(firebaseUid: String): List<Remote> {
        val snapshot =
            getProgressDocs(collection(FirebaseWeb.requireFirestore(), "users/$firebaseUid/progress"))
                .await()
        val result = mutableListOf<Remote>()
        snapshot.forEach { docSnapshot ->
            val nonogramId = docSnapshot.id.toLongOrNull()
            if (nonogramId != null) {
                val data = docSnapshot.data()
                result += Remote(nonogramId, data.boardState, data.updatedAt.toLong())
            }
        }
        return result
    }

    override suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long) {
        try {
            if (!sessionMatches(firebaseUid)) return
            val reference = doc(FirebaseWeb.requireFirestore(), "users/$firebaseUid/progress/$nonogramId")
            setDoc(reference, FirebaseWeb.makeProgressData(boardState, updatedAt)).await()
        } catch (e: Throwable) {
            println("FirestoreSync(web): push failed for nonogram $nonogramId: ${e.message}")
        }
    }

    override suspend fun hasRemoteProgress(firebaseUid: String): Boolean {
        return try {
            if (!sessionMatches(firebaseUid)) return false
            fetchAll(firebaseUid).isNotEmpty()
        } catch (e: Throwable) {
            println("FirestoreSync(web): check remote failed: ${e.message}")
            false
        }
    }

    override suspend fun uploadAllLocalProgress(firebaseUid: String) {
        try {
            if (!sessionMatches(firebaseUid)) return
            val allProgress = sdk.getProgressForUserWithTimestamp(firebaseUid)
            for ((nonogramId, boardState, updatedAt) in allProgress) {
                pushProgress(firebaseUid, nonogramId, boardState, updatedAt)
            }
        } catch (e: Throwable) {
            println("FirestoreSync(web): upload all failed: ${e.message}")
        }
    }

    override suspend fun pullAllProgress(firebaseUid: String) {
        try {
            if (!sessionMatches(firebaseUid)) return
            for ((nonogramId, boardState, updatedAt) in fetchAll(firebaseUid)) {
                sdk.saveProgressWithTimestamp(firebaseUid, nonogramId, boardState, updatedAt)
            }
        } catch (e: Throwable) {
            println("FirestoreSync(web): pull all failed: ${e.message}")
        }
    }

    override suspend fun pullAndMergeAllProgress(firebaseUid: String) {
        try {
            if (!sessionMatches(firebaseUid)) return
            for ((nonogramId, boardState, updatedAt) in fetchAll(firebaseUid)) {
                val local = sdk.getSingleProgress(firebaseUid, nonogramId)

                if (local == null || local.updatedAt < updatedAt) {
                    sdk.saveProgressWithTimestamp(firebaseUid, nonogramId, boardState, updatedAt)
                } else if (local.updatedAt > updatedAt) {
                    pushProgress(firebaseUid, nonogramId, local.boardState, local.updatedAt)
                }
            }
        } catch (e: Throwable) {
            println("FirestoreSync(web): pull and merge failed: ${e.message}")
        }
    }

    override suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram, resetPublishStatus: Boolean) {
        try {
            if (!sessionMatches(firebaseUid)) return
            val reference = doc(FirebaseWeb.requireFirestore(), "nonograms/${nonogram.id}")
            // Merge, so an ordinary save never clobbers a pending or approved publish status.
            setDocMerged(
                reference,
                FirebaseWeb.makeNonogramData(
                    difficulty = nonogram.difficulty.toString(),
                    // Solution stays a JSON string: Firestore rejects nested arrays.
                    solutionJson = json.encodeToString(nonogram.solution),
                    name = nonogram.name,
                    authorUid = firebaseUid,
                    updatedAt = nonogram.updatedAt,
                    publishStatus = if (resetPublishStatus) PublishStatus.NONE.name else null,
                ),
                FirebaseWeb.mergeOptions(),
            ).await()
        } catch (e: Throwable) {
            println("FirestoreSync(web): push nonogram ${nonogram.id} failed: ${e.message}")
        }
    }

    override suspend fun uploadAllLocalNonograms(firebaseUid: String) {
        try {
            if (!sessionMatches(firebaseUid)) return
            for (nonogram in sdk.getNonogramsByAuthor(firebaseUid)) {
                pushNonogram(firebaseUid, nonogram)
            }
        } catch (e: Throwable) {
            println("FirestoreSync(web): upload all nonograms failed: ${e.message}")
        }
    }

    override suspend fun pullPublicNonogramsSince(firebaseUid: String?, since: Long): Long? {
        if (firebaseUid != null) FirebaseWeb.awaitSignedInUid()

        val nonograms = collection(FirebaseWeb.requireFirestore(), "nonograms")
        val updatedAfterValue = since.toDouble().toJsNumber()
        return try {
            val snapshot = getNonogramDocs(
                query(
                    nonograms,
                    where("publishStatus", "==", PublishStatus.APPROVED.name.toJsString()),
                    where("updatedAt", ">", updatedAfterValue),
                )
            ).await()
            mergeRemoteNonograms(sdk, firebaseUid, since, parseNonograms(snapshot))
        } catch (e: Throwable) {
            println("FirestoreSync(web): pull public nonograms for puzzle list failed: ${e.message}")
            null
        }
    }

    override suspend fun pullOwnedNonograms(firebaseUid: String, since: Long): Long? {
        if (!sessionMatches(firebaseUid)) return null

        val nonograms = collection(FirebaseWeb.requireFirestore(), "nonograms")
        val updatedAfterValue = since.toDouble().toJsNumber()
        return try {
            val snapshot = getNonogramDocs(
                query(
                    nonograms,
                    where("authorUid", "==", firebaseUid.toJsString()),
                    where("updatedAt", ">", updatedAfterValue)
                )
            ).await()
            mergeRemoteNonograms(sdk, firebaseUid, since, parseNonograms(snapshot))
        } catch (e: Throwable) {
            println("FirestoreSync(web): pull owned nonograms for Generator failed: ${e.message}")
            null
        }
    }

    override suspend fun requestPublish(firebaseUid: String, nonogram: Nonogram): Boolean = try {
        if (!sessionMatches(firebaseUid)) {
            false
        } else {
            setDocMerged(
                doc(FirebaseWeb.requireFirestore(), "nonograms/${nonogram.id}"),
                FirebaseWeb.makePublishStatusData(PublishStatus.PENDING.name, nonogram.updatedAt),
                FirebaseWeb.mergeOptions(),
            ).await()
            true
        }
    } catch (e: Throwable) {
        println("FirestoreSync(web): publish request for nonogram ${nonogram.id} rejected: ${e.message}")
        false
    }

    override suspend fun fetchModerationGate(firebaseUid: String): ModerationGate? = try {
        if (!sessionMatches(firebaseUid)) {
            null
        } else {
            readModerationGate(firebaseUid)
        }
    } catch (e: Throwable) {
        println("FirestoreSync(web): moderation gate read failed: ${e.message}")
        null
    }

    private suspend fun readModerationGate(uid: String): ModerationGate {
        val snapshot = getUserGateDoc(doc(FirebaseWeb.requireFirestore(), "users/$uid"))
            .await()
        if (!snapshot.exists()) return ModerationGate()
        val data = snapshot.data() ?: return ModerationGate()
        val streak = data.denialStreak?.toInt() ?: 0
        return ModerationGate(streak, data.publishBanned ?: isPublishBanned(streak))
    }

    override suspend fun isAdmin(firebaseUid: String): Boolean = try {
        if (!sessionMatches(firebaseUid)) {
            false
        } else {
            getExistsDoc(doc(FirebaseWeb.requireFirestore(), "admins/$firebaseUid"))
                .await()
                .exists()
        }
    } catch (e: Throwable) {
        println("FirestoreSync(web): admin check failed: ${e.message}")
        false
    }

    override suspend fun pullPendingReviews(firebaseUid: String, limit: Int): List<Nonogram> = try {
        if (!sessionMatches(firebaseUid)) {
            emptyList()
        } else {
            parseNonograms(
                getNonogramDocs(
                    query3(
                        collection(FirebaseWeb.requireFirestore(), "nonograms"),
                        where("publishStatus", "==", PublishStatus.PENDING.name.toJsString()),
                        orderBy("updatedAt"),
                        limitTo(limit),
                    )
                ).await()
            )
        }
    } catch (e: Throwable) {
        println("FirestoreSync(web): pending review fetch failed: ${e.message}")
        emptyList()
    }

    override suspend fun decideReview(
        firebaseUid: String,
        nonogram: Nonogram,
        approve: Boolean,
    ): Boolean = try {
        if (!sessionMatches(firebaseUid)) {
            false
        } else {
            val firestore = FirebaseWeb.requireFirestore()
            setDocMerged(
                doc(firestore, "nonograms/${nonogram.id}"),
                FirebaseWeb.makePublishStatusData(
                    publishStatus = (if (approve) PublishStatus.APPROVED else PublishStatus.DENIED).name,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                ),
                FirebaseWeb.mergeOptions(),
            ).await()
            val streak = nextDenialStreak(readModerationGate(nonogram.authorUid).denialStreak, approve)
            setDocMerged(
                doc(firestore, "users/${nonogram.authorUid}"),
                FirebaseWeb.makeUserGateData(streak, isPublishBanned(streak)),
                FirebaseWeb.mergeOptions(),
            ).await()
            true
        }
    } catch (e: Throwable) {
        println("FirestoreSync(web): decision on nonogram ${nonogram.id} failed: ${e.message}")
        false
    }

    private fun parseNonograms(snapshot: NonogramQuerySnapshot): List<Nonogram> = buildList {
        // The JS forEach callback cannot suspend, so parsing stays synchronous here.
        snapshot.forEach { docSnapshot ->
            val nonogramId = docSnapshot.id.toLongOrNull()
            if (nonogramId != null) {
                try {
                    val data = docSnapshot.data()
                    val solution: List<List<Int>> = json.decodeFromString(data.solution)
                    require(solution.isWellFormedGrid()) { "grid out of range or ragged" }
                    add(
                        Nonogram(
                            id = nonogramId,
                            difficulty = Difficulty.valueOf(data.difficulty),
                            solution = solution,
                            name = data.name,
                            authorUid = data.authorUid ?: "",
                            updatedAt = data.updatedAt.toLong(),
                        )
                    )
                } catch (e: Throwable) {
                    println("FirestoreSync(web): skipping malformed nonogram doc ${docSnapshot.id}: ${e.message}")
                }
            }
        }
    }
}
