package com.trainpaths.nonogram.cache

import app.cash.sqldelight.driver.worker.WebWorkerDriver
import app.cash.sqldelight.db.SqlDriver
import org.w3c.dom.Worker

internal actual fun createDbWorkerDriver(): SqlDriver =
    WebWorkerDriver(Worker("sqlite.worker.js"))
