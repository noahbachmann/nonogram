@file:JsModule("firebase/app")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlin.js.ExperimentalWasmJsInterop

import kotlin.js.JsAny
import kotlin.js.JsModule

internal external interface FirebaseApp : JsAny

internal external fun initializeApp(options: JsAny): FirebaseApp
