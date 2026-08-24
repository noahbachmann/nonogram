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

internal external interface ExistsDocSnapshot : JsAny {
    fun exists(): Boolean
}

internal external interface UserGateDocSnapshot : JsAny {
    fun exists(): Boolean
    fun data(): UserGateDocData?
}

internal external fun getFirestore(app: FirebaseApp): Firestore

internal external fun doc(firestore: Firestore, path: String): DocumentReference

internal external fun collection(firestore: Firestore, path: String): CollectionReference

internal external fun setDoc(reference: DocumentReference, data: JsAny): Promise<JsAny?>

// The JS `setDoc`/`query` are variadic; externals need one declaration per arity.
@JsName("setDoc")
internal external fun setDocMerged(
    reference: DocumentReference,
    data: JsAny,
    options: JsAny,
): Promise<JsAny?>

internal external fun query(base: Query, c1: QueryConstraint, c2: QueryConstraint): Query

@JsName("query")
internal external fun query3(
    base: Query,
    c1: QueryConstraint,
    c2: QueryConstraint,
    c3: QueryConstraint,
): Query

internal external fun where(fieldPath: String, opStr: String, value: JsAny): QueryConstraint

internal external fun orderBy(fieldPath: String): QueryConstraint

internal external fun limit(limit: Int): QueryConstraint

@JsName("getDocs")
internal external fun getProgressDocs(query: Query): Promise<ProgressQuerySnapshot>

@JsName("getDocs")
internal external fun getNonogramDocs(query: Query): Promise<NonogramQuerySnapshot>

@JsName("getDoc")
internal external fun getExistsDoc(reference: DocumentReference): Promise<ExistsDocSnapshot>

@JsName("getDoc")
internal external fun getUserGateDoc(reference: DocumentReference): Promise<UserGateDocSnapshot>
