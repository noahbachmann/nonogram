package com.trainpaths.nonogram

import com.trainpaths.nonogram.cache.Database
import com.trainpaths.nonogram.cache.DatabaseFactory
import com.trainpaths.nonogram.cache.NonogramProgress
import com.trainpaths.nonogram.cache.ProgressWithTimestamp
import com.trainpaths.nonogram.cache.User
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.PublishStatus
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
        // Fixed ids: seeds must be identical on every device so progress sync lines up.
        addNonogram(
            "EASY",
            listOf(
                listOf(0, 0, 1, 0, 0), listOf(0, 0, 1, 0, 0), listOf(1, 1, 1, 1, 1),
                listOf(0, 0, 1, 0, 0), listOf(0, 0, 1, 0, 0)
            ),
            publishStatus = PublishStatus.APPROVED,
            id = 1,
        )
        addNonogram(
            "MEDIUM",
            listOf(
                listOf(1, 1, 1, 1, 1), listOf(1, 0, 0, 0, 0), listOf(1, 1, 1, 0, 0),
                listOf(1, 0, 0, 0, 0), listOf(1, 1, 1, 1, 1)
            ),
            publishStatus = PublishStatus.APPROVED,
            id = 2,
        )
        addNonogram(
            "HARD",
            listOf(
                listOf(1, 1, 0, 0, 0), listOf(0, 1, 1, 0, 0), listOf(0, 0, 1, 1, 0),
                listOf(0, 0, 0, 1, 1), listOf(0, 0, 0, 0, 1)
            ),
            publishStatus = PublishStatus.APPROVED,
            id = 3,
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
            publishStatus = PublishStatus.APPROVED,
            id = 4,
        )
    }

    suspend fun getAllNonograms(): List<Nonogram> =
        db().getAllNonograms()

    suspend fun getNonogramsByDifficulty(difficulty: String): List<Nonogram> =
        db().getNonogramsByDifficulty(difficulty)

    suspend fun getNonogramsByAuthor(authorUid: String): List<Nonogram> =
        db().getNonogramsByAuthor(authorUid)

    suspend fun getNonogramById(id: Long): Nonogram? =
        db().getNonogramById(id)

    suspend fun getRandomNonogram(difficulty: String? = null): Nonogram? =
        db().getRandomNonogram(difficulty)

    suspend fun addNonogram(
        difficulty: String,
        solution: List<List<Int>>,
        authorUid: String = "",
        id: Long? = null,
        name: String? = null,
        publishStatus: PublishStatus = PublishStatus.NONE,
    ): Long =
        db().addNonogram(difficulty, solution, authorUid, id, name, publishStatus)

    suspend fun updateNonogram(
        id: Long,
        nonogram: Nonogram,
    ): Long =
        db().updateNonogram(id, nonogram)

    suspend fun upsertNonogramFromRemote(nonogram: Nonogram) =
        db().upsertNonogram(nonogram)

    /** Moves every puzzle authored under one author key to another, as sign-in does. */
    suspend fun reassignAuthor(fromUid: String, toUid: String) =
        db().reassignAuthor(fromUid, toUid)

    suspend fun upsertUser(uid: String, name: String) =
        db().upsertUser(uid, name)

    suspend fun getUser(uid: String): User? =
        db().getUser(uid)

    suspend fun deleteUser(uid: String) =
        db().deleteUser(uid)

    /** Moves every progress row from one user key to another, newest row winning. */
    suspend fun mergeProgressInto(fromUid: String, toUid: String) =
        db().mergeProgressInto(fromUid, toUid)

    suspend fun saveProgress(userUid: String, nonogramId: Long, board: List<List<Int>>?) =
        db().saveProgress(userUid, nonogramId, board)

    suspend fun saveProgressAfterWin(userUid: String, nonogramId: Long) =
        db().saveProgressAfterWin(userUid, nonogramId)

    suspend fun getProgressForUser(userUid: String): List<NonogramProgress> =
        db().getProgressForUser(userUid)

    suspend fun getProgressForUserWithTimestamp(userUid: String): List<ProgressWithTimestamp> =
        db().getProgressForUserWithTimestamp(userUid)

    suspend fun getSingleProgress(userUid: String, nonogramId: Long): ProgressWithTimestamp? =
        db().getSingleProgress(userUid, nonogramId)

    suspend fun saveProgressWithTimestamp(userUid: String, nonogramId: Long, boardState: String?, updatedAt: Long) =
        db().saveProgressWithTimestamp(userUid, nonogramId, boardState, updatedAt)
}
