package com.trainpaths.nonogram.screens

import androidx.compose.runtime.Composable

/**
 * Remembers a launcher that opens the platform's image picker and hands back the encoded bytes.
 *
 * Only *acquiring* the bytes is platform-specific — decoding them is common code, since
 * `decodeToImageBitmap` and `readPixels` are both in Compose's multiplatform API. The button itself
 * stays in `GenScanScreen` so its styling lives in one place rather than once per platform.
 *
 * A dismissed picker is silent: it reports neither a pick nor an error, the same way
 * [GoogleSignInSection] swallows a cancelled sign-in.
 */
@Composable
expect fun rememberImagePicker(
    onPicked: (ByteArray) -> Unit,
    onError: (String) -> Unit,
): () -> Unit
