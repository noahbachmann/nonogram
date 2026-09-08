package com.trainpaths.nonogram

import com.trainpaths.nonogram.cache.SEED_PUZZLES
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.classes.PublishStatus
import com.trainpaths.nonogram.util.toDifficulty
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Seeding, on the JVM: the Solver is too slow for the browser test targets, and the cheap invariants that
 * do belong there live in `commonTest/cache/SeedPuzzlesTest`.
 */
class SeedingTest {

    private lateinit var sdk: AppSDK

    @BeforeTest
    fun setUp() {
        sdk = AppSDK(TestDatabaseFactory())
    }

    /** The check the exporter cannot make: it has no access to the Solver. */
    @Test
    fun shippedSeeds_areUniquelySolvable() {
        SEED_PUZZLES.forEach { seed ->
            val puzzle = Nonogram(
                id = seed.id,
                difficulty = seed.difficulty.toDifficulty(),
                solution = seed.solution,
            )
            assertTrue(puzzle.isValid, "seed ${seed.id} (${seed.name}) is not uniquely solvable")
        }
    }

    @Test
    fun seeding_writesApprovedRowsOwnedByNobody() = runTest {
        assertTrue(sdk.seedIfEmpty(SEED_FIXTURE))

        val rows = sdk.getAllNonograms().associateBy { it.id }
        assertEquals(SEED_FIXTURE.map { it.id }.toSet(), rows.keys)
        rows.values.forEach { row ->
            assertEquals(PublishStatus.APPROVED, row.publishStatus)
            assertEquals("", row.authorUid)
        }
        assertEquals("Plus", rows.getValue(SEED_FIXTURE.first().id).name)
        assertEquals(null, rows.getValue(SEED_FIXTURE.last().id).name)
    }

    /** Every launch after the first: the seeds are already there, and nothing they carry is rewritten. */
    @Test
    fun seedIfEmpty_leavesAnAlreadySeededDatabaseAlone() = runTest {
        sdk.seedIfEmpty(SEED_FIXTURE)
        val kept = SEED_FIXTURE.first().id
        val existing = assertNotNull(sdk.getNonogramById(kept))
        sdk.updateNonogram(kept, existing.copy(name = "Renamed"))
        sdk.saveProgress("uid-a", kept, listOf(listOf(1, 0), listOf(0, 1)))

        assertFalse(sdk.seedIfEmpty(SEED_FIXTURE))

        assertEquals("Renamed", sdk.getNonogramById(kept)!!.name)
        assertEquals(SEED_FIXTURE.map { it.id }.toSet(), sdk.getAllNonograms().map { it.id }.toSet())
        val progress = sdk.getProgressForUser("uid-a")
        assertEquals(1, progress.size)
        assertEquals(listOf(listOf(1, 0), listOf(0, 1)), progress.single().board)
    }

    /** Web storage is evictable, so "already seeded" cannot be remembered in a flag: an empty DB re-seeds. */
    @Test
    fun seedIfEmpty_seedsAgainAfterTheDatabaseIsWiped() = runTest {
        sdk.seedIfEmpty(SEED_FIXTURE)

        val wiped = AppSDK(TestDatabaseFactory())

        assertTrue(wiped.seedIfEmpty(SEED_FIXTURE))
        assertEquals(SEED_FIXTURE.size, wiped.getAllNonograms().size)
    }
}
