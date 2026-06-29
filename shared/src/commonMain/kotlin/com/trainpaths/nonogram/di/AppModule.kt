package com.trainpaths.nonogram.di

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.GameViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { AppSDK(get()) }
    viewModelOf(::GameViewModel)
}
