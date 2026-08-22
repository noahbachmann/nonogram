@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

// Firestore numbers are doubles; epoch millis fit losslessly.
internal external interface ProgressDocData : JsAny {
    val boardState: String?
    val updatedAt: Double
}

internal external interface NonogramDocData : JsAny {
    val difficulty: String
    val solution: String
    val name: String?
    val authorUid: String
    val updatedAt: Double
    val publishStatus: String?

    /** Legacy visibility flag, read only to classify docs written before review existed. */
    val status: Double?
}

internal external interface UserGateDocData : JsAny {
    val denialStreak: Double?
    val publishBanned: Boolean?
}

// Global JSON — the portable way to build plain JS objects from webMain, where js(...) is unavailable.
internal external object JSON : JsAny {
    fun parse(text: String): JsAny?
}
