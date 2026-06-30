package com.trainpaths.nonogram.di

import com.trainpaths.nonogram.GameViewModel
import com.trainpaths.nonogram.MenuViewModel
import com.trainpaths.nonogram.cache.AndroidDatabaseFactory
import com.trainpaths.nonogram.cache.DatabaseFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val androidModule = module {
    single<DatabaseFactory> { AndroidDatabaseFactory(androidContext()) }
    viewModelOf(::MenuViewModel)
    viewModelOf(::GameViewModel)
}
