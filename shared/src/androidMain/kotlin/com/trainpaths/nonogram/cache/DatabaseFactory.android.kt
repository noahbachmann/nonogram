package com.trainpaths.nonogram.cache

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/** The Android SQLite driver is synchronous and blocks, so it belongs on the IO pool. */
internal actual val dbDispatcher: CoroutineContext = Dispatchers.IO

class AndroidDatabaseFactory(private val context: Context) : DatabaseFactory {
    override suspend fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(NonogramDb.Schema.synchronous(), context, "launch.db")
    }
}