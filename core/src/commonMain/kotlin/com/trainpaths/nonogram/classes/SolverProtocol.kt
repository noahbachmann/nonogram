package com.trainpaths.nonogram.classes

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the page sends the web Solver worker, and what comes back — JSON strings over `postMessage`,
 * see `docs/web-architecture.md`. It lives here, next to the Solver itself, because both ends of the
 * exchange are Kotlin and this is the one module they share.
 *
 * [SolveRequest.id] correlates a reply with its request; the worker answers in whatever order it
 * finishes.
 */
@Serializable
data class SolveRequest(val id: Int, val grid: List<List<Int>>)

@Serializable
data class SolveResponse(
    val id: Int,
    val deduced: List<List<Int>>? = null,
    val error: String? = null,
)

private val protocolJson = Json { ignoreUnknownKeys = true }

fun SolveRequest.encode(): String = protocolJson.encodeToString(this)

fun SolveResponse.encode(): String = protocolJson.encodeToString(this)

fun decodeSolveRequest(text: String): SolveRequest = protocolJson.decodeFromString(text)

fun decodeSolveResponse(text: String): SolveResponse = protocolJson.decodeFromString(text)
