package com.trainpaths.nonogram.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.ColorTheme
import com.trainpaths.nonogram.switchColors
import com.trainpaths.nonogram.darken
import com.trainpaths.nonogram.dialogs.SignOutConfirmDialog
import com.trainpaths.nonogram.icons.settings
import com.trainpaths.nonogram.navigation.TopAppBar
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.SettingsViewModel
import com.trainpaths.nonogram.auth.AuthState

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onAdminPanel: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val authState by authViewModel.authState.collectAsState()
    val theme by settingsViewModel.theme.collectAsState()
    val showAllNames by settingsViewModel.showAllNames.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            titleIcon = settings,
            onBack = onBack,
            backArrow = true,
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ColorTheme.entries.forEach { entry ->
                        ThemeSwatch(
                            theme = entry,
                            selected = entry == theme,
                            onSelect = { settingsViewModel.selectTheme(entry) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Always show names",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Switch(
                    checked = showAllNames,
                    onCheckedChange = { settingsViewModel.setShowAllNames(it) },
                    colors = switchColors(),
                )
            }
            if (isAdmin) {
                Button(
                    onClick = onAdminPanel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Admin panel")
                }
            }
            if (authState == AuthState.GUEST) {
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Sign In with Google")
                }
            } else if (authState == AuthState.SIGNED_IN) {
                Button(
                    onClick = { showSignOutDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Log Out")
                }
            }
        }
    }

    if (showSignOutDialog) {
        SignOutConfirmDialog(
            onConfirm = {
                showSignOutDialog = false
                onSignOut()
            },
            onCancel = { showSignOutDialog = false },
        )
    }
}

@Composable
private fun ThemeSwatch(
    theme: ColorTheme,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier = Modifier.size(48.dp)) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .offset(x = 4.dp, y = 4.dp)
                    .background(color = MaterialTheme.colorScheme.onPrimary, shape = shape)
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .background(theme.scheme.primary.darken(if (selected) 0.15f else 0f))
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .semantics { contentDescription = theme.label },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(14.dp).background(theme.scheme.onPrimary, CircleShape))
        }
    }
}
