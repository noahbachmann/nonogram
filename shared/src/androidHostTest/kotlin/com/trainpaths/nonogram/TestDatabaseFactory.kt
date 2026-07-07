package com.trainpaths.nonogram

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.trainpaths.nonogram.cache.DatabaseFactory
import com.trainpaths.nonogram.cache.NonogramDb

class TestDatabaseFactory : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NonogramDb.Schema.create(driver)
        return driver
    }
}
