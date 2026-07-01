package com.trainpaths.nonogram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val authState = authRepository.authState
    val currentUserId = authRepository.currentUserId
    val hasCompletedOnboarding get() = authRepository.hasCompletedOnboarding

    fun onFirebaseSignInSuccess(firebaseUid: String, displayName: String?) {
        viewModelScope.launch(Dispatchers.Default) {
            authRepository.linkFirebaseUser(firebaseUid, displayName)
        }
    }

    fun completeOnboarding() {
        authRepository.completeOnboarding()
    }
}
