package com.trainpaths.nonogram

import com.trainpaths.nonogram.cache.Database
import com.trainpaths.nonogram.cache.DatabaseFactory
import com.trainpaths.nonogram.cache.NonogramProgress
import com.trainpaths.nonogram.classes.Nonogram

class AppSDK(databaseFactory: DatabaseFactory) {
    private val database = Database(databaseFactory)

    fun seedIfEmpty() {
        if (getAllNonograms().isNotEmpty()) return
        addNonogram("EASY", listOf(
            listOf(0,0,1,0,0), listOf(0,0,1,0,0), listOf(1,1,1,1,1),
            listOf(0,0,1,0,0), listOf(0,0,1,0,0)
        ))
        addNonogram("MEDIUM", listOf(
            listOf(1,1,1,1,1), listOf(1,0,0,0,0), listOf(1,1,1,0,0),
            listOf(1,0,0,0,0), listOf(1,1,1,1,1)
        ))
        addNonogram("HARD", listOf(
            listOf(1,1,0,0,0), listOf(0,1,1,0,0), listOf(0,0,1,1,0),
            listOf(0,0,0,1,1), listOf(0,0,0,0,1)
        ))
    }

    fun getAllNonograms(): List<Nonogram> =
        database.getAllNonograms()

    fun getNonogramsByDifficulty(difficulty: String): List<Nonogram> =
        database.getNonogramsByDifficulty(difficulty)

    fun getNonogramById(id: Long): Nonogram? =
        database.getNonogramById(id)

    fun getRandomNonogram(difficulty: String? = null): Nonogram? =
        database.getRandomNonogram(difficulty)

    fun addNonogram(difficulty: String, solution: List<List<Int>>): Long =
        database.addNonogram(difficulty, solution)

    fun addUser(name: String): Long =
        database.addUser(name)

    fun getUserById(id: Long) =
        database.getUserById(id)

    fun saveProgress(userId: Long, nonogramId: Long, board: List<List<Int>>?) =
        database.saveProgress(userId, nonogramId, board)

    fun getProgressForUser(userId: Long): List<NonogramProgress> =
        database.getProgressForUser(userId)
}