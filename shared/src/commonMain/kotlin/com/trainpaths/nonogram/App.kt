package com.trainpaths.nonogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.trainpaths.nonogram.dialogs.LeaveConfirmDialog
import com.trainpaths.nonogram.dialogs.PlayConfirmDialog
import com.trainpaths.nonogram.dialogs.WinConfirmDialog
import com.trainpaths.nonogram.navigation.GameRoute
import com.trainpaths.nonogram.navigation.GenConfRoute
import com.trainpaths.nonogram.navigation.GeneratorRoute
import com.trainpaths.nonogram.navigation.LeaveDialogRoute
import com.trainpaths.nonogram.navigation.LocalNavController
import com.trainpaths.nonogram.navigation.LoginRoute
import com.trainpaths.nonogram.navigation.MenuRoute
import com.trainpaths.nonogram.navigation.PlayDialogRoute
import com.trainpaths.nonogram.navigation.SettingsRoute
import com.trainpaths.nonogram.navigation.WinDialogRoute
import com.trainpaths.nonogram.screens.GameScreen
import com.trainpaths.nonogram.screens.GenConfScreen
import com.trainpaths.nonogram.screens.GenScreen
import com.trainpaths.nonogram.screens.LoginScreen
import com.trainpaths.nonogram.screens.MenuScreen
import com.trainpaths.nonogram.screens.SettingsScreen
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.GameViewModel
import com.trainpaths.nonogram.screens.viewModel.GenViewModel
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel

@Composable
fun App(
    menuViewModel: MenuViewModel,
    authViewModel: AuthViewModel,
    genViewModel: GenViewModel,
    gameViewModelFactory: @Composable (Long) -> GameViewModel,
) {
    val startDestination = if (authViewModel.hasCompletedOnboarding) MenuRoute else LoginRoute

    LaunchedEffect(Unit) {
        authViewModel.syncOnStart { menuViewModel.loadAll() }
    }

    AppTheme {
        val navController = rememberNavController()
        var onResetBoard by remember { mutableStateOf<(() -> Unit)?>(null) }
        CompositionLocalProvider(LocalNavController provides navController) {
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
                            menuViewModel.loadAll()
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
                        onGenClick = {
                            navController.navigate(GenConfRoute) {
                                popUpTo(MenuRoute) { inclusive = true }
                            }
                        },
                    )
                }
                composable<GenConfRoute> {
                    GenConfScreen(
                        genViewModel = genViewModel,
                        onMenuClick = {
                            navController.navigate(MenuRoute) {
                                popUpTo(GenConfRoute) { inclusive = true }
                            }
                        },
                        onStart = {
                            navController.navigate(GeneratorRoute)
                        },
                    )
                }
                composable<GeneratorRoute> {
                    GenScreen(
                        genViewModel = genViewModel,
                        onBack = { navController.popBackStack() },
                        onSwapMode = {
                            navController.navigate(MenuRoute) {
                                popUpTo(GenConfRoute) { inclusive = true }
                            }
                        },
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
                    onResetBoard = { viewModel.resetBoard() }
                    LaunchedEffect(route.nonogramId) {
                        viewModel.loadNonogram(route.nonogramId)
                    }
                    GameScreen(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.saveCurrentProgress()
                            viewModel.currentNonogramId?.let { id ->
                                menuViewModel.updateSingleProgress(id, viewModel.currentBoardAsInts)
                            }
                            navController.navigate(LeaveDialogRoute)
                        },
                        onWin = {
                            viewModel.saveCurrentProgress(true)
                            viewModel.currentNonogramId?.let { id ->
                                menuViewModel.clearProgress(id)
                                menuViewModel.incrementBeatCount(id)
                            }
                            navController.navigate(WinDialogRoute)
                        },
                        onSwapMode = {
                            viewModel.saveCurrentProgress()
                            viewModel.currentNonogramId?.let { id ->
                                menuViewModel.updateSingleProgress(id, viewModel.currentBoardAsInts)
                            }
                            navController.navigate(GenConfRoute) {
                                popUpTo(MenuRoute) { inclusive = true }
                            }
                        },
                    )
                }
                dialog<WinDialogRoute> {
                    WinConfirmDialog(
                        onConfirm = {
                            navController.navigate(MenuRoute) {
                                popUpTo(MenuRoute) { inclusive = true }
                            }
                        },
                        onRestart = {
                            onResetBoard?.invoke()
                            navController.popBackStack()
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
}
