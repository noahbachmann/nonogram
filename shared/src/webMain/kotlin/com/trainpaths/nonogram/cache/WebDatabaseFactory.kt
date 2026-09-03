package com.trainpaths.nonogram.cache

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.async.coroutines.awaitMigrate
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver

internal expect fun createDbWorkerDriver(): SqlDriver

class WebDatabaseFactory : DatabaseFactory {
    override suspend fun createDriver(): SqlDriver {
        val driver = createDbWorkerDriver()
        val current = currentVersion(driver)
        val target = NonogramDb.Schema.version
        when {
            current == 0L -> NonogramDb.Schema.awaitCreate(driver)
            current < target -> NonogramDb.Schema.awaitMigrate(driver, current, target)
            current == target -> Unit
            else -> error("OPFS database is at schema version $current, newer than this build's $target")
        }
        if (current != target) {
            driver.execute(null, "PRAGMA user_version = $target;", 0).await()
        }
        return driver
    }

    private suspend fun currentVersion(driver: SqlDriver): Long {
        var result = 0L
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor ->
                QueryResult.AsyncValue {
                    if (cursor.next().await()) {
                        result = cursor.getLong(0) ?: 0L
                    }
                }
            },
            parameters = 0,
        ).await()
        return result
    }
}
