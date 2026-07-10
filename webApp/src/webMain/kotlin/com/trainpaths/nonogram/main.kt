package com.trainpaths.nonogram

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.di.appModule
import com.trainpaths.nonogram.di.webModule
import com.trainpaths.nonogram.screens.LoadingScreen
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.screens.viewModel.GenViewModel
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin { modules(webModule, appModule) }
    MainScope().launch {
        AppInitializer.initializeAuth(KoinPlatform.getKoin().get<AuthRepository>())
    }

    ComposeViewport {
        val authViewModel = koinViewModel<AuthViewModel>()
        val authState by authViewModel.authState.collectAsState()
        if (authState == AuthState.INITIALIZING) {
            AppTheme { LoadingScreen() }
        } else {
            App(
                menuViewModel = koinViewModel<MenuViewModel>(),
                authViewModel = authViewModel,
                genViewModel = koinViewModel<GenViewModel>(),
                gameViewModelFactory = { _ -> koinViewModel<GameViewModel>() },
            )
        }
    }
}
