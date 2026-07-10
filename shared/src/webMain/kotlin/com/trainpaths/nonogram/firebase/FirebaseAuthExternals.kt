@file:JsModule("firebase/auth")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal external interface Auth : JsAny {
    val currentUser: FbUser?
    fun authStateReady(): Promise<JsAny?>
}

internal external interface FbUser : JsAny {
    val uid: String
    val displayName: String?
}

internal external interface AuthCredential : JsAny

internal external interface UserCredential : JsAny {
    val user: FbUser
}

internal external fun getAuth(app: FirebaseApp): Auth

internal external fun signInWithCredential(auth: Auth, credential: AuthCredential): Promise<UserCredential>

// The Firebase JS class is only used for its statics, so an external object suffices.
internal external object GoogleAuthProvider : JsAny {
    fun credential(idToken: String?, accessToken: String?): AuthCredential
}
