package com.trainpaths.nonogram.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import com.trainpaths.nonogram.screens.viewModel.AuthViewModel
import dev.gitlive.firebase.auth.FirebaseUser

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    var isSigningIn by remember { mutableStateOf(false) }
    val signInComplete by authViewModel.signInComplete.collectAsState()

    LaunchedEffect(signInComplete) {
        if (signInComplete && isSigningIn) {
            isSigningIn = false
            onLoginSuccess()
        }
    }

    val onFirebaseResult: (Result<FirebaseUser?>) -> Unit = { result ->
        if (result.isSuccess) {
            val firebaseUser = result.getOrNull()
            if (firebaseUser != null) {
                isSigningIn = true
                authViewModel.onFirebaseSignInSuccess(firebaseUser.uid, firebaseUser.displayName)
            }
        } else {
            println("Error Result: ${result.exceptionOrNull()?.message}")
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nonogram",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (isSigningIn) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Syncing progress...", color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoogleButtonUiContainerFirebase(onResult = onFirebaseResult, linkAccount = false) {
                    GoogleSignInButton(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        fontSize = 19.sp,
                        text = "Sign in with Google"
                    ) { this.onClick() }
                }

                OutlinedButton(
                    onClick = {
                        authViewModel.completeOnboarding()
                        onContinueAsGuest()
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text("Continue as Guest")
                }
            }
        }
    }
}
