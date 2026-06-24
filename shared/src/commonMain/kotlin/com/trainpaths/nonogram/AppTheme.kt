package com.trainpaths.nonogram

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Colors: ColorScheme = lightColorScheme(
    primary = Color(0xFF153D36),
    secondary = Color(0xFFC2EFFF),
    error = Color.White,
)

private val AppTypography: Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
)

@Composable
fun AppTheme(
    colorScheme: ColorScheme = Colors,
    typography: Typography = AppTypography,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}