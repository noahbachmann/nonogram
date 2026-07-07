package com.trainpaths.nonogram

import android.app.Application
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.di.androidModule
import com.trainpaths.nonogram.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val koinApp = startKoin {
            androidContext(this@MainApplication)
            modules(androidModule, appModule)
        }

        val authRepository = koinApp.koin.get<AuthRepository>()
        AppInitializer.initializeAuth(authRepository)
    }
}