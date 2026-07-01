package com.trainpaths.nonogram.di

import com.russhwolf.settings.Settings
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.sync.FirestoreSyncService
import org.koin.dsl.module

val appModule = module {
    single { AppSDK(get()) }
    single { Settings() }
    single { AuthRepository(get(), get()) }
    single { FirestoreSyncService(get()) }
}
