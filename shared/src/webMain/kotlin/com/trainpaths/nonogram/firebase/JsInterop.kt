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
    val difficulty: String?
    val solution: String?
    val name: String?
    val authorUid: String?
    val updatedAt: Double?
    val publishStatus: String?
}

internal external interface UserGateDocData : JsAny {
    val denialStreak: Double?
    val publishBanned: Boolean?
}

// Global JSON — the portable way to build plain JS objects from webMain, where js(...) is unavailable.
internal external object JSON : JsAny {
    fun parse(text: String): JsAny?
}

internal external interface JsLocation : JsAny {
    val hostname: String
}

internal external val location: JsLocation

internal external interface GlobalScope : JsAny {
    var FIREBASE_APPCHECK_DEBUG_TOKEN: Boolean
}

internal external val globalThis: GlobalScope
