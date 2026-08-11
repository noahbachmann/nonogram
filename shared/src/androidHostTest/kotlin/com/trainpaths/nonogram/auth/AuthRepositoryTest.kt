package com.trainpaths.nonogram.auth

import com.russhwolf.settings.MapSettings
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.TestDatabaseFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthRepositoryTest {

    private lateinit var sdk: AppSDK
    private lateinit var settings: MapSettings
    private lateinit var authRepo: AuthRepository

    @BeforeTest
    fun setUp() {
        sdk = AppSDK(TestDatabaseFactory())
        settings = MapSettings()
        authRepo = AuthRepository(sdk, settings)
    }

    @Test
    fun initialState_isInitializing() {
        assertEquals(AuthState.INITIALIZING, authRepo.authState.value)
    }

    @Test
    fun initialize_noSavedUser_createsGuestAndSetsGuest() = runTest {
        authRepo.initialize()
        assertEquals(AuthState.GUEST, authRepo.authState.value)
        assertNotNull(authRepo.currentUserId.value)
    }

    @Test
    fun initialize_withSavedGuestUser_restoresAndSetsGuest() = runTest {
        val userId = sdk.addUser("Guest")
        settings.putLong("current_user_id", userId)

        authRepo.initialize()

        assertEquals(AuthState.GUEST, authRepo.authState.value)
        assertEquals(userId, authRepo.currentUserId.value)
    }

    @Test
    fun initialize_withSavedSignedInUser_setsSignedIn() = runTest {
        val userId = sdk.addUser("Signed User")
        sdk.updateUserFirebaseUid(userId, "firebase-abc", "Signed User")
        settings.putLong("current_user_id", userId)

        authRepo.initialize()

        assertEquals(AuthState.SIGNED_IN, authRepo.authState.value)
        assertEquals(userId, authRepo.currentUserId.value)
    }

    @Test
    fun initialize_withStaleUserId_createsNewGuest() = runTest {
        settings.putLong("current_user_id", 9999)

        authRepo.initialize()

        assertEquals(AuthState.GUEST, authRepo.authState.value)
        val userId = authRepo.currentUserId.value
        assertNotNull(userId)
        assertNotNull(sdk.getUserById(userId))
    }

    @Test
    fun linkFirebaseUser_newUid_updatesCurrentUser() = runTest {
        authRepo.initialize()
        val userId = authRepo.currentUserId.value!!

        authRepo.linkFirebaseUser("firebase-new", "New Name")

        assertEquals(AuthState.SIGNED_IN, authRepo.authState.value)
        assertEquals(userId, authRepo.currentUserId.value)
        val user = sdk.getUserByFirebaseUid("firebase-new")
        assertNotNull(user)
        assertEquals(userId, user.id)
        assertEquals("New Name", user.name)
    }

    @Test
    fun linkFirebaseUser_existingUid_switchesToThatUser() = runTest {
        val existingUserId = sdk.addUser("Existing")
        sdk.updateUserFirebaseUid(existingUserId, "firebase-existing", "Existing")

        authRepo.initialize()
        val guestId = authRepo.currentUserId.value!!

        authRepo.linkFirebaseUser("firebase-existing", "Existing")

        assertEquals(AuthState.SIGNED_IN, authRepo.authState.value)
        assertEquals(existingUserId, authRepo.currentUserId.value)
        assertTrue(existingUserId != guestId)
    }

    @Test
    fun completeOnboarding_setsFlag() {
        assertFalse(authRepo.hasCompletedOnboarding)
        authRepo.completeOnboarding()
        assertTrue(authRepo.hasCompletedOnboarding)
    }

    @Test
    fun linkFirebaseUser_alsoCompletesOnboarding() = runTest {
        authRepo.initialize()
        assertFalse(authRepo.hasCompletedOnboarding)
        authRepo.linkFirebaseUser("firebase-xyz", "User")
        assertTrue(authRepo.hasCompletedOnboarding)
    }

    @Test
    fun nonogramSyncTimestamps_defaultToZero() {
        assertEquals(0L, authRepo.getLastPublicNonogramSyncTimestamp("firebase-user"))
        assertEquals(0L, authRepo.getLastOwnedNonogramSyncTimestamp("firebase-user"))
    }

    @Test
    fun nonogramSyncTimestamps_areIndependentByListAndUser() {
        authRepo.setLastPublicNonogramSyncTimestamp("firebase-user", 100)
        authRepo.setLastOwnedNonogramSyncTimestamp("firebase-user", 200)

        assertEquals(100L, authRepo.getLastPublicNonogramSyncTimestamp("firebase-user"))
        assertEquals(200L, authRepo.getLastOwnedNonogramSyncTimestamp("firebase-user"))
        assertEquals(0L, authRepo.getLastPublicNonogramSyncTimestamp("other-user"))
        assertEquals(0L, authRepo.getLastOwnedNonogramSyncTimestamp("other-user"))
    }
}
