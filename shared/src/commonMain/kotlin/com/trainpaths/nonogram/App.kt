package com.trainpaths.nonogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.trainpaths.nonogram.dialogs.LeaveConfirmDialog
import com.trainpaths.nonogram.dialogs.PlayConfirmDialog
import com.trainpaths.nonogram.navigation.GameRoute
import com.trainpaths.nonogram.navigation.LeaveDialogRoute
import com.trainpaths.nonogram.navigation.MenuRoute
import com.trainpaths.nonogram.navigation.PlayDialogRoute
import com.trainpaths.nonogram.navigation.SettingsRoute
import com.trainpaths.nonogram.screens.GameScreen
import com.trainpaths.nonogram.screens.MenuScreen
import com.trainpaths.nonogram.screens.SettingsScreen

@Composable
fun App(
    menuViewModel: MenuViewModel,
    gameViewModelFactory: @Composable (Long) -> GameViewModel,
) {
    AppTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = MenuRoute,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxSize(),
        ) {
            composable<MenuRoute> {
                MenuScreen(
                    viewModel = menuViewModel,
                    onNonogramClick = { id -> navController.navigate(PlayDialogRoute(id)) },
                    onSettingsClick = { navController.navigate(SettingsRoute) },
                )
            }
            dialog<PlayDialogRoute> { entry ->
                val route: PlayDialogRoute = entry.toRoute()
                PlayConfirmDialog(
                    onConfirm = {
                        navController.navigate(GameRoute(route.nonogramId)) {
                            popUpTo(MenuRoute)
                        }
                    },
                    onDismiss = { navController.popBackStack() },
                )
            }
            composable<GameRoute> { entry ->
                val route: GameRoute = entry.toRoute()
                val viewModel = gameViewModelFactory(route.nonogramId)
                LaunchedEffect(route.nonogramId) {
                    viewModel.loadNonogram(route.nonogramId)
                }
                GameScreen(
                    viewModel = viewModel,
                    onBack = { navController.navigate(LeaveDialogRoute) },
                    onSettingsClick = { navController.navigate(SettingsRoute) },
                )
            }
            dialog<LeaveDialogRoute> {
                LeaveConfirmDialog(
                    onConfirm = { navController.popBackStack(MenuRoute, inclusive = false) },
                    onDismiss = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
