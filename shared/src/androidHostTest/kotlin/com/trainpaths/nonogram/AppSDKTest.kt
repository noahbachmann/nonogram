package com.trainpaths.nonogram

import com.trainpaths.nonogram.classes.Difficulty
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppSDKTest {

    private lateinit var sdk: AppSDK

    @BeforeTest
    fun setUp() {
        sdk = AppSDK(TestDatabaseFactory())
    }

    @Test
    fun seedIfEmpty_insertsThreeNonograms() {
        assertTrue(sdk.getAllNonograms().isEmpty())
        sdk.seedIfEmpty()
        assertEquals(3, sdk.getAllNonograms().size)
    }

    @Test
    fun seedIfEmpty_doesNotDuplicateOnSecondCall() {
        sdk.seedIfEmpty()
        sdk.seedIfEmpty()
        assertEquals(3, sdk.getAllNonograms().size)
    }

    @Test
    fun addNonogram_and_getAllNonograms_roundTrip() {
        val solution = listOf(listOf(1, 0), listOf(0, 1))
        val id = sdk.addNonogram("EASY", solution)
        val all = sdk.getAllNonograms()
        assertEquals(1, all.size)
        assertEquals(id, all[0].id)
        assertEquals(Difficulty.EASY, all[0].difficulty)
        assertEquals(solution, all[0].solution)
    }

    @Test
    fun getNonogramsByDifficulty_filtersCorrectly() {
        sdk.addNonogram("EASY", listOf(listOf(1)))
        sdk.addNonogram("HARD", listOf(listOf(0)))
        sdk.addNonogram("EASY", listOf(listOf(1, 1)))

        val easy = sdk.getNonogramsByDifficulty("EASY")
        assertEquals(2, easy.size)
        assertTrue(easy.all { it.difficulty == Difficulty.EASY })

        val hard = sdk.getNonogramsByDifficulty("HARD")
        assertEquals(1, hard.size)
    }

    @Test
    fun getNonogramById_returnsCorrectOrNull() {
        val id = sdk.addNonogram("EASY", listOf(listOf(1)))
        assertNotNull(sdk.getNonogramById(id))
        assertNull(sdk.getNonogramById(9999))
    }

    @Test
    fun addUser_and_getUserById_roundTrip() {
        val userId = sdk.addUser("Alice")
        val user = sdk.getUserById(userId)
        assertNotNull(user)
        assertEquals("Alice", user.name)
        assertNull(user.firebaseUid)
    }

    @Test
    fun getUserByFirebaseUid_returnsNullInitially() {
        sdk.addUser("Bob")
        assertNull(sdk.getUserByFirebaseUid("firebase-123"))
    }

    @Test
    fun updateUserFirebaseUid_thenFindByUid() {
        val userId = sdk.addUser("Charlie")
        sdk.updateUserFirebaseUid(userId, "firebase-456", "Charlie Updated")
        val found = sdk.getUserByFirebaseUid("firebase-456")
        assertNotNull(found)
        assertEquals(userId, found.id)
        assertEquals("Charlie Updated", found.name)
    }

    @Test
    fun saveProgress_and_getProgressForUser_roundTrip() {
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1, 0), listOf(0, 1)))
        val userId = sdk.addUser("Dave")
        val board = listOf(listOf(1, 0), listOf(0, 0))

        sdk.saveProgress(userId, nonogramId, board)

        val progress = sdk.getProgressForUser(userId)
        assertEquals(1, progress.size)
        assertEquals(nonogramId, progress[0].nonogram.id)
        assertEquals(board, progress[0].board)
    }

    @Test
    fun incrementBeat_incrementsCounter() {
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1)))
        val userId = sdk.addUser("Eve")

        sdk.saveProgress(userId, nonogramId, listOf(listOf(1)))
        sdk.incrementBeat(userId, nonogramId)

        val progress = sdk.getProgressForUser(userId)
        assertEquals(1, progress[0].beat)

        sdk.incrementBeat(userId, nonogramId)
        val progress2 = sdk.getProgressForUser(userId)
        assertEquals(2, progress2[0].beat)
    }

    @Test
    fun upsertProgress_preservesBeatCount() {
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1)))
        val userId = sdk.addUser("Frank")

        sdk.saveProgress(userId, nonogramId, listOf(listOf(1)))
        sdk.incrementBeat(userId, nonogramId)
        sdk.incrementBeat(userId, nonogramId)

        sdk.saveProgress(userId, nonogramId, listOf(listOf(0)))

        val progress = sdk.getProgressForUser(userId)
        assertEquals(2, progress[0].beat)
        assertEquals(listOf(listOf(0)), progress[0].board)
    }

    @Test
    fun saveProgressWithTimestamp_and_getSingleProgress_roundTrip() {
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1)))
        val userId = sdk.addUser("Grace")
        val timestamp = 1700000000000L

        sdk.saveProgressWithTimestamp(userId, nonogramId, "[[1]]", timestamp)

        val single = sdk.getSingleProgress(userId, nonogramId)
        assertNotNull(single)
        assertEquals("[[1]]", single.boardState)
        assertEquals(timestamp, single.updatedAt)
    }

    @Test
    fun getProgressForUserWithTimestamp_returnsAllProgress() {
        val id1 = sdk.addNonogram("EASY", listOf(listOf(1)))
        val id2 = sdk.addNonogram("HARD", listOf(listOf(0)))
        val userId = sdk.addUser("Heidi")

        sdk.saveProgress(userId, id1, listOf(listOf(1)))
        sdk.saveProgress(userId, id2, listOf(listOf(0)))

        val progress = sdk.getProgressForUserWithTimestamp(userId)
        assertEquals(2, progress.size)
    }

    @Test
    fun saveProgress_nullBoard() {
        val nonogramId = sdk.addNonogram("EASY", listOf(listOf(1)))
        val userId = sdk.addUser("Ivan")

        sdk.saveProgress(userId, nonogramId, null)

        val single = sdk.getSingleProgress(userId, nonogramId)
        assertNotNull(single)
        assertNull(single.boardState)
    }
}
