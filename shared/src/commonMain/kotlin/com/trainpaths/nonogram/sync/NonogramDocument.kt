package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.isWellFormedGrid
import com.trainpaths.nonogram.util.toDifficulty
import com.trainpaths.nonogram.util.toPublishStatus
import kotlinx.serialization.json.Json

private val json = Json

internal fun encodeSolution(solution: List<List<Int>>): String = json.encodeToString(solution)

/**
 * A `nonograms/{id}` document exactly as it comes off the wire. Each platform's adapter does
 * nothing but pull these fields out of its own snapshot type.
 */
internal data class NonogramDocument(
    val id: String,
    val difficulty: String?,
    val solution: String?,
    val name: String?,
    val authorUid: String?,
    val updatedAt: Long?,
    val publishStatus: String?,
)

/**
 * The document as a domain [Nonogram], or null when it is unusable.
 * Fields that are merely *absent* are tolerated.
 */
internal fun NonogramDocument.toNonogram(onSkip: (String) -> Unit): Nonogram? {
    val nonogramId = id.toLongOrNull() ?: return skip(onSkip, "id is not a number")
    val encoded = solution ?: return skip(onSkip, "no ${Fields.SOLUTION}")
    val timestamp = updatedAt ?: return skip(onSkip, "no ${Fields.UPDATED_AT}")
    val grid = try {
        json.decodeFromString<List<List<Int>>>(encoded)
    } catch (e: Exception) {
        return skip(onSkip, "unreadable solution: ${e.message}")
    }
    if (!grid.isWellFormedGrid()) return skip(onSkip, "grid out of range or ragged")
    return Nonogram(
        id = nonogramId,
        difficulty = difficulty.toDifficulty(),
        solution = grid,
        name = name,
        authorUid = authorUid ?: "",
        updatedAt = timestamp,
        publishStatus = publishStatus.toPublishStatus(),
    )
}

private fun skip(onSkip: (String) -> Unit, reason: String): Nonogram? {
    onSkip(reason)
    return null
}
