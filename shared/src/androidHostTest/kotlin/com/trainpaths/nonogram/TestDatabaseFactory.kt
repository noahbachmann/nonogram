package com.trainpaths.nonogram

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.trainpaths.nonogram.cache.DatabaseFactory
import com.trainpaths.nonogram.cache.NonogramDb

class TestDatabaseFactory : DatabaseFactory {
    override suspend fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NonogramDb.Schema.synchronous().create(driver)
        return driver
    }
}
