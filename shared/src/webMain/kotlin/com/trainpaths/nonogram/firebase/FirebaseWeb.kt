@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import com.trainpaths.nonogram.util.toLong
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
        authorUid: String,
        isPublic: Boolean,
        updatedAt: Long,
    ): JsAny =
        JSON.parse(
            buildJsonObject {
                put("difficulty", difficulty)
                put("solution", solutionJson)
                put("authorUid", authorUid)
                put("status", isPublic.toLong())
                put("updatedAt", updatedAt)
            }.toString()
        )!!
}
