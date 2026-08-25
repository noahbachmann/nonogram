package com.trainpaths.nonogram.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import com.trainpaths.nonogram.BUTTON_SHAPE
import com.trainpaths.nonogram.firebase.FirebaseWeb
import kotlinx.coroutines.launch

@Composable
actual fun GoogleSignInSection(
    onSignedIn: (uid: String, displayName: String?) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()

    GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
        // kmpauth's web flow (GIS token client) usually yields only an access token, no ID token.
        val idToken = googleUser?.idToken?.takeIf { it.isNotBlank() }
        val accessToken = googleUser?.accessToken
        if (idToken == null && accessToken == null) {
            println("GoogleSignIn(web): no token returned")
        } else {
            scope.launch {
                try {
                    val user = FirebaseWeb.signInWithGoogle(idToken, accessToken)
                    onSignedIn(user.uid, user.displayName ?: googleUser.displayName.takeIf { it.isNotBlank() })
                } catch (e: Throwable) {
                    println("GoogleSignIn(web): Firebase sign-in failed: ${e.message}")
                }
            }
        }
    }) {
        GoogleSignInButton(
            modifier = modifier,
            shape = BUTTON_SHAPE,
            fontSize = 19.sp,
            text = "Sign in with Google"
        ) { this.onClick() }
    }
}
