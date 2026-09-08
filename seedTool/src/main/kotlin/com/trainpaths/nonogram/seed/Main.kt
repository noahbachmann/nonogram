package com.trainpaths.nonogram.seed

import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val SEED_FILE =
    "shared/src/commonMain/kotlin/com/trainpaths/nonogram/cache/SeedPuzzles.kt"

// Mirrors `classes/Nonogram.kt`, which this module cannot see (`:shared` has no JVM target).
private const val MIN_SIDE = 5
private const val MAX_SIDE = 60
private const val MAX_NAME_LENGTH = 30
private val DIFFICULTIES = setOf("EASY", "MEDIUM", "HARD", "HARDCORE")

/** The cap the Firestore rules put on the encoded solution. */
private const val MAX_SOLUTION_CHARS = 20_000

/**
 * Rewrites the committed seed file from every approved puzzle in the dev project. It is a projection, not
 * an append log: each run recreates the file from scratch, so the way to change what ships is to change
 * what is approved in dev.
 *
 *     ./gradlew :seedTool:run                       # dev, writes SeedPuzzles.kt
 *     ./gradlew :seedTool:run --args="--dry-run"    # report only
 */
fun main(args: Array<String>) {
    val options = args.filter { it.startsWith("--") }
        .associate { it.removePrefix("--").substringBefore('=') to it.substringAfter('=', "true") }

    val root = Path(options["root"] ?: ".")
    val env = options["env"] ?: "dev"
    require(env == "dev" || options["allow-prod"] == "true") {
        "refusing to export from '$env': seeds come from dev. Pass --allow-prod if you really mean it."
    }
    val configured = readEnv(root, env)
    val firebase = FirebaseEnv(
        projectId = options["project"] ?: configured.projectId,
        apiKey = options["key"] ?: configured.apiKey,
    )
    val outFile = root.resolve(options["out"] ?: SEED_FILE)

    println("Reading approved puzzles from ${firebase.projectId}")
    val documents = fetchApproved(firebase)

    val skipped = mutableListOf<String>()
    val seeds = documents
        .mapNotNull { document ->
            val problem = document.problem()
            if (problem != null) {
                skipped += "${document.id} (${document.name ?: "unnamed"}): $problem"
                null
            } else {
                Seed(
                    // The document id is the seed id: same puzzle, same id, on every device.
                    id = document.id.toLong(),
                    name = document.name?.normalized(),
                    difficulty = document.difficulty!!,
                    solution = document.solution!!.toGrid()!!,
                )
            }
        }
        .sortedBy { it.id }

    val rendered = renderSeedFile(seeds, firebase.projectId)
    println("  fetched ${documents.size} approved, shipping ${seeds.size}")
    skipped.forEach { println("  skipped $it") }

    if (outFile.exists() && outFile.readText() == rendered) {
        println("  unchanged — nothing written")
        return
    }
    if (options["dry-run"] == "true") {
        println("  --dry-run: nothing written")
        return
    }
    outFile.writeText(rendered)
    println("  wrote $outFile")
}

/** Why this document cannot ship, or null when it can. The Solver check is a test's job, not the tool's. */
private fun RemoteDoc.problem(): String? {
    if (id.toLongOrNull() == null) return "document id is not a number"
    if (difficulty !in DIFFICULTIES) return "unknown difficulty '$difficulty'"
    val encoded = solution ?: return "no solution"
    if (encoded.length > MAX_SOLUTION_CHARS) return "solution is ${encoded.length} chars, over the $MAX_SOLUTION_CHARS cap"
    val grid = encoded.toGrid() ?: return "solution is not a grid"
    val width = grid.firstOrNull()?.size ?: 0
    if (grid.any { it.size != width }) return "ragged grid"
    if (grid.size !in MIN_SIDE..MAX_SIDE || width !in MIN_SIDE..MAX_SIDE) {
        return "grid is ${width}x${grid.size}, outside $MIN_SIDE..$MAX_SIDE"
    }
    if (grid.any { row -> row.any { it != 0 && it != 1 } }) return "cells other than 0/1"
    return null
}

private val json = Json

private fun String.toGrid(): List<List<Int>>? =
    runCatching { json.decodeFromString<List<List<Int>>>(this) }.getOrNull()

/** Mirror of `normalizeNonogramName`: newlines flattened, trimmed, truncated, blank stored as null. */
private fun String.normalized(): String? =
    trim().replace('\n', ' ').replace('\r', ' ').take(MAX_NAME_LENGTH).takeIf { it.isNotEmpty() }
