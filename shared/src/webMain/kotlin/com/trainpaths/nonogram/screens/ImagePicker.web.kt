@file:OptIn(ExperimentalWasmJsInterop::class)

package com.trainpaths.nonogram.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop

@Composable
actual fun rememberImagePicker(
    onPicked: (ByteArray) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onError)

    return remember(scope) {
        {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/*"
            input.addEventListener("change") { _: Event ->
                val file = input.files?.item(0)
                if (file != null) {
                    scope.launch {
                        try {
                            picked(file.readBytes())
                        } catch (error: Throwable) {
                            failed(error.message ?: "Could not open that image.")
                        }
                    }
                }
            }
            input.click()
        }
    }
}

/**
 * Reads the picked file.
 *
 * This lives in `webMain` rather than being split into `jsMain`/`wasmJsMain` actuals the way
 * `createDbWorkerDriver` is: the DOM API here comes from `kotlinx-browser` for *both* targets — the
 * stdlib no longer ships `org.w3c` — so `FileReader.result` is one unified `JsAny?` and the code
 * compiles unchanged for js and wasmJs.
 */
private suspend fun File.readBytes(): ByteArray =
    suspendCancellableCoroutine { continuation ->
        val reader = FileReader()
        reader.onload = {
            val bytes = Uint8Array(reader.result as ArrayBuffer)
            continuation.resume(ByteArray(bytes.length) { bytes[it] })
        }
        reader.onerror = {
            continuation.resumeWithException(IllegalStateException("Could not read that image."))
        }
        reader.readAsArrayBuffer(this)
    }
