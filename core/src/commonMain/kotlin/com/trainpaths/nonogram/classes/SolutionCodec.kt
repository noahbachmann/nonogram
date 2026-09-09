package com.trainpaths.nonogram.classes

import kotlinx.serialization.json.Json

private val json = Json

/**
 * Encoding for solutions and saved boards. `hasPublishConflict` matches rows on this exact string.
 *
 * Public rather than internal only because `:shared` and `:solverWorker` are separate modules; it is
 * still the one encoding the whole app uses, and nothing outside them should be adding another.
 */
fun List<List<Int>>.toSolutionJson(): String = json.encodeToString(this)

/**
 * Decode: null when the string is not a grid at all, with the reason handed to [onError].
 * Shape is the caller's business — a stored grid and a grid off the wire are held to different rules.
 */
fun String.toSolutionOrNull(onError: (String) -> Unit = {}): List<List<Int>>? =
    try {
        json.decodeFromString<List<List<Int>>>(this)
    } catch (e: Exception) {
        onError(e.message.orEmpty())
        null
    }
