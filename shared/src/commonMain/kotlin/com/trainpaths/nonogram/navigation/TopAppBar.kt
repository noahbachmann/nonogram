package com.trainpaths.nonogram.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.trainpaths.nonogram.icons.arrowBack
import com.trainpaths.nonogram.icons.build
import com.trainpaths.nonogram.icons.edit_square
import com.trainpaths.nonogram.icons.indeterminate_question_box
import com.trainpaths.nonogram.icons.settings

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}

enum class AppBarMode { PUZZLE, GENERATOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: String = "",
    onBack: (() -> Unit)? = null,
    showSettings: Boolean = false,
    mode: AppBarMode? = null,
    onSwapMode: (() -> Unit)? = null,
    backArrow: Boolean = false,
) {
    val navController = if (showSettings) LocalNavController.current else null
    CenterAlignedTopAppBar(
        title = {
            if (mode != null && onSwapMode != null) {
                IconButton(onClick = onSwapMode) {
                    Icon(
                        imageVector = if (mode == AppBarMode.PUZZLE) indeterminate_question_box else edit_square,
                        contentDescription = if (mode == AppBarMode.PUZZLE) "Switch to Generator" else "Switch to Puzzles",
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else if (title.isNotEmpty()) {
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = if (backArrow || mode == AppBarMode.PUZZLE) arrowBack else build,
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        },
        actions = {
            if (showSettings) {
                IconButton(onClick = { navController?.navigate(SettingsRoute) }) {
                    Icon(
                        settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            scrolledContainerColor = Color.Unspecified,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
    )
}
