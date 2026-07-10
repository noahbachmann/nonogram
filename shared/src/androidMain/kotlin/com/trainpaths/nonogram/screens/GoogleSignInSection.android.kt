package com.trainpaths.nonogram.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import dev.gitlive.firebase.auth.FirebaseUser

@Composable
actual fun GoogleSignInSection(
    onSignedIn: (uid: String, displayName: String?) -> Unit,
    modifier: Modifier,
) {
    val onFirebaseResult: (Result<FirebaseUser?>) -> Unit = { result ->
        if (result.isSuccess) {
            val firebaseUser = result.getOrNull()
            if (firebaseUser != null) {
                onSignedIn(firebaseUser.uid, firebaseUser.displayName)
            }
        } else {
            println("Error Result: ${result.exceptionOrNull()?.message}")
        }
    }

    GoogleButtonUiContainerFirebase(onResult = onFirebaseResult, linkAccount = false) {
        GoogleSignInButton(
            modifier = modifier,
            fontSize = 19.sp,
            text = "Sign in with Google"
        ) { this.onClick() }
    }
}
