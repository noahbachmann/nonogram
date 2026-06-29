package com.trainpaths.nonogram.cache

import app.cash.sqldelight.db.SqlDriver

interface DatabaseFactory {
    fun createDriver(): SqlDriver
}