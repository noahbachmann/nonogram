package com.trainpaths.nonogram.theme

import com.russhwolf.settings.Settings
import com.trainpaths.nonogram.ColorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val KEY_THEME = "color_theme"

class ThemeRepository(private val settings: Settings) {

    private val _theme = MutableStateFlow(ColorTheme.fromKey(settings.getStringOrNull(KEY_THEME)))
    val theme: StateFlow<ColorTheme> = _theme.asStateFlow()

    fun setTheme(theme: ColorTheme) {
        if (_theme.value == theme) return
        _theme.value = theme
        settings.putString(KEY_THEME, theme.name)
    }
}
