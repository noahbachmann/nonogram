package com.trainpaths.nonogram.cache

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(NonogramDb.Schema, context, "launch.db")
    }
}