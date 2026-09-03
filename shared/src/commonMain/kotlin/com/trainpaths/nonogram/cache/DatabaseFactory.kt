package com.trainpaths.nonogram.cache

import app.cash.sqldelight.db.SqlDriver
import kotlin.coroutines.CoroutineContext

interface DatabaseFactory {
    suspend fun createDriver(): SqlDriver
}

internal expect val dbDispatcher: CoroutineContext
