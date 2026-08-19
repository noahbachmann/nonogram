package com.trainpaths.nonogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.screens.LoadingScreen
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.screens.viewModel.GenViewModel
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import com.trainpaths.nonogram.screens.viewModel.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppInitializer.onApplicationStart(getString(R.string.default_web_client_id))

        setContent {
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
}
