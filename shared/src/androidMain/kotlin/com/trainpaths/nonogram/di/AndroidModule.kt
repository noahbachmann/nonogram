package com.trainpaths.nonogram.di

import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import com.trainpaths.nonogram.cache.AndroidDatabaseFactory
import com.trainpaths.nonogram.cache.DatabaseFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val androidModule = module {
    single<DatabaseFactory> { AndroidDatabaseFactory(androidContext()) }
    viewModelOf(::AuthViewModel)
    viewModelOf(::MenuViewModel)
    viewModelOf(::GameViewModel)
}
