package com.trainpaths.nonogram

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors: ColorScheme = lightColorScheme(
    primary = Color(0xFF153D36),
    onPrimary = Color(0xFFEF7F71),
    secondary = Color(0xFF0E2620),
    onSecondary = Color(0xFFE55D4C),
    tertiary = Color(0xFFD7B400),
    onTertiary = Color(0xFF6DB85C),
    tertiaryFixed = Color(0xFFCE0C0C),
    outline = Color(0xFFF5F5F5),
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
