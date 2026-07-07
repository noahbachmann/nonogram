package com.trainpaths.nonogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppInitializer.onApplicationStart(BuildConfig.GOOGLE_WEB_CLIENT_ID)

        setContent {
            val menuViewModel = koinViewModel<MenuViewModel>()
            val authViewModel = koinViewModel<AuthViewModel>()
            App(
                menuViewModel = menuViewModel,
                authViewModel = authViewModel,
                gameViewModelFactory = { _ -> koinViewModel<GameViewModel>() },
            )
        }
    }
}
