@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlinx.coroutines.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

data class WebFirebaseUser(val uid: String, val displayName: String?)

/**
 * Facade over the handwritten Firebase JS SDK bindings. Everything outside this
 * package talks to Firebase only through here
 */
object FirebaseWeb {

    private var auth: Auth? = null
    private var firestore: Firestore? = null

    fun initialize(
        apiKey: String,
        authDomain: String,
        projectId: String,
        messagingSenderId: String,
        appId: String,
    ) {
        val options = JSON.parse(
            buildJsonObject {
                put("apiKey", apiKey)
                put("authDomain", authDomain)
                put("projectId", projectId)
                put("messagingSenderId", messagingSenderId)
                put("appId", appId)
            }.toString()
        )!!
        val app = initializeApp(options)
        auth = getAuth(app)
        firestore = getFirestore(app)
    }

    suspend fun signInWithGoogle(idToken: String?, accessToken: String?): WebFirebaseUser {
        val credential = GoogleAuthProvider.credential(idToken, accessToken)
        val user = signInWithCredential(requireNotNull(auth), credential).await<UserCredential>().user
        return WebFirebaseUser(user.uid, user.displayName)
    }

    suspend fun signOut() {
        val currentAuth = auth ?: return
        signOut(currentAuth).await<JsAny?>()
    }

    /** Waits for the indexedDB session restore; null when signed out. */
    suspend fun awaitSignedInUid(): String? {
        val auth = auth ?: return null
        auth.authStateReady().await()
        return auth.currentUser?.uid
    }

    internal fun requireFirestore(): Firestore =
        requireNotNull(firestore) { "FirebaseWeb.initialize not called" }

    internal fun makeProgressData(boardState: String?, updatedAt: Long): JsAny =
        JSON.parse(
            buildJsonObject {
                put("boardState", boardState)
                put("updatedAt", updatedAt)
            }.toString()
        )!!

    internal fun makeNonogramData(
        difficulty: String,
        solutionJson: String,
        name: String?,
        authorUid: String,
        updatedAt: Long,
        publishStatus: String?,
    ): JsAny =
        JSON.parse(
            buildJsonObject {
                put("difficulty", difficulty)
                put("solution", solutionJson)
                put("name", name)
                put("authorUid", authorUid)
                put("updatedAt", updatedAt)
                // Omitted unless the caller means to reset it: a merge write must not clobber it.
                if (publishStatus != null) put("publishStatus", publishStatus)
            }.toString()
        )!!

    internal fun makePublishStatusData(publishStatus: String, updatedAt: Long): JsAny =
        JSON.parse(
            buildJsonObject {
                put("publishStatus", publishStatus)
                put("updatedAt", updatedAt)
            }.toString()
        )!!

    internal fun makeUserGateData(denialStreak: Int, publishBanned: Boolean): JsAny =
        JSON.parse(
            buildJsonObject {
                put("denialStreak", denialStreak)
                put("publishBanned", publishBanned)
            }.toString()
        )!!

    internal fun mergeOptions(): JsAny =
        JSON.parse(buildJsonObject { put("merge", true) }.toString())!!
}
