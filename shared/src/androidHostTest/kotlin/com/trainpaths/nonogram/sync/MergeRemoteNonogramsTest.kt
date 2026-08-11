package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.TestDatabaseFactory
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class MergeRemoteNonogramsTest {

    private lateinit var sdk: AppSDK
    private lateinit var sync: RecordingSyncService

    private val uid = "firebase-uid"
    private val localUserId = 7L

    @BeforeTest
    fun setUp() {
        sdk = AppSDK(TestDatabaseFactory())
        sync = RecordingSyncService()
    }

    private fun nonogram(
        id: Long,
        updatedAt: Long,
        authorId: Long = localUserId,
        solution: List<List<Int>> = listOf(listOf(1, 0)),
    ) = Nonogram(
        id = id, difficulty = Difficulty.EASY, solution = solution,
        authorId = authorId, isPublic = true, updatedAt = updatedAt,
    )

    @Test
    fun noLocalCopy_insertsRemote() = runTest {
        val remote = nonogram(id = 1, updatedAt = 100)

        sync.mergeRemoteNonograms(sdk, uid, localUserId, lastSyncedAt = 0, remotes = listOf(remote))

        assertEquals(remote, sdk.getNonogramById(1))
        assertTrue(sync.pushedNonograms.isEmpty())
    }

    @Test
    fun remoteNewer_overwritesLocal() = runTest {
        val local = nonogram(id = 1, updatedAt = 100)
        sdk.upsertNonogramFromRemote(local)
        val remote = nonogram(id = 1, updatedAt = 200, solution = listOf(listOf(0, 1)))

        sync.mergeRemoteNonograms(sdk, uid, localUserId, lastSyncedAt = 0, remotes = listOf(remote))

        assertEquals(remote, sdk.getNonogramById(1))
        assertTrue(sync.pushedNonograms.isEmpty())
    }

    @Test
    fun localNewerAndLocallyAuthored_pushesLocalBack() = runTest {
        val local = nonogram(id = 1, updatedAt = 200, authorId = localUserId)
        sdk.upsertNonogramFromRemote(local)
        val remote = nonogram(id = 1, updatedAt = 100, solution = listOf(listOf(0, 1)))

        sync.mergeRemoteNonograms(sdk, uid, localUserId, lastSyncedAt = 0, remotes = listOf(remote))

        assertEquals(listOf(uid to local), sync.pushedNonograms)
        assertEquals(local, sdk.getNonogramById(1))
    }

    @Test
    fun localNewerButForeignAuthor_doesNothing() = runTest {
        val local = nonogram(id = 1, updatedAt = 200, authorId = localUserId + 1)
        sdk.upsertNonogramFromRemote(local)
        val remote = nonogram(id = 1, updatedAt = 100, authorId = localUserId + 1)

        sync.mergeRemoteNonograms(sdk, uid, localUserId, lastSyncedAt = 0, remotes = listOf(remote))

        assertTrue(sync.pushedNonograms.isEmpty())
        assertEquals(local, sdk.getNonogramById(1))
    }

    @Test
    fun equalTimestamps_doesNothing() = runTest {
        val local = nonogram(id = 1, updatedAt = 100)
        sdk.upsertNonogramFromRemote(local)
        val remote = nonogram(id = 1, updatedAt = 100, solution = listOf(listOf(0, 1)))

        sync.mergeRemoteNonograms(sdk, uid, localUserId, lastSyncedAt = 0, remotes = listOf(remote))

        assertTrue(sync.pushedNonograms.isEmpty())
        assertEquals(local, sdk.getNonogramById(1))
    }

    @Test
    fun returnsNewestRemoteUpdateTimestamp() = runTest {
        val remotes = listOf(
            nonogram(id = 1, updatedAt = 100),
            nonogram(id = 2, updatedAt = 300),
            nonogram(id = 3, updatedAt = 40),
        )

        val newestReceivedAt = sync.mergeRemoteNonograms(sdk, uid, localUserId, lastSyncedAt = 50, remotes = remotes)

        assertEquals(300, newestReceivedAt)
    }

    @Test
    fun olderRemotes_keepLastSyncTimestamp() = runTest {
        val newestReceivedAt = sync.mergeRemoteNonograms(
            sdk, uid, localUserId, lastSyncedAt = 500,
            remotes = listOf(nonogram(id = 1, updatedAt = 100)),
        )

        assertEquals(500, newestReceivedAt)
    }

    @Test
    fun emptyRemotes_returnLastSyncTimestamp() = runTest {
        val newestReceivedAt = sync.mergeRemoteNonograms(
            sdk, uid, localUserId, lastSyncedAt = 42, remotes = emptyList()
        )

        assertEquals(42, newestReceivedAt)
        assertTrue(sync.pushedNonograms.isEmpty())
    }

    @Test
    fun mixedBatch_handlesEachRemoteIndependently() = runTest {
        val staleLocal = nonogram(id = 1, updatedAt = 100)
        sdk.upsertNonogramFromRemote(staleLocal)
        val freshLocal = nonogram(id = 2, updatedAt = 400, authorId = localUserId)
        sdk.upsertNonogramFromRemote(freshLocal)

        val remoteNewer = nonogram(id = 1, updatedAt = 200, solution = listOf(listOf(0, 1)))
        val remoteStale = nonogram(id = 2, updatedAt = 300)
        val remoteUnknown = nonogram(id = 3, updatedAt = 250)

        val newestReceivedAt = sync.mergeRemoteNonograms(
            sdk, uid, localUserId, lastSyncedAt = 0,
            remotes = listOf(remoteNewer, remoteStale, remoteUnknown),
        )

        assertEquals(remoteNewer, sdk.getNonogramById(1))
        assertEquals(freshLocal, sdk.getNonogramById(2))
        assertEquals(remoteUnknown, sdk.getNonogramById(3))
        assertEquals(listOf(uid to freshLocal), sync.pushedNonograms)
        assertEquals(300, newestReceivedAt)
    }

}

/** Records pushNonogram calls; every other SyncService method is out of scope for the merge policy. */
private class RecordingSyncService : SyncService {
    val pushedNonograms = mutableListOf<Pair<String, Nonogram>>()

    override suspend fun pushNonogram(firebaseUid: String, nonogram: Nonogram) {
        pushedNonograms += firebaseUid to nonogram
    }

    override suspend fun pushProgress(firebaseUid: String, nonogramId: Long, boardState: String?, updatedAt: Long) =
        fail("unexpected pushProgress")

    override suspend fun hasRemoteProgress(firebaseUid: String): Boolean = fail("unexpected hasRemoteProgress")

    override suspend fun uploadAllLocalProgress(firebaseUid: String, localUserId: Long) =
        fail("unexpected uploadAllLocalProgress")

    override suspend fun pullAllProgress(firebaseUid: String, localUserId: Long) = fail("unexpected pullAllProgress")

    override suspend fun pullAndMergeAllProgress(firebaseUid: String, localUserId: Long) =
        fail("unexpected pullAndMergeAllProgress")

    override suspend fun uploadAllLocalNonograms(firebaseUid: String, localUserId: Long) =
        fail("unexpected uploadAllLocalNonograms")

    override suspend fun pullPublicNonogramsSince(
        firebaseUid: String,
        localUserId: Long,
        since: Long,
    ): Long = fail("unexpected pullPublicNonogramsForPuzzleList")

    override suspend fun pullOwnedNonograms(
        firebaseUid: String,
        localUserId: Long,
        since: Long,
    ): Long = fail("unexpected pullOwnedNonogramsForGenerator")
}
