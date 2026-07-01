package com.trainpaths.nonogram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.sync.FirestoreSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncService: FirestoreSyncService,
    private val sdk: AppSDK,
) : ViewModel() {

    val authState = authRepository.authState
    val currentUserId = authRepository.currentUserId
    val hasCompletedOnboarding get() = authRepository.hasCompletedOnboarding

    private val _signInComplete = MutableStateFlow(false)
    val signInComplete = _signInComplete.asStateFlow()

    fun onFirebaseSignInSuccess(firebaseUid: String, displayName: String?) {
        _signInComplete.value = false
        viewModelScope.launch(Dispatchers.Default) {
            val hasRemote = syncService.hasRemoteProgress(firebaseUid)
            authRepository.linkFirebaseUser(firebaseUid, displayName)
            val userId = authRepository.currentUserId.value ?: return@launch
            if (hasRemote) {
                syncService.pullAndMergeAllProgress(firebaseUid, userId)
            } else {
                syncService.uploadAllLocalProgress(firebaseUid, userId)
            }
            _signInComplete.value = true
        }
    }

    fun syncOnStart() {
        viewModelScope.launch(Dispatchers.Default) {
            if (authRepository.authState.value == AuthState.SIGNED_IN) {
                val userId = authRepository.currentUserId.value ?: return@launch
                val user = sdk.getUserById(userId) ?: return@launch
                val firebaseUid = user.firebaseUid ?: return@launch
                syncService.pullAndMergeAllProgress(firebaseUid, userId)
            }
        }
    }

    fun completeOnboarding() {
        authRepository.completeOnboarding()
    }
}
