package com.trainpaths.nonogram.di

import com.trainpaths.nonogram.cache.DatabaseFactory
import com.trainpaths.nonogram.cache.WebDatabaseFactory
import com.trainpaths.nonogram.sync.NoOpSyncService
import com.trainpaths.nonogram.sync.SyncService
import org.koin.dsl.module

val webModule = module {
    single<DatabaseFactory> { WebDatabaseFactory() }
    single<SyncService> { NoOpSyncService() }
}
