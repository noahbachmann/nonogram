@file:JsModule("firebase/app-check")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsModule

internal external interface AppCheck : JsAny

// external classes are implicitly JsAny in wasmJs — no explicit supertype
internal external class ReCaptchaV3Provider(siteKey: String)

internal external fun initializeAppCheck(app: FirebaseApp, options: JsAny): AppCheck
