@file:JsModule("firebase/firestore")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.JsName
import kotlin.js.Promise

internal external interface Firestore : JsAny

internal external interface DocumentReference : JsAny

internal external interface Query : JsAny

internal external interface CollectionReference : Query

internal external interface QueryConstraint : JsAny

internal external interface ProgressDocSnapshot : JsAny {
    val id: String
    fun data(): ProgressDocData
}

// forEach: JsArray element access differs between js and wasmJs.
internal external interface ProgressQuerySnapshot : JsAny {
    val empty: Boolean
    fun forEach(callback: (ProgressDocSnapshot) -> Unit)
}

internal external interface NonogramDocSnapshot : JsAny {
    val id: String
    fun data(): NonogramDocData
}

internal external interface NonogramQuerySnapshot : JsAny {
    fun forEach(callback: (NonogramDocSnapshot) -> Unit)
}

internal external fun getFirestore(app: FirebaseApp): Firestore

internal external fun doc(firestore: Firestore, path: String): DocumentReference

internal external fun collection(firestore: Firestore, path: String): CollectionReference

internal external fun setDoc(reference: DocumentReference, data: JsAny): Promise<JsAny?>

internal external fun query(base: Query, c1: QueryConstraint, c2: QueryConstraint): Query

internal external fun where(fieldPath: String, opStr: String, value: JsAny): QueryConstraint

@JsName("getDocs")
internal external fun getProgressDocs(query: Query): Promise<ProgressQuerySnapshot>

@JsName("getDocs")
internal external fun getNonogramDocs(query: Query): Promise<NonogramQuerySnapshot>
