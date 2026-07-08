package com.trainpaths.nonogram.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.trainpaths.nonogram.icons.arrowBack
import com.trainpaths.nonogram.icons.build
import com.trainpaths.nonogram.icons.edit_square
import com.trainpaths.nonogram.icons.indeterminate_question_box
import com.trainpaths.nonogram.icons.settings
import com.trainpaths.nonogram.navigation.SettingsRoute

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}

enum class AppBarMode { PUZZLE, GENERATOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NonogramAppBar(
    title: String = "",
    onBack: (() -> Unit)? = null,
    showSettings: Boolean = false,
    mode: AppBarMode? = null,
    onSwapMode: (() -> Unit)? = null,
) {
    val navController = if (showSettings) LocalNavController.current else null
    TopAppBar(
        title = {
            if (mode != null && onSwapMode != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onSwapMode) {
                        Icon(
                            imageVector = if (mode == AppBarMode.PUZZLE) indeterminate_question_box else edit_square,
                            contentDescription = if (mode == AppBarMode.PUZZLE) "Switch to Generator" else "Switch to Puzzles",
                        )
                    }
                }
            } else if (title.isNotEmpty()) {
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(if (mode == AppBarMode.PUZZLE) arrowBack else build, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (showSettings) {
                IconButton(onClick = { navController?.navigate(SettingsRoute) }) {
                    Icon(settings, contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
