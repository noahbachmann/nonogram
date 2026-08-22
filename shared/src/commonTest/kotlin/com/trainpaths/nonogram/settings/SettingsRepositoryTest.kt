package com.trainpaths.nonogram.settings

import com.russhwolf.settings.MapSettings
import com.trainpaths.nonogram.ColorTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsRepositoryTest {

    @Test
    fun noSavedTheme_defaultsToForest() {
        val repo = SettingsRepository(MapSettings())
        assertEquals(ColorTheme.DEFAULT, repo.theme.value)
    }

    @Test
    fun setTheme_persistsAcrossRepositoryInstances() {
        val settings = MapSettings()
        SettingsRepository(settings).setTheme(ColorTheme.FROST)

        val restored = SettingsRepository(settings)
        assertEquals(ColorTheme.FROST, restored.theme.value)
    }

    @Test
    fun garbageStoredKey_fallsBackToDefault() {
        val settings = MapSettings()
        settings.putString("color_theme", "NOT_A_THEME")

        val repo = SettingsRepository(settings)
        assertEquals(ColorTheme.DEFAULT, repo.theme.value)
    }

    @Test
    fun noSavedPreference_showAllNamesDefaultsToTrue() {
        val repo = SettingsRepository(MapSettings())
        assertTrue(repo.showNames.value)
    }

    @Test
    fun setShowAllNames_persistsAcrossRepositoryInstances() {
        val settings = MapSettings()
        SettingsRepository(settings).setShowAllNames(false)

        val restored = SettingsRepository(settings)
        assertEquals(false, restored.showNames.value)
        assertEquals(ColorTheme.DEFAULT, restored.theme.value)
    }
}
