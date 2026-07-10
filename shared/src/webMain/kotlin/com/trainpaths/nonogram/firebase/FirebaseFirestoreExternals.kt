@file:JsModule("firebase/firestore")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.Promise

internal external interface Firestore : JsAny

internal external interface DocumentReference : JsAny

internal external interface CollectionReference : JsAny

internal external interface QueryDocumentSnapshot : JsAny {
    val id: String
    fun data(): ProgressDocData
}

// forEach instead of .docs: JsArray element access differs between js and wasmJs.
internal external interface QuerySnapshot : JsAny {
    val empty: Boolean
    fun forEach(callback: (QueryDocumentSnapshot) -> Unit)
}

internal external fun getFirestore(app: FirebaseApp): Firestore

internal external fun doc(firestore: Firestore, path: String): DocumentReference

internal external fun collection(firestore: Firestore, path: String): CollectionReference

internal external fun setDoc(reference: DocumentReference, data: JsAny): Promise<JsAny?>

internal external fun getDocs(reference: CollectionReference): Promise<QuerySnapshot>
