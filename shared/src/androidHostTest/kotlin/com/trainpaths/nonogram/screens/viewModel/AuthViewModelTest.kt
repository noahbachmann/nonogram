package com.trainpaths.nonogram.screens.viewModel

import com.russhwolf.settings.MapSettings
import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.TestDatabaseFactory
import com.trainpaths.nonogram.auth.AuthRepository
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.sync.SyncService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val uid = "firebase-uid"
    private lateinit var sdk: AppSDK
    private lateinit var authRepository: AuthRepository
    private lateinit var syncService: QueuedSyncService
    private lateinit var viewModel: AuthViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sdk = AppSDK(TestDatabaseFactory())
        authRepository = AuthRepository(sdk, MapSettings())
        syncService = QueuedSyncService()
        viewModel = AuthViewModel(authRepository, syncService, sdk)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun menuSync_updatesPublicAndOwnedTimestamps() = runBlocking {
        signIn()
        syncService.publicResults += 100
        syncService.ownedResults += 200

        awaitMenuSync()

        assertEquals(100, authRepository.getLastPublicNonogramSyncTimestamp(uid))
        assertEquals(200, authRepository.getLastOwnedNonogramSyncTimestamp(uid))
        assertEquals(GeneratorSyncState.IDLE, viewModel.generatorNonogramSyncState.value)
    }

    @Test
    fun failedOwnedPull_showsGeneratorErrorAndKeepsLastTimestamp() = runBlocking {
        signIn()
        syncService.publicResults += 100
        syncService.ownedResults.add(null)

        awaitMenuSync()

        assertEquals(GeneratorSyncState.ERROR, viewModel.generatorNonogramSyncState.value)
        assertEquals(0, authRepository.getLastOwnedNonogramSyncTimestamp(uid))
    }

    @Test
    fun ownedRetry_clearsErrorAndUpdatesTimestamp() = runBlocking {
        signIn()
        syncService.publicResults += 100
        syncService.ownedResults.add(null)
        awaitMenuSync()
        syncService.ownedResults += 250

        awaitOwnedRetry()

        assertEquals(GeneratorSyncState.IDLE, viewModel.generatorNonogramSyncState.value)
        assertEquals(250, authRepository.getLastOwnedNonogramSyncTimestamp(uid))
    }

    private suspend fun signIn() {
        authRepository.initialize()
        authRepository.linkFirebaseUser(uid, "User")
    }

    private suspend fun awaitMenuSync() {
        awaitCompletion { onComplete ->
            viewModel.syncNonograms(onComplete)
        }
    }

    private suspend fun awaitOwnedRetry() {
        awaitCompletion { onComplete ->
            viewModel.retryOwnNonograms(onComplete)
        }
    }

    private suspend fun awaitCompletion(start: (() -> Unit) -> Unit) {
        val completed = CompletableDeferred<Unit>()
        start { completed.complete(Unit) }
        withTimeout(5.seconds) { completed.await() }
    }
}

private class QueuedSyncService : SyncService {
    val publicResults = mutableListOf<Long?>()
    val ownedResults = mutableListOf<Long?>()

    override suspend fun pullPublicNonogramsSince(
        firebaseUid: String,
        localUserId: Long,
        since: Long,
    ): Long? = publicResults.removeAt(0)

    override suspend fun pullOwnedNonograms(
        firebaseUid: String,
        localUserId: Long,
        since: Long,
    ): Long? = ownedResults.removeAt(0)

    override suspend fun pushProgress(
        firebaseUid: String,
        nonogramId: Long,
        boardState: String?,
        updatedAt: Long,
    ) = Unit

    override suspend fun hasRemoteProgress(firebaseUid: String): Boolean = false

    override suspend fun uploadAllLocalProgress(firebaseUid: String, localUserId: Long) = Unit

    override suspend fun pullAllProgress(firebaseUid: String, localUserId: Long) = Unit

    override suspend fun pullAndMergeAllProgress(firebaseUid: String, localUserId: Long) = Unit

    override suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram) = Unit

    override suspend fun uploadAllLocalNonograms(firebaseUid: String, localUserId: Long) = Unit
}
