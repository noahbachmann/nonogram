package com.trainpaths.nonogram.sync

import com.trainpaths.nonogram.AppSDK
import com.trainpaths.nonogram.TestDatabaseFactory
import com.trainpaths.nonogram.classes.Difficulty
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.PublishStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The nonogram merge as both platform services run it — the same shape as [RemoteProgressTest], and
 * for the same reason: the policy is common code and the platforms only fetch and write.
 */
class RemoteNonogramsTest {

    private lateinit var sdk: AppSDK
    private lateinit var service: RecordingSyncService

    private val uid = "uid-7"

    @BeforeTest
    fun setUp() {
        sdk = AppSDK(TestDatabaseFactory())
        service = RecordingSyncService()
    }

    private fun remote(
        id: Long,
        updatedAt: Long,
        authorUid: String = uid,
        name: String? = null,
    ) = Nonogram(
        id = id,
        difficulty = Difficulty.EASY,
        solution = listOf(listOf(1, 0), listOf(0, 1)),
        name = name,
        authorUid = authorUid,
        updatedAt = updatedAt,
        publishStatus = PublishStatus.APPROVED,
    )

    @Test
    fun insertsEveryPuzzleTheDeviceHasNeverSeen() = runTest {
        val newest = service.mergeRemoteNonograms(
            sdk,
            uid,
            lastSyncedAt = 0,
            remotes = listOf(remote(1, 100), remote(2, 300), remote(3, 200)),
        )

        assertEquals(300, newest)
        assertEquals(setOf(1L, 2L, 3L), sdk.getAllNonograms().map { it.id }.toSet())
        assertTrue(service.pushedNonograms.isEmpty())
    }

    @Test
    fun remoteNewerOverwritesLocal() = runTest {
        sdk.upsertNonogramFromRemote(remote(1, 100, name = "old"))

        service.mergeRemoteNonograms(sdk, uid, lastSyncedAt = 100, remotes = listOf(remote(1, 200, name = "new")))

        assertEquals("new", assertNotNull(sdk.getNonogramById(1)).name)
        assertTrue(service.pushedNonograms.isEmpty())
    }

    @Test
    fun localNewerAndOwnedIsPushedBackWithItsSolution() = runTest {
        sdk.upsertNonogramFromRemote(remote(1, 300, name = "mine"))

        service.mergeRemoteNonograms(sdk, uid, lastSyncedAt = 0, remotes = listOf(remote(1, 100)))

        assertEquals("mine", assertNotNull(sdk.getNonogramById(1)).name)
        val pushed = service.pushedNonograms.single()
        assertEquals(1, pushed.id)
        assertEquals(listOf(listOf(1, 0), listOf(0, 1)), pushed.solution)
    }

    /** A guest pulls the public stream unauthenticated, and must never push anything back. */
    @Test
    fun aGuestNeverPushesBack() = runTest {
        sdk.upsertNonogramFromRemote(remote(1, 300, name = "mine"))

        service.mergeRemoteNonograms(sdk, firebaseUid = null, lastSyncedAt = 0, remotes = listOf(remote(1, 100)))

        assertTrue(service.pushedNonograms.isEmpty())
    }

    @Test
    fun aPuzzleAnotherUserAuthoredLocallyIsLeftAlone() = runTest {
        sdk.upsertNonogramFromRemote(remote(1, 100, authorUid = "someone-else", name = "theirs"))

        service.mergeRemoteNonograms(sdk, uid, lastSyncedAt = 0, remotes = listOf(remote(1, 200, name = "hijack")))

        assertEquals("theirs", assertNotNull(sdk.getNonogramById(1)).name)
        assertTrue(service.pushedNonograms.isEmpty())
    }

    /** The cursor only ever moves forward, and never past now. */
    @Test
    fun theCursorIgnoresTimestampsFromTheFuture() = runTest {
        val future = 4_102_444_800_000 // 2100-01-01

        val newest = service.mergeRemoteNonograms(
            sdk,
            uid,
            lastSyncedAt = 50,
            remotes = listOf(remote(1, future), remote(2, 120)),
        )

        assertEquals(120, newest)
    }

    @Test
    fun anEmptyPullLeavesTheCursorWhereItWas() = runTest {
        assertEquals(999, service.mergeRemoteNonograms(sdk, uid, lastSyncedAt = 999, remotes = emptyList()))
    }
}
