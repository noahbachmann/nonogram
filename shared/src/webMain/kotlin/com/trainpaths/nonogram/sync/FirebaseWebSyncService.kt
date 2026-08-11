@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.firebase.FirebaseWeb
import com.trainpaths.nonogram.firebase.ProgressQuerySnapshot
import com.trainpaths.nonogram.firebase.collection
import com.trainpaths.nonogram.firebase.doc
import com.trainpaths.nonogram.firebase.getNonogramDocs
import com.trainpaths.nonogram.firebase.getProgressDocs
import com.trainpaths.nonogram.firebase.query
import com.trainpaths.nonogram.firebase.setDoc
import com.trainpaths.nonogram.firebase.where
import com.trainpaths.nonogram.util.toBoolean
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsNumber
import kotlin.js.toJsString

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
                .await<ProgressQuerySnapshot>()
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

    override suspend fun uploadAllLocalProgress(firebaseUid: String, localUserId: Long) {
        try {
            if (!sessionMatches(firebaseUid)) return
            val allProgress = sdk.getProgressForUserWithTimestamp(localUserId)
            for (progress in allProgress) {
                pushProgress(firebaseUid, progress.nonogramId, progress.boardState, progress.updatedAt)
            }
        } catch (e: Throwable) {
            println("FirestoreSync(web): upload all failed: ${e.message}")
        }
    }

    override suspend fun pullAllProgress(firebaseUid: String, localUserId: Long) {
        try {
            if (!sessionMatches(firebaseUid)) return
            for (remote in fetchAll(firebaseUid)) {
                sdk.saveProgressWithTimestamp(localUserId, remote.nonogramId, remote.boardState, remote.updatedAt)
            }
        } catch (e: Throwable) {
            println("FirestoreSync(web): pull all failed: ${e.message}")
        }
    }

    override suspend fun pullAndMergeAllProgress(firebaseUid: String, localUserId: Long) {
        try {
            if (!sessionMatches(firebaseUid)) return
            for (remote in fetchAll(firebaseUid)) {
                val local = sdk.getSingleProgress(localUserId, remote.nonogramId)

                if (local == null || local.updatedAt < remote.updatedAt) {
                    sdk.saveProgressWithTimestamp(localUserId, remote.nonogramId, remote.boardState, remote.updatedAt)
                } else if (local.updatedAt > remote.updatedAt) {
                    pushProgress(firebaseUid, remote.nonogramId, local.boardState, local.updatedAt)
                }
            }
        } catch (e: Throwable) {
            println("FirestoreSync(web): pull and merge failed: ${e.message}")
        }
    }

    override suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram) {
        try {
            if (!sessionMatches(firebaseUid)) return
            val reference = doc(FirebaseWeb.requireFirestore(), "nonograms/${nonogram.id}")
            setDoc(
                reference,
                FirebaseWeb.makeNonogramData(
                    difficulty = nonogram.difficulty.toString(),
                    // Solution stays a JSON string: Firestore rejects nested arrays.
                    solutionJson = json.encodeToString(nonogram.solution),
                    authorUid = firebaseUid,
                    isPublic = nonogram.isPublic,
                    updatedAt = nonogram.updatedAt,
                )
            ).await()
        } catch (e: Throwable) {
            println("FirestoreSync(web): push nonogram ${nonogram.id} failed: ${e.message}")
        }
    }

    override suspend fun uploadAllLocalNonograms(firebaseUid: String, localUserId: Long) {
        try {
            if (!sessionMatches(firebaseUid)) return
            for (nonogram in sdk.getNonogramsByAuthor(localUserId)) {
                pushNonogram(firebaseUid, nonogram)
            }
        } catch (e: Throwable) {
            println("FirestoreSync(web): upload all nonograms failed: ${e.message}")
        }
    }

    override suspend fun pullNonogramsSince(firebaseUid: String, localUserId: Long, since: Long): Long {
        return try {
            if (!sessionMatches(firebaseUid)) return since
            val nonograms = collection(FirebaseWeb.requireFirestore(), "nonograms")
            val sinceValue = since.toDouble().toJsNumber()
            val publicSnapshot = getNonogramDocs(
                query(nonograms, where("status", "==", 1.0.toJsNumber()), where("updatedAt", ">", sinceValue))
            ).await()
            val ownSnapshot = getNonogramDocs(
                query(
                    nonograms,
                    where("authorUid", "==", firebaseUid.toJsString()),
                    where("updatedAt", ">", sinceValue)
                )
            ).await()

            // Collect first: the forEach callback can't suspend.
            val remotes = mutableListOf<Nonogram>()
            val seen = mutableSetOf<Long>()
            for (snapshot in listOf(publicSnapshot, ownSnapshot)) {
                snapshot.forEach { docSnapshot ->
                    val nonogramId = docSnapshot.id.toLongOrNull()
                    if (nonogramId != null && seen.add(nonogramId)) {
                        try {
                            val data = docSnapshot.data()
                            remotes += Nonogram(
                                id = nonogramId,
                                difficulty = Difficulty.valueOf(data.difficulty),
                                solution = json.decodeFromString(data.solution),
                                authorId = if (data.authorUid == firebaseUid) localUserId else 0,
                                isPublic = data.status.toLong().toBoolean(),
                                updatedAt = data.updatedAt.toLong(),
                            )
                        } catch (e: Throwable) {
                            println("FirestoreSync(web): skipping malformed nonogram doc ${docSnapshot.id}: ${e.message}")
                        }
                    }
                }
            }

            mergeRemoteNonograms(sdk, firebaseUid, localUserId, since, remotes)
        } catch (e: Throwable) {
            println("FirestoreSync(web): pull nonograms failed: ${e.message}")
            since
        }
    }
}
