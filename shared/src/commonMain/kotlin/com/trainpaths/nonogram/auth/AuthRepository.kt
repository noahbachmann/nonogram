package com.trainpaths.nonogram.auth

import com.russhwolf.settings.Settings
import com.trainpaths.nonogram.AppSDK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

private const val KEY_CURRENT_USER_UID = "current_user_uid"
private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
private const val KEY_PUBLIC_NONOGRAM_PREFIX = "public_nonogram_sync_timestamp_"
private const val KEY_OWNED_NONOGRAM_PREFIX = "owned_nonogram_sync_timestamp_"
private const val KEY_PUBLISH_BANNED_PREFIX = "publish_banned_"
private const val KEY_DENIAL_STREAK_PREFIX = "denial_streak_"
private const val KEY_IS_ADMIN_PREFIX = "is_admin_"

/** Marks a user key as device-local, i.e. a guest's. Firebase uids are alphanumeric. */
private const val LOCAL_USER_PREFIX = "local:"

enum class AuthState { INITIALIZING, GUEST, SIGNED_IN }

class AuthRepository(private val sdk: AppSDK, private val settings: Settings) {

    private val _authState = MutableStateFlow(AuthState.INITIALIZING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * The one key for everything this user owns — `Nonogram.authorUid` and `UserProgress.userUid`.
     * The Firebase uid once signed in, a device-local key while a guest.
     */
    private val _currentUserUid = MutableStateFlow<String?>(null)
    val currentUserUid: StateFlow<String?> = _currentUserUid.asStateFlow()

    /** The same key, but null while a guest — for the calls that must reach Firestore. */
    val currentFirebaseUid: String?
        get() = _currentUserUid.value?.takeUnless { it.startsWith(LOCAL_USER_PREFIX) }

    val hasCompletedOnboarding: Boolean
        get() = settings.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)

    suspend fun initialize() {
        val savedUid = settings.getStringOrNull(KEY_CURRENT_USER_UID)
        if (savedUid != null && sdk.getUser(savedUid) != null) {
            _currentUserUid.value = savedUid
            _authState.value =
                if (savedUid.startsWith(LOCAL_USER_PREFIX)) AuthState.GUEST else AuthState.SIGNED_IN
        } else {
            startGuest()
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

    fun getPublishBanned(firebaseUid: String): Boolean =
        settings.getBoolean(KEY_PUBLISH_BANNED_PREFIX + firebaseUid, false)

    fun getDenialStreak(firebaseUid: String): Int =
        settings.getInt(KEY_DENIAL_STREAK_PREFIX + firebaseUid, 0)

    fun setModerationGate(firebaseUid: String, denialStreak: Int, banned: Boolean) {
        settings.putInt(KEY_DENIAL_STREAK_PREFIX + firebaseUid, denialStreak)
        settings.putBoolean(KEY_PUBLISH_BANNED_PREFIX + firebaseUid, banned)
    }

    fun getIsAdmin(firebaseUid: String): Boolean =
        settings.getBoolean(KEY_IS_ADMIN_PREFIX + firebaseUid, false)

    fun setIsAdmin(firebaseUid: String, isAdmin: Boolean) =
        settings.putBoolean(KEY_IS_ADMIN_PREFIX + firebaseUid, isAdmin)

    suspend fun linkFirebaseUser(firebaseUid: String, displayName: String?) {
        val previousUid = _currentUserUid.value
        sdk.upsertUser(firebaseUid, displayName ?: "User")
        if (previousUid != null && previousUid.startsWith(LOCAL_USER_PREFIX)) {
            sdk.reassignAuthor(previousUid, firebaseUid)
            sdk.mergeProgressInto(previousUid, firebaseUid)
            sdk.deleteUser(previousUid)
        }
        settings.putString(KEY_CURRENT_USER_UID, firebaseUid)
        _currentUserUid.value = firebaseUid
        _authState.value = AuthState.SIGNED_IN
        completeOnboarding()
    }

    suspend fun signOut() {
        startGuest()
    }

    private suspend fun startGuest() {
        val guestUid = LOCAL_USER_PREFIX + Random.nextLong(1L shl 20, 1L shl 53)
        sdk.upsertUser(guestUid, "Guest")
        settings.putString(KEY_CURRENT_USER_UID, guestUid)
        _currentUserUid.value = guestUid
        _authState.value = AuthState.GUEST
    }
}
