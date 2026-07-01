package com.trainpaths.nonogram

import android.app.Application
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.di.androidModule
import com.trainpaths.nonogram.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NonogramApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NonogramApplication)
            modules(androidModule, appModule)
        }
        val authRepository: AuthRepository = org.koin.java.KoinJavaComponent.getKoin().get()
        AppInitializer.initializeAuth(authRepository)
    }
}
