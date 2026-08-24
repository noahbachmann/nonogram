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
import kotlin.test.assertNull
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
        val uid = authRepo.currentUserUid.value
        assertNotNull(uid)
        assertTrue(uid.startsWith("local:"))
        assertNotNull(sdk.getUser(uid))
    }

    @Test
    fun initialize_withSavedGuestUser_restoresAndSetsGuest() = runTest {
        sdk.upsertUser("local:1", "Guest")
        settings.putString("current_user_uid", "local:1")

        authRepo.initialize()

        assertEquals(AuthState.GUEST, authRepo.authState.value)
        assertEquals("local:1", authRepo.currentUserUid.value)
    }

    @Test
    fun initialize_withSavedSignedInUser_setsSignedIn() = runTest {
        sdk.upsertUser("firebase-abc", "Signed User")
        settings.putString("current_user_uid", "firebase-abc")

        authRepo.initialize()

        assertEquals(AuthState.SIGNED_IN, authRepo.authState.value)
        assertEquals("firebase-abc", authRepo.currentUserUid.value)
    }

    @Test
    fun initialize_withStaleUserUid_createsNewGuest() = runTest {
        settings.putString("current_user_uid", "local:9999")

        authRepo.initialize()

        assertEquals(AuthState.GUEST, authRepo.authState.value)
        val uid = authRepo.currentUserUid.value
        assertNotNull(uid)
        assertTrue(uid != "local:9999")
        assertNotNull(sdk.getUser(uid))
    }

    @Test
    fun currentFirebaseUid_isNullForAGuestAndTheUidWhenSignedIn() = runTest {
        authRepo.initialize()
        assertNull(authRepo.currentFirebaseUid)

        authRepo.linkFirebaseUser("firebase-abc", "Name")
        assertEquals("firebase-abc", authRepo.currentFirebaseUid)

        authRepo.signOut()
        assertNull(authRepo.currentFirebaseUid)
    }

    @Test
    fun linkFirebaseUser_switchesTheKeyAndStoresTheDisplayName() = runTest {
        authRepo.initialize()

        authRepo.linkFirebaseUser("firebase-new", "New Name")

        assertEquals(AuthState.SIGNED_IN, authRepo.authState.value)
        assertEquals("firebase-new", authRepo.currentUserUid.value)
        assertEquals("firebase-new", settings.getStringOrNull("current_user_uid"))
        val user = sdk.getUser("firebase-new")
        assertNotNull(user)
        assertEquals("New Name", user.name)
    }

    @Test
    fun linkFirebaseUser_movesGuestAuthoredPuzzlesToTheUid() = runTest {
        authRepo.initialize()
        val guestKey = authRepo.currentUserUid.value!!
        val drawn = sdk.addNonogram("EASY", listOf(listOf(1)), authorUid = guestKey)

        authRepo.linkFirebaseUser("firebase-new", "New Name")

        assertEquals(listOf(drawn), sdk.getNonogramsByAuthor("firebase-new").map { it.id })
        assertTrue(sdk.getNonogramsByAuthor(guestKey).isEmpty())
    }

    @Test
    fun linkFirebaseUser_movesGuestProgressToTheUid() = runTest {
        authRepo.initialize()
        val guestKey = authRepo.currentUserUid.value!!
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1)))
        sdk.saveProgressWithTimestamp(guestKey, nonogramId, "[[1]]", 100)

        authRepo.linkFirebaseUser("firebase-new", "New Name")

        val moved = sdk.getSingleProgress("firebase-new", nonogramId)
        assertNotNull(moved)
        assertEquals("[[1]]", moved.boardState)
        assertTrue(sdk.getProgressForUserWithTimestamp(guestKey).isEmpty())
    }

    @Test
    fun linkFirebaseUser_mergesGuestProgressOntoAnAccountThatAlreadyHasSome() = runTest {
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1)))
        sdk.upsertUser("firebase-existing", "Existing")
        sdk.saveProgressWithTimestamp("firebase-existing", nonogramId, "[[0]]", 100)

        authRepo.initialize()
        val guestKey = authRepo.currentUserUid.value!!
        sdk.saveProgressWithTimestamp(guestKey, nonogramId, "[[1]]", 200)

        authRepo.linkFirebaseUser("firebase-existing", "Existing")

        val merged = sdk.getSingleProgress("firebase-existing", nonogramId)
        assertNotNull(merged)
        assertEquals("[[1]]", merged.boardState)
    }

    @Test
    fun linkFirebaseUser_dropsTheEmptiedGuestRow() = runTest {
        authRepo.initialize()
        val guestKey = authRepo.currentUserUid.value!!

        authRepo.linkFirebaseUser("firebase-new", "New Name")

        assertNull(sdk.getUser(guestKey))
    }

    @Test
    fun linkFirebaseUser_neverReassignsAnotherAccountsData() = runTest {
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1)))
        authRepo.initialize()
        authRepo.linkFirebaseUser("firebase-a", "A")
        val theirs = sdk.addNonogram("EASY", listOf(listOf(1)), authorUid = "firebase-a")
        sdk.saveProgressWithTimestamp("firebase-a", nonogramId, "[[1]]", 100)

        authRepo.linkFirebaseUser("firebase-b", "B")

        assertEquals(listOf(theirs), sdk.getNonogramsByAuthor("firebase-a").map { it.id })
        assertTrue(sdk.getNonogramsByAuthor("firebase-b").isEmpty())
        assertNotNull(sdk.getSingleProgress("firebase-a", nonogramId))
        assertNull(sdk.getSingleProgress("firebase-b", nonogramId))
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
        assertEquals(0L, authRepo.getLastPublicNonogramSyncTimestamp())
        assertEquals(0L, authRepo.getLastOwnedNonogramSyncTimestamp("firebase-user"))
    }

    @Test
    fun nonogramSyncTimestamps_areIndependentByList() {
        authRepo.setLastPublicNonogramSyncTimestamp(100)
        authRepo.setLastOwnedNonogramSyncTimestamp("firebase-user", 200)

        assertEquals(100L, authRepo.getLastPublicNonogramSyncTimestamp())
        assertEquals(200L, authRepo.getLastOwnedNonogramSyncTimestamp("firebase-user"))
        assertEquals(0L, authRepo.getLastOwnedNonogramSyncTimestamp("other-user"))
    }

    // The public cursor is device-wide: guests have no uid to key it by, and every user on the
    // device shares the same approved puzzles.
    @Test
    fun publicNonogramSyncTimestamp_isSharedAcrossUsers() = runTest {
        authRepo.initialize()
        authRepo.setLastPublicNonogramSyncTimestamp(100)

        authRepo.linkFirebaseUser("firebase-abc", "Name")

        assertEquals(100L, authRepo.getLastPublicNonogramSyncTimestamp())
    }

    @Test
    fun signOut_switchesToAFreshGuestKey() = runTest {
        authRepo.initialize()
        authRepo.linkFirebaseUser("firebase-abc", "Name")

        authRepo.signOut()

        assertEquals(AuthState.GUEST, authRepo.authState.value)
        val guestKey = authRepo.currentUserUid.value!!
        assertTrue(guestKey.startsWith("local:"))
        assertEquals(guestKey, settings.getStringOrNull("current_user_uid"))
        // The signed-in row is left intact, so signing back in restores its puzzles and progress.
        assertNotNull(sdk.getUser("firebase-abc"))
    }

    @Test
    fun signOut_thenLinkAgain_restoresTheOriginalKey() = runTest {
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1)))
        authRepo.initialize()
        authRepo.linkFirebaseUser("firebase-abc", "Name")
        sdk.saveProgressWithTimestamp("firebase-abc", nonogramId, "[[1]]", 100)

        authRepo.signOut()
        authRepo.linkFirebaseUser("firebase-abc", "Name")

        assertEquals(AuthState.SIGNED_IN, authRepo.authState.value)
        assertEquals("firebase-abc", authRepo.currentUserUid.value)
        assertNotNull(sdk.getSingleProgress("firebase-abc", nonogramId))
    }

    @Test
    fun signOut_keepsOnboardingAndSyncCursors() = runTest {
        authRepo.initialize()
        authRepo.linkFirebaseUser("firebase-abc", "Name")
        authRepo.setLastPublicNonogramSyncTimestamp(100)
        authRepo.setLastOwnedNonogramSyncTimestamp("firebase-abc", 200)

        authRepo.signOut()

        assertTrue(authRepo.hasCompletedOnboarding)
        assertEquals(100L, authRepo.getLastPublicNonogramSyncTimestamp())
        assertEquals(200L, authRepo.getLastOwnedNonogramSyncTimestamp("firebase-abc"))
    }
}
