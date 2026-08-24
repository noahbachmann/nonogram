package com.trainpaths.nonogram.screens.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainpaths.nonogram.auth.AuthRepository
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
) : ViewModel() {

    val authState = authRepository.authState
    val hasCompletedOnboarding get() = authRepository.hasCompletedOnboarding

    private val _signInComplete = MutableStateFlow(false)
    val signInComplete = _signInComplete.asStateFlow()

    private val _generatorSyncState =
        MutableStateFlow(GeneratorSyncState.IDLE)
    val generatorNonogramSyncState = _generatorSyncState.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    private val _publishBanned = MutableStateFlow(false)
    val publishBanned = _publishBanned.asStateFlow()

    fun onFirebaseSignInSuccess(firebaseUid: String, displayName: String?) {
        _signInComplete.value = false
        viewModelScope.launch(Dispatchers.Default) {
            val hasRemote = syncService.hasRemoteProgress(firebaseUid)
            authRepository.linkFirebaseUser(firebaseUid, displayName)
            if (hasRemote) {
                syncService.pullAllProgress(firebaseUid)
            } else {
                syncService.uploadAllLocalProgress(firebaseUid)
            }
            syncService.uploadAllLocalNonograms(firebaseUid)
            refreshPublishState(firebaseUid)
            _signInComplete.value = true
        }
    }

    fun syncAll(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val firebaseUid = authRepository.currentFirebaseUid ?: return@launch

                syncService.pullAndMergeAllProgress(firebaseUid)
                syncPublicNonograms(firebaseUid)
                syncOwnedNonograms(firebaseUid)
                refreshPublishState(firebaseUid)
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    fun retryOwnNonograms(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val firebaseUid = authRepository.currentFirebaseUid ?: return@launch
                syncOwnedNonograms(firebaseUid)
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    private suspend fun refreshPublishState(firebaseUid: String) {
        val admin = syncService.isAdmin(firebaseUid)
        authRepository.setIsAdmin(firebaseUid, admin)
        _isAdmin.value = admin

        val gate = syncService.fetchModerationGate(firebaseUid) ?: return
        authRepository.setModerationGate(firebaseUid, gate.denialStreak, gate.banned)
        _publishBanned.value = gate.banned
    }

    private suspend fun syncPublicNonograms(firebaseUid: String) {
        val lastSyncedAt = authRepository.getLastPublicNonogramSyncTimestamp(firebaseUid)
        val newestReceivedAt = syncService.pullPublicNonogramsSince(firebaseUid, lastSyncedAt)
        if (newestReceivedAt != null && newestReceivedAt != lastSyncedAt) {
            authRepository.setLastPublicNonogramSyncTimestamp(firebaseUid, newestReceivedAt)
        }
    }

    private suspend fun syncOwnedNonograms(firebaseUid: String) {
        val lastSyncedAt = authRepository.getLastOwnedNonogramSyncTimestamp(firebaseUid)
        _generatorSyncState.value = GeneratorSyncState.SYNCING
        try {
            val newestReceivedAt = syncService.pullOwnedNonograms(firebaseUid, lastSyncedAt)
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
                _isAdmin.value = false
                _publishBanned.value = false
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }
}
