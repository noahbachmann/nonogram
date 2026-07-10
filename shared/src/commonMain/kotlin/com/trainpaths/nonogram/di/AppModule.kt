package com.trainpaths.nonogram.di

import com.russhwolf.settings.Settings
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.screens.viewModel.GenViewModel
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { AppSDK(get()) }
    single { Settings() }
    single { AuthRepository(get(), get()) }
    viewModelOf(::AuthViewModel)
    viewModelOf(::MenuViewModel)
    viewModelOf(::GameViewModel)
    viewModelOf(::GenViewModel)
}
