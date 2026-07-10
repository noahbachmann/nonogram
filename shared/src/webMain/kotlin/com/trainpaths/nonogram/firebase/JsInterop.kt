@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

// Firestore numbers are doubles; epoch millis fit losslessly.
internal external interface ProgressDocData : JsAny {
    val boardState: String?
    val updatedAt: Double
}

// Global JSON — the portable way to build plain JS objects from webMain, where js(...) is unavailable.
internal external object JSON : JsAny {
    fun parse(text: String): JsAny?
}
