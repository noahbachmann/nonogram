package com.trainpaths.nonogram

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors: ColorScheme = lightColorScheme(
    primary = Color(0xFF153D36),
    onPrimary = Color.White,
    secondary = Color(0xFFC2EFFF),
    background = Color(0xFF153D36),
    onBackground = Color.White,
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = Colors,
        content = content
    )
}
