package com.trainpaths.nonogram.di

import com.trainpaths.nonogram.AppSDK
import org.koin.dsl.module

val appModule = module {
    single { AppSDK(get()) }
}
