package com.trainpaths.nonogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trainpaths.nonogram.screens.viewModel.AdminViewModel
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.screens.viewModel.GenViewModel
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import com.trainpaths.nonogram.screens.viewModel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppInitializer.onApplicationStart(getString(R.string.default_web_client_id))

        setContent {
            App(
                menuViewModelFactory = { koinViewModel<MenuViewModel>() },
                authViewModel = koinViewModel<AuthViewModel>(),
                genViewModelFactory = { koinViewModel<GenViewModel>() },
                settingsViewModel = koinViewModel<SettingsViewModel>(),
                gameViewModelFactory = { _ -> koinViewModel<GameViewModel>() },
                adminViewModelFactory = { koinViewModel<AdminViewModel>() },
            )
        }
    }
}
