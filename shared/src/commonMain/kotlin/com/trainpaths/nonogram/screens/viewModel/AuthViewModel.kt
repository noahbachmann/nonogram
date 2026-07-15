package com.trainpaths.nonogram.screens.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncService: SyncService,
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
                syncService.pullAllProgress(firebaseUid, userId)
            } else {
                syncService.uploadAllLocalProgress(firebaseUid, userId)
            }
            syncService.uploadAllLocalNonograms(firebaseUid, userId)
            pullNonogramsIncremental(firebaseUid, userId)
            _signInComplete.value = true
        }
    }

    fun syncOnStart(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (authRepository.authState.value == AuthState.SIGNED_IN) {
                    val userId = authRepository.currentUserId.value ?: return@launch
                    val user = sdk.getUserById(userId) ?: return@launch
                    val firebaseUid = user.firebaseUid ?: return@launch
                    syncService.pullAndMergeAllProgress(firebaseUid, userId)
                }
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    /** Only fetches docs with updatedAt past the persisted cursor. */
    fun syncNonograms(onNewData: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            if (authRepository.authState.value != AuthState.SIGNED_IN) return@launch
            val userId = authRepository.currentUserId.value ?: return@launch
            val firebaseUid = sdk.getUserById(userId)?.firebaseUid ?: return@launch
            if (pullNonogramsIncremental(firebaseUid, userId)) {
                withContext(Dispatchers.Main) { onNewData() }
            }
        }
    }

    private suspend fun pullNonogramsIncremental(firebaseUid: String, userId: Long): Boolean {
        val cursor = authRepository.getNonogramSyncCursor(firebaseUid)
        val newCursor = syncService.pullNonogramsSince(firebaseUid, userId, cursor)
        if (newCursor == cursor) return false
        authRepository.setNonogramSyncCursor(firebaseUid, newCursor)
        return true
    }

    fun completeOnboarding() {
        authRepository.completeOnboarding()
    }
}
