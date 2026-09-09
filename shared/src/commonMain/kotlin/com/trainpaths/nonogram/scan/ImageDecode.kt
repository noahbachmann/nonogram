package com.trainpaths.nonogram.scan

/**
 * A picked image as its platform holds it: the encoded bytes on Android, the browser's own `Blob`
 * on web.
 *
 * Deliberately not a `ByteArray`. On web the bytes would have to be copied out of JS one element at
 * a time — a 12 MP photo is twelve million boundary crossings on the thread that also draws the UI —
 * and then handed straight back to a decoder that would rather have had the `Blob`.
 */
expect class PickedImage {
    val sizeBytes: Long
}

/**
 * Reduces the image to a [LumaMap] of at most [maxSide] on its longest side.
 *
 * Suspending because on web both the decode and the downscale belong to the browser, which does
 * them asynchronously and off the main thread; see `docs/web-architecture.md`.
 */
internal expect suspend fun PickedImage.decodeToLumaMap(maxSide: Int = WORKING_SIDE): LumaMap
