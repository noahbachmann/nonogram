package com.trainpaths.nonogram.screens.viewModel

import androidx.lifecycle.ViewModel
import com.trainpaths.nonogram.ColorTheme
import com.trainpaths.nonogram.theme.ThemeRepository

class ThemeViewModel(private val themeRepository: ThemeRepository) : ViewModel() {
    val theme = themeRepository.theme
    fun selectTheme(theme: ColorTheme) = themeRepository.setTheme(theme)
}
