package com.trainpaths.nonogram

import com.trainpaths.nonogram.cache.Database
import com.trainpaths.nonogram.cache.DatabaseFactory
import com.trainpaths.nonogram.cache.NonogramProgress
import com.trainpaths.nonogram.cache.ProgressWithTimestamp
import com.trainpaths.nonogram.classes.Nonogram
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppSDK(private val databaseFactory: DatabaseFactory) {
    private val mutex = Mutex()
    private var database: Database? = null

    private suspend fun db(): Database =
        database ?: mutex.withLock {
            database ?: Database(databaseFactory.createDriver()).also { database = it }
        }

    suspend fun seedIfEmpty() {
        if (getAllNonograms().isNotEmpty()) return
        addNonogram(
            "EASY",
            listOf(
                listOf(0, 0, 1, 0, 0), listOf(0, 0, 1, 0, 0), listOf(1, 1, 1, 1, 1),
                listOf(0, 0, 1, 0, 0), listOf(0, 0, 1, 0, 0)
            ),
            status = 1,
        )
        addNonogram(
            "MEDIUM",
            listOf(
                listOf(1, 1, 1, 1, 1), listOf(1, 0, 0, 0, 0), listOf(1, 1, 1, 0, 0),
                listOf(1, 0, 0, 0, 0), listOf(1, 1, 1, 1, 1)
            ),
            status = 1,
        )
        addNonogram(
            "HARD",
            listOf(
                listOf(1, 1, 0, 0, 0), listOf(0, 1, 1, 0, 0), listOf(0, 0, 1, 1, 0),
                listOf(0, 0, 0, 1, 1), listOf(0, 0, 0, 0, 1)
            ),
            status = 1,
        )
        addNonogram(
            "HARD",
            listOf(
                listOf(0, 1, 1, 1, 0, 0, 0, 1, 1, 1),
                listOf(1, 1, 0, 0, 1, 1, 1, 1, 1, 1),
                listOf(1, 1, 1, 1, 1, 0, 0, 0, 1, 1),
                listOf(1, 1, 1, 0, 0, 0, 1, 1, 1, 1),
                listOf(0, 0, 0, 0, 0, 1, 1, 1, 0, 0),
                listOf(0, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                listOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 0),
                listOf(0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
                listOf(0, 0, 1, 1, 1, 1, 0, 1, 0, 1),
                listOf(0, 0, 0, 0, 1, 1, 1, 0, 0, 1),
            ),
            status = 1,
        )
    }

    suspend fun getAllNonograms(): List<Nonogram> =
        db().getAllNonograms()

    suspend fun getNonogramsByDifficulty(difficulty: String): List<Nonogram> =
        db().getNonogramsByDifficulty(difficulty)

    suspend fun getNonogramsByAuthor(authorId: Long): List<Nonogram> =
        db().getNonogramsByAuthor(authorId)

    suspend fun getNonogramById(id: Long): Nonogram? =
        db().getNonogramById(id)

    suspend fun getRandomNonogram(difficulty: String? = null): Nonogram? =
        db().getRandomNonogram(difficulty)

    suspend fun addNonogram(
        difficulty: String,
        solution: List<List<Int>>,
        authorId: Long = 0,
        valid: Long = 0,
        status: Long = 0
    ): Long =
        db().addNonogram(difficulty, solution, authorId, valid, status)

    suspend fun updateNonogram(
        id: Long,
        nonogram: Nonogram,
    ): Long =
        db().updateNonogram(id, nonogram)

    suspend fun addUser(name: String): Long =
        db().addUser(name)

    suspend fun getUserById(id: Long) =
        db().getUserById(id)

    suspend fun getUserByFirebaseUid(uid: String) =
        db().getUserByFirebaseUid(uid)

    suspend fun updateUserFirebaseUid(userId: Long, firebaseUid: String, name: String) =
        db().updateUserFirebaseUid(userId, firebaseUid, name)

    suspend fun saveProgress(userId: Long, nonogramId: Long, board: List<List<Int>>?) =
        db().saveProgress(userId, nonogramId, board)

    suspend fun incrementBeat(userId: Long, nonogramId: Long) =
        db().incrementBeat(userId, nonogramId)

    suspend fun saveProgressAfterWin(userId: Long, nonogramId: Long) =
        db().saveProgressAfterWin(userId, nonogramId)

    suspend fun getProgressForUser(userId: Long): List<NonogramProgress> =
        db().getProgressForUser(userId)

    suspend fun getProgressForUserWithTimestamp(userId: Long): List<ProgressWithTimestamp> =
        db().getProgressForUserWithTimestamp(userId)

    suspend fun getSingleProgress(userId: Long, nonogramId: Long): ProgressWithTimestamp? =
        db().getSingleProgress(userId, nonogramId)

    suspend fun saveProgressWithTimestamp(userId: Long, nonogramId: Long, boardState: String?, updatedAt: Long) =
        db().saveProgressWithTimestamp(userId, nonogramId, boardState, updatedAt)
}
