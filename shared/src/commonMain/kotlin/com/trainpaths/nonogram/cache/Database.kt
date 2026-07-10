package com.trainpaths.nonogram.cache

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import kotlinx.serialization.json.Json
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
        valid: Long = 0,
        status: Long = 0
    ): Long =
        dbQuery.transactionWithResult {
            dbQuery.insertNonogram(difficulty, json.encodeToString(solution), authorId, valid, status)
            dbQuery.lastInsertedId().awaitAsOne()
        }

    internal suspend fun updateNonogram(
        id: Long,
        nonogram: Nonogram,
    ): Long =
        dbQuery.transactionWithResult {
            dbQuery.updateNonogram(
                nonogram.difficulty.toString(),
                json.encodeToString(nonogram.solution),
                nonogram.authorId,
                nonogram.valid,
                nonogram.status,
                id
            )
            dbQuery.lastInsertedId().awaitAsOne()
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
        status: Long
    ): Nonogram = Nonogram(
        id = id,
        difficulty = Difficulty.valueOf(difficulty),
        solution = json.decodeFromString(solution),
        authorId = authorId,
        valid = valid,
        status = status
    )

    private fun mapProgress(
        id: Long,
        difficulty: String,
        solution: String,
        authorId: Long,
        valid: Long,
        status: Long,
        boardState: String?,
        beat: Long
    ): NonogramProgress = NonogramProgress(
        nonogram = Nonogram(
            id = id,
            difficulty = Difficulty.valueOf(difficulty),
            solution = json.decodeFromString(solution),
            authorId = authorId,
            valid = valid,
            status = status
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
