package com.trainpaths.nonogram.cache

internal class Database(databaseFactory: DatabaseFactory) {
    private val database = NonogramDb(databaseFactory.createDriver())
    private val dbQuery = database.databaseQueries
}