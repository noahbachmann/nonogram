package com.trainpaths.nonogram.theme

import com.russhwolf.settings.MapSettings
import com.trainpaths.nonogram.ColorTheme
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeRepositoryTest {

    @Test
    fun noSavedTheme_defaultsToForest() {
        val repo = ThemeRepository(MapSettings())
        assertEquals(ColorTheme.DEFAULT, repo.theme.value)
    }

    @Test
    fun setTheme_persistsAcrossRepositoryInstances() {
        val settings = MapSettings()
        ThemeRepository(settings).setTheme(ColorTheme.FROST)

        val restored = ThemeRepository(settings)
        assertEquals(ColorTheme.FROST, restored.theme.value)
    }

    @Test
    fun garbageStoredKey_fallsBackToDefault() {
        val settings = MapSettings()
        settings.putString("color_theme", "NOT_A_THEME")

        val repo = ThemeRepository(settings)
        assertEquals(ColorTheme.DEFAULT, repo.theme.value)
    }
}
