package com.trainpaths.nonogram

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors: ColorScheme = lightColorScheme(
    primary = Color(0xFF153D36),
    onPrimary = Color(0xFFEF7F71),
    secondary = Color(0xFFEF7F71),
    tertiary = Color(0xFFFFD700),
    outline = Color(0xFF9A9A9A),
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
