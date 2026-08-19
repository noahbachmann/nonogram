package com.trainpaths.nonogram.screens.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.auth.AuthState
import com.trainpaths.nonogram.auth.firebaseSignOut
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GeneratorSyncState { IDLE, SYNCING, ERROR }

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

    private val _generatorSyncState =
        MutableStateFlow(GeneratorSyncState.IDLE)
    val generatorNonogramSyncState = _generatorSyncState.asStateFlow()

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
            _signInComplete.value = true
        }
    }

    fun syncAll(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (authRepository.authState.value != AuthState.SIGNED_IN) return@launch
                val userId = authRepository.currentUserId.value ?: return@launch
                val firebaseUid = sdk.getUserById(userId)?.firebaseUid ?: return@launch

                syncService.pullAndMergeAllProgress(firebaseUid, userId)
                syncPublicNonograms(firebaseUid, userId)
                syncOwnedNonograms(firebaseUid, userId)
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    fun retryOwnNonograms(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (authRepository.authState.value != AuthState.SIGNED_IN) return@launch
                val userId = authRepository.currentUserId.value ?: return@launch
                val firebaseUid = sdk.getUserById(userId)?.firebaseUid ?: return@launch
                syncOwnedNonograms(firebaseUid, userId)
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    private suspend fun syncPublicNonograms(
        firebaseUid: String,
        userId: Long,
    ) {
        val lastSyncedAt = authRepository.getLastPublicNonogramSyncTimestamp(firebaseUid)
        val newestReceivedAt = syncService.pullPublicNonogramsSince(firebaseUid, userId, lastSyncedAt)
        if (newestReceivedAt != null && newestReceivedAt != lastSyncedAt) {
            authRepository.setLastPublicNonogramSyncTimestamp(firebaseUid, newestReceivedAt)
        }
    }

    private suspend fun syncOwnedNonograms(
        firebaseUid: String,
        userId: Long,
    ) {
        val lastSyncedAt = authRepository.getLastOwnedNonogramSyncTimestamp(firebaseUid)
        _generatorSyncState.value = GeneratorSyncState.SYNCING
        try {
            val newestReceivedAt = syncService.pullOwnedNonograms(firebaseUid, userId, lastSyncedAt)
            if (newestReceivedAt == null) {
                _generatorSyncState.value = GeneratorSyncState.ERROR
                return
            }
            if (newestReceivedAt != lastSyncedAt) {
                authRepository.setLastOwnedNonogramSyncTimestamp(firebaseUid, newestReceivedAt)
            }
            _generatorSyncState.value = GeneratorSyncState.IDLE
        } catch (error: Throwable) {
            println("FirestoreSync: owned nonogram sync for Generator failed: ${error.message}")
            _generatorSyncState.value = GeneratorSyncState.ERROR
        }
    }

    fun completeOnboarding() {
        authRepository.completeOnboarding()
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                runCatching { firebaseSignOut() }
                    .onFailure { println("SignOut: firebase sign-out failed: ${it.message}") }
                authRepository.signOut()
                _signInComplete.value = false
                _generatorSyncState.value = GeneratorSyncState.IDLE
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }
}
