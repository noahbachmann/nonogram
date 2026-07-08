package com.trainpaths.nonogram

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.trainpaths.nonogram.icons.arrowBack
import com.trainpaths.nonogram.icons.settings
import com.trainpaths.nonogram.navigation.SettingsRoute

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NonogramAppBar(
    title: String = "",
    onBack: (() -> Unit)? = null,
    showSettings: Boolean = false,
    centerContent: (@Composable () -> Unit)? = null,
) {
    val navController = if (showSettings) LocalNavController.current else null
    TopAppBar(
        title = {
            if (centerContent != null) {
                centerContent()
            } else if (title.isNotEmpty()) {
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(arrowBack, contentDescription = "Back")
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

private val rootTabs = listOf("Puzzles", "Generator")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreenToggle(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 8.dp)) {
        rootTabs.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index, rootTabs.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondary,
                    activeContentColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.primary,
                    inactiveContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.secondary,
                    inactiveBorderColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(label)
            }
        }
    }
}
