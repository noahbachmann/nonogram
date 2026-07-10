@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.firebase.FirebaseWeb
import com.trainpaths.nonogram.firebase.collection
import com.trainpaths.nonogram.firebase.doc
import com.trainpaths.nonogram.firebase.QuerySnapshot
import com.trainpaths.nonogram.firebase.getDocs
import com.trainpaths.nonogram.firebase.setDoc
import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop

class FirebaseWebSyncService(private val sdk: AppSDK) : SyncService {

    private data class Remote(val nonogramId: Long, val boardState: String?, val updatedAt: Long)

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
            getDocs(collection(FirebaseWeb.requireFirestore(), "users/$firebaseUid/progress")).await<QuerySnapshot>()
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
}
