package com.trainpaths.nonogram.cache

import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import kotlinx.serialization.json.Json
import kotlin.time.Clock

internal class Database(databaseFactory: DatabaseFactory) {
    private val database = NonogramDb(databaseFactory.createDriver())
    private val dbQuery = database.databaseQueries
    private val json = Json

    // ---------- Nonograms ----------

    internal fun getAllNonograms(): List<Nonogram> =
        dbQuery.selectAllNonograms(::mapNonogram).executeAsList()

    internal fun getNonogramsByDifficulty(difficulty: String): List<Nonogram> =
        dbQuery.selectNonogramsByDifficulty(difficulty, ::mapNonogram).executeAsList()

    internal fun getNonogramById(id: Long): Nonogram? =
        dbQuery.selectNonogramById(id, ::mapNonogram).executeAsOneOrNull()

    internal fun getRandomNonogram(difficulty: String? = null): Nonogram? =
        if (difficulty == null) {
            dbQuery.selectRandomNonogram(::mapNonogram).executeAsOneOrNull()
        } else {
            dbQuery.selectRandomNonogramByDifficulty(difficulty, ::mapNonogram)
                .executeAsOneOrNull()
        }

    internal fun addNonogram(difficulty: String, solution: List<List<Int>>): Long =
        dbQuery.transactionWithResult {
            dbQuery.insertNonogram(difficulty, json.encodeToString(solution))
            dbQuery.lastInsertedId().executeAsOne()
        }

    // ---------- Users ----------

    internal fun addUser(name: String): Long =
        dbQuery.transactionWithResult {
            dbQuery.insertUser(name)
            dbQuery.lastInsertedId().executeAsOne()
        }

    internal fun getUserById(id: Long) =
        dbQuery.selectUserById(id).executeAsOneOrNull()

    internal fun getUserByFirebaseUid(uid: String) =
        dbQuery.selectUserByFirebaseUid(uid).executeAsOneOrNull()

    internal fun updateUserFirebaseUid(userId: Long, firebaseUid: String, name: String) {
        dbQuery.updateUserFirebaseUid(firebaseUid, name, userId)
    }

    internal fun saveProgress(
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

    internal fun incrementBeat(userId: Long, nonogramId: Long) {
        dbQuery.incrementBeat(userId, nonogramId)
    }

    internal fun getProgressForUser(userId: Long): List<NonogramProgress> =
        dbQuery.selectProgressForUser(userId, ::mapProgress).executeAsList()

    internal fun getProgressForUserWithTimestamp(userId: Long): List<ProgressWithTimestamp> =
        dbQuery.selectProgressForUserWithTimestamp(userId) { nonogramId, boardState, updatedAt ->
            ProgressWithTimestamp(nonogramId, boardState, updatedAt)
        }.executeAsList()

    internal fun getSingleProgress(userId: Long, nonogramId: Long): ProgressWithTimestamp? =
        dbQuery.selectSingleProgress(userId, nonogramId) { boardState, updatedAt ->
            ProgressWithTimestamp(nonogramId, boardState, updatedAt)
        }.executeAsOneOrNull()

    internal fun saveProgressWithTimestamp(
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
        solution: String
    ): Nonogram = Nonogram(
        id = id,
        difficulty = Difficulty.valueOf(difficulty),
        solution = json.decodeFromString(solution)
    )

    private fun mapProgress(
        id: Long,
        difficulty: String,
        solution: String,
        boardState: String?,
        beat: Long
    ): NonogramProgress = NonogramProgress(
        nonogram = Nonogram(
            id = id,
            difficulty = Difficulty.valueOf(difficulty),
            solution = json.decodeFromString(solution)
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