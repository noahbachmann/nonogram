package com.trainpaths.nonogram.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun GoogleSignInSection(
    onSignedIn: (uid: String, displayName: String?) -> Unit,
    modifier: Modifier,
) {
    // No web sign-in in guest-only v1; see milestone-2 issue for the Firebase JS SDK bindings.
}
