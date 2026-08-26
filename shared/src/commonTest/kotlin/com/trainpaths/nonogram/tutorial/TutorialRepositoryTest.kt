package com.trainpaths.nonogram.tutorial

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TutorialRepositoryTest {

    @Test
    fun freshInstall_nothingSeen() {
        val repo = TutorialRepository(MapSettings())
        assertTrue(repo.seenSteps.value.isEmpty())
    }

    @Test
    fun markSeen_persistsAcrossRepositoryInstances() {
        val settings = MapSettings()
        TutorialRepository(settings).markSeen(TutorialStep.MENU_FILTER)

        val restored = TutorialRepository(settings)
        assertEquals(setOf(TutorialStep.MENU_FILTER), restored.seenSteps.value)
    }

    @Test
    fun markAllSeen_marksEveryStep() {
        val settings = MapSettings()
        TutorialRepository(settings).markAllSeen()

        val restored = TutorialRepository(settings)
        assertEquals(TutorialStep.entries.toSet(), restored.seenSteps.value)
    }

    @Test
    fun resetAll_clearsEverything() {
        val settings = MapSettings()
        val repo = TutorialRepository(settings)
        repo.markAllSeen()
        repo.resetAll()

        assertTrue(repo.seenSteps.value.isEmpty())
        assertTrue(TutorialRepository(settings).seenSteps.value.isEmpty())
    }

    @Test
    fun unrelatedSettingsKeysAreUntouched() {
        val settings = MapSettings()
        settings.putString("color_theme", "FROST")
        val repo = TutorialRepository(settings)
        repo.markAllSeen()
        repo.resetAll()

        assertEquals("FROST", settings.getStringOrNull("color_theme"))
    }

    @Test
    fun markSeen_isIdempotent() {
        val repo = TutorialRepository(MapSettings())
        repo.markSeen(TutorialStep.BOARD_LOCK)
        repo.markSeen(TutorialStep.BOARD_LOCK)

        assertEquals(1, repo.seenSteps.value.size)
        assertFalse(TutorialStep.BOARD_DRAW_MODE in repo.seenSteps.value)
    }
}
