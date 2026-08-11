package com.trainpaths.nonogram.auth

import com.russhwolf.settings.Settings
import com.trainpaths.nonogram.AppSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AuthState { INITIALIZING, GUEST, SIGNED_IN }

class AuthRepository(private val sdk: AppSDK, private val settings: Settings) {

    private val _authState = MutableStateFlow(AuthState.INITIALIZING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    val hasCompletedOnboarding: Boolean
        get() = settings.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)

    suspend fun initialize() {
        val savedId = settings.getLongOrNull(KEY_CURRENT_USER_ID)
        if (savedId != null && sdk.getUserById(savedId) != null) {
            _currentUserId.value = savedId
            val user = sdk.getUserById(savedId)
            _authState.value = if (user?.firebaseUid != null) AuthState.SIGNED_IN else AuthState.GUEST
        } else {
            val guestId = sdk.addUser("Guest")
            settings.putLong(KEY_CURRENT_USER_ID, guestId)
            _currentUserId.value = guestId
            _authState.value = AuthState.GUEST
        }
    }

    fun completeOnboarding() {
        settings.putBoolean(KEY_HAS_COMPLETED_ONBOARDING, true)
    }

    fun getLastPublicNonogramSyncTimestamp(firebaseUid: String): Long =
        settings.getLong(KEY_PUBLIC_NONOGRAM_PREFIX + firebaseUid, 0L)

    fun setLastPublicNonogramSyncTimestamp(firebaseUid: String, timestamp: Long) {
        settings.putLong(KEY_PUBLIC_NONOGRAM_PREFIX + firebaseUid, timestamp)
    }

    fun getLastOwnedNonogramSyncTimestamp(firebaseUid: String): Long =
        settings.getLong(KEY_OWNED_NONOGRAM_PREFIX + firebaseUid, 0L)

    fun setLastOwnedNonogramSyncTimestamp(firebaseUid: String, timestamp: Long) {
        settings.putLong(KEY_OWNED_NONOGRAM_PREFIX + firebaseUid, timestamp)
    }

    suspend fun linkFirebaseUser(firebaseUid: String, displayName: String?) {
        val existingUser = sdk.getUserByFirebaseUid(firebaseUid)
        if (existingUser != null) {
            _currentUserId.value = existingUser.id
            settings.putLong(KEY_CURRENT_USER_ID, existingUser.id)
        } else {
            val userId = _currentUserId.value ?: return
            sdk.updateUserFirebaseUid(userId, firebaseUid, displayName ?: "User")
        }
        _authState.value = AuthState.SIGNED_IN
        completeOnboarding()
    }

    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
        private const val KEY_PUBLIC_NONOGRAM_PREFIX = "public_nonogram_sync_timestamp_"
        private const val KEY_OWNED_NONOGRAM_PREFIX = "owned_nonogram_sync_timestamp_"
    }
}
