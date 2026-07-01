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
import com.trainpaths.nonogram.navigation.LoginRoute
import com.trainpaths.nonogram.navigation.MenuRoute
import com.trainpaths.nonogram.navigation.PlayDialogRoute
import com.trainpaths.nonogram.navigation.SettingsRoute
import com.trainpaths.nonogram.screens.GameScreen
import com.trainpaths.nonogram.screens.LoginScreen
import com.trainpaths.nonogram.screens.MenuScreen
import com.trainpaths.nonogram.screens.SettingsScreen

@Composable
fun App(
    menuViewModel: MenuViewModel,
    authViewModel: AuthViewModel,
    gameViewModelFactory: @Composable (Long) -> GameViewModel,
) {
    val startDestination = if (authViewModel.hasCompletedOnboarding) MenuRoute else LoginRoute

    AppTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxSize(),
        ) {
            composable<LoginRoute> {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(MenuRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onContinueAsGuest = {
                        navController.navigate(MenuRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                )
            }
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
                    onBack = {
                        viewModel.saveCurrentProgress()
                        navController.navigate(LeaveDialogRoute)
                    },
                    onSettingsClick = {
                        viewModel.saveCurrentProgress()
                        navController.navigate(SettingsRoute)
                    },
                )
            }
            dialog<LeaveDialogRoute> {
                LeaveConfirmDialog(
                    onConfirm = { navController.popBackStack(MenuRoute, inclusive = false) },
                    onDismiss = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onSignIn = { navController.navigate(LoginRoute) },
                )
            }
        }
    }
}
