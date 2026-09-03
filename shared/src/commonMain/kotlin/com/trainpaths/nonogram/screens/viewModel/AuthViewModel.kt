package com.trainpaths.nonogram.screens.viewModel

import androidx.lifecycle.ViewModel
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.auth.firebaseSignOut
import com.trainpaths.nonogram.sync.SyncService
import com.trainpaths.nonogram.sync.syncPublicNonograms
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        launchGuarded {
            authRepository.currentUserUid.collect {
                val firebaseUid = authRepository.currentFirebaseUid
                _isAdmin.value = firebaseUid != null && authRepository.getIsAdmin(firebaseUid)
                _publishBanned.value =
                    firebaseUid != null && authRepository.getPublishBanned(firebaseUid)
            }
        }
    }

    fun onFirebaseSignInSuccess(firebaseUid: String, displayName: String?) {
        _signInComplete.value = false
        launchGuarded(
            Dispatchers.Default,
            onError = { println("SignIn: post-sign-in sync failed: ${it.message}") },
        ) {
            try {
                authRepository.linkFirebaseUser(firebaseUid, displayName)
                if (syncService.hasRemoteProgress(firebaseUid)) {
                    syncService.pullAllProgress(firebaseUid)
                } else {
                    syncService.uploadAllLocalProgress(firebaseUid)
                }
                syncService.uploadAllLocalNonograms(firebaseUid)
                refreshPublishState(firebaseUid)
            } finally {
                _signInComplete.value = true
            }
        }
    }

    fun syncAll(onComplete: () -> Unit = {}) {
        launchGuarded(Dispatchers.Default) {
            try {
                syncService.syncPublicNonograms(authRepository, authRepository.currentFirebaseUid)

                val firebaseUid = authRepository.currentFirebaseUid.orMissing() ?: return@launchGuarded
                syncService.pullAndMergeAllProgress(firebaseUid)
                syncOwnedNonograms(firebaseUid)
                refreshPublishState(firebaseUid)
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    fun retryOwnNonograms(onComplete: () -> Unit = {}) {
        launchGuarded(Dispatchers.Default) {
            try {
                val firebaseUid = authRepository.currentFirebaseUid.orMissing() ?: return@launchGuarded
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
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            println("FirestoreSync: owned nonogram sync for Generator failed: ${error.message}")
            _generatorSyncState.value = GeneratorSyncState.ERROR
        }
    }

    fun completeOnboarding() {
        authRepository.completeOnboarding()
    }

    fun signOut(onComplete: () -> Unit = {}) {
        launchGuarded(Dispatchers.Default) {
            try {
                try {
                    firebaseSignOut()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    println("SignOut: firebase sign-out failed: ${error.message}")
                }
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
