package com.trainpaths.nonogram

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.di.appModule
import com.trainpaths.nonogram.di.webModule
import com.trainpaths.nonogram.firebase.FirebaseWeb
import com.trainpaths.nonogram.screens.LoadingScreen
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.screens.viewModel.GenViewModel
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import com.trainpaths.nonogram.screens.viewModel.ThemeViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    FirebaseWeb.initialize(
        apiKey = FirebaseWebConfig.API_KEY,
        authDomain = FirebaseWebConfig.AUTH_DOMAIN,
        projectId = FirebaseWebConfig.PROJECT_ID,
        messagingSenderId = FirebaseWebConfig.MESSAGING_SENDER_ID,
        appId = FirebaseWebConfig.APP_ID,
    )
    AppInitializer.onApplicationStart(FirebaseWebConfig.GOOGLE_WEB_CLIENT_ID)
    startKoin { modules(webModule, appModule) }
    MainScope().launch {
        AppInitializer.initializeAuth(KoinPlatform.getKoin().get<AuthRepository>())
    }

    ComposeViewport {
        val authViewModel = koinViewModel<AuthViewModel>()
        val authState by authViewModel.authState.collectAsState()
        val themeViewModel = koinViewModel<ThemeViewModel>()
        val theme by themeViewModel.theme.collectAsState()
        if (authState == AuthState.INITIALIZING) {
            AppTheme(theme) { LoadingScreen() }
        } else {
            App(
                menuViewModel = koinViewModel<MenuViewModel>(),
                authViewModel = authViewModel,
                genViewModel = koinViewModel<GenViewModel>(),
                themeViewModel = themeViewModel,
                gameViewModelFactory = { _ -> koinViewModel<GameViewModel>() },
            )
        }
    }
}
