package com.trainpaths.nonogram.cache

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.util.toBoolean
import com.trainpaths.nonogram.util.toLong
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Clock

internal class Database(driver: SqlDriver) {
    private val database = NonogramDb(driver)
    private val dbQuery = database.databaseQueries
    private val json = Json

    // ---------- Nonograms ----------

    internal suspend fun getAllNonograms(): List<Nonogram> =
        dbQuery.selectAllNonograms(::mapNonogram).awaitAsList()

    internal suspend fun getNonogramsByDifficulty(difficulty: String): List<Nonogram> =
        dbQuery.selectNonogramsByDifficulty(difficulty, ::mapNonogram).awaitAsList()

    internal suspend fun getNonogramsByAuthor(authorId: Long): List<Nonogram> =
        dbQuery.selectNonogramsByAuthor(authorId, ::mapNonogram).awaitAsList()

    internal suspend fun getNonogramById(id: Long): Nonogram? =
        dbQuery.selectNonogramById(id, ::mapNonogram).awaitAsOneOrNull()

    internal suspend fun getRandomNonogram(difficulty: String? = null): Nonogram? =
        if (difficulty == null) {
            dbQuery.selectRandomNonogram(::mapNonogram).awaitAsOneOrNull()
        } else {
            dbQuery.selectRandomNonogramByDifficulty(difficulty, ::mapNonogram)
                .awaitAsOneOrNull()
        }

    internal suspend fun addNonogram(
        difficulty: String,
        solution: List<List<Int>>,
        authorId: Long = 0,
        isValid: Boolean = false,
        isPublic: Boolean = false,
        id: Long? = null
    ): Long {
        // Random ids in [2^20, 2^53) stay unique across devices (Firestore doc ids) and JS-double-safe.
        val nonogramId = id ?: Random.nextLong(1L shl 20, 1L shl 53)
        dbQuery.upsertNonogram(
            nonogramId,
            difficulty,
            json.encodeToString(solution),
            authorId,
            isValid.toLong(),
            isPublic.toLong(),
            Clock.System.now().toEpochMilliseconds()
        )
        return nonogramId
    }

    internal suspend fun updateNonogram(
        id: Long,
        nonogram: Nonogram,
    ): Long {
        dbQuery.updateNonogram(
            nonogram.difficulty.toString(),
            json.encodeToString(nonogram.solution),
            nonogram.authorId,
            nonogram.isValid.toLong(),
            nonogram.isPublic.toLong(),
            Clock.System.now().toEpochMilliseconds(),
            id
        )
        return id
    }

    internal suspend fun upsertNonogram(nonogram: Nonogram) {
        dbQuery.upsertNonogram(
            nonogram.id,
            nonogram.difficulty.toString(),
            json.encodeToString(nonogram.solution),
            nonogram.authorId,
            nonogram.isValid.toLong(),
            nonogram.isPublic.toLong(),
            nonogram.updatedAt
        )
    }
    // ---------- Users ----------

    internal suspend fun addUser(name: String): Long =
        dbQuery.transactionWithResult {
            dbQuery.insertUser(name)
            dbQuery.lastInsertedId().awaitAsOne()
        }

    internal suspend fun getUserById(id: Long) =
        dbQuery.selectUserById(id).awaitAsOneOrNull()

    internal suspend fun getUserByFirebaseUid(uid: String) =
        dbQuery.selectUserByFirebaseUid(uid).awaitAsOneOrNull()

    internal suspend fun updateUserFirebaseUid(userId: Long, firebaseUid: String, name: String) {
        dbQuery.updateUserFirebaseUid(firebaseUid, name, userId)
    }

    internal suspend fun saveProgress(
        userId: Long,
        nonogramId: Long,
        board: List<List<Int>>?
    ) {
        dbQuery.upsertProgress(
            userId = userId,
            nonogramId = nonogramId,
            boardState = board?.let { json.encodeToString(it) },
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    internal suspend fun incrementBeat(userId: Long, nonogramId: Long) {
        dbQuery.incrementBeat(userId, nonogramId)
    }

    internal suspend fun saveProgressAfterWin(userId: Long, nonogramId: Long) {
        dbQuery.upsertProgressAfterWin(
            userId = userId,
            nonogramId = nonogramId,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    internal suspend fun getProgressForUser(userId: Long): List<NonogramProgress> =
        dbQuery.selectProgressForUser(userId, ::mapProgress).awaitAsList()

    internal suspend fun getProgressForUserWithTimestamp(userId: Long): List<ProgressWithTimestamp> =
        dbQuery.selectProgressForUserWithTimestamp(userId) { nonogramId, boardState, updatedAt ->
            ProgressWithTimestamp(nonogramId, boardState, updatedAt)
        }.awaitAsList()

    internal suspend fun getSingleProgress(userId: Long, nonogramId: Long): ProgressWithTimestamp? =
        dbQuery.selectSingleProgress(userId, nonogramId) { boardState, updatedAt ->
            ProgressWithTimestamp(nonogramId, boardState, updatedAt)
        }.awaitAsOneOrNull()

    internal suspend fun saveProgressWithTimestamp(
        userId: Long,
        nonogramId: Long,
        boardState: String?,
        updatedAt: Long
    ) {
        dbQuery.upsertProgress(
            userId = userId,
            nonogramId = nonogramId,
            boardState = boardState,
            updatedAt = updatedAt
        )
    }

    private fun mapNonogram(
        id: Long,
        difficulty: String,
        solution: String,
        authorId: Long,
        valid: Long,
        status: Long,
        updatedAt: Long
    ): Nonogram = Nonogram(
        id = id,
        difficulty = Difficulty.valueOf(difficulty),
        solution = json.decodeFromString(solution),
        authorId = authorId,
        isValid = valid.toBoolean(),
        isPublic = status.toBoolean(),
        updatedAt = updatedAt
    )

    private fun mapProgress(
        id: Long,
        difficulty: String,
        solution: String,
        authorId: Long,
        valid: Long,
        status: Long,
        updatedAt: Long,
        boardState: String?,
        beat: Long
    ): NonogramProgress = NonogramProgress(
        nonogram = Nonogram(
            id = id,
            difficulty = Difficulty.valueOf(difficulty),
            solution = json.decodeFromString(solution),
            authorId = authorId,
            isValid = valid.toBoolean(),
            isPublic = status.toBoolean(),
            updatedAt = updatedAt
        ),
        board = boardState?.let { json.decodeFromString(it) },
        beat = beat
    )
}

data class NonogramProgress(
    val nonogram: Nonogram,
    val board: List<List<Int>>?,
    val beat: Long = 0
)

data class ProgressWithTimestamp(
    val nonogramId: Long,
    val boardState: String?,
    val updatedAt: Long
)
