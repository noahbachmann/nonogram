@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.firebase

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

internal external interface AppCheckOptions : JsAny {
    var provider: ReCaptchaV3Provider
}

internal fun appCheckOptions(provider: ReCaptchaV3Provider): AppCheckOptions {
    val options = JSON.parse(
        buildJsonObject {
            put("isTokenAutoRefreshEnabled", true)
        }.toString()
    )!! as AppCheckOptions
    options.provider = provider
    return options
}
