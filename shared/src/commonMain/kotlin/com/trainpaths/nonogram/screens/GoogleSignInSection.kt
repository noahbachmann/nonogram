package com.trainpaths.nonogram.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun GoogleSignInSection(
    onSignedIn: (uid: String, displayName: String?) -> Unit,
    modifier: Modifier = Modifier,
)
