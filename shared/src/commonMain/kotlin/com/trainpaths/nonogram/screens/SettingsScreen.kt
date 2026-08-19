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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.ColorTheme
import com.trainpaths.nonogram.navigation.TopAppBar
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import com.trainpaths.nonogram.screens.viewModel.ThemeViewModel
import com.trainpaths.nonogram.auth.AuthState

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
) {
    val authState by authViewModel.authState.collectAsState()
    val theme by themeViewModel.theme.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = "Settings",
            onBack = onBack,
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
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
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            onSelect = { themeViewModel.selectTheme(entry) },
                        )
                    }
                }
            }
        }
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
                .background(theme.scheme.primary)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .semantics { contentDescription = theme.label },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(14.dp).background(theme.scheme.onPrimary, CircleShape))
        }
    }
}
