package com.trainpaths.nonogram

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun colorScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    onSecondary: Color,
    outline: Color,
    onBackground: Color,
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    outline = outline,
    onBackground = onBackground,
    background = primary,
    tertiary = Color(0xFFD7B400),
    onTertiary = Color(0xFF6DB85C),
    tertiaryFixed = Color(0xFFCE0C0C),
)

enum class ColorTheme(val label: String, val scheme: ColorScheme) {
    FOREST(
        "Forest", colorScheme(
            primary = Color(0xFF153D36),
            onPrimary = Color(0xFFEF7F71),
            secondary = Color(0xFF0E2620),
            onSecondary = Color(0xFFE55D4C),
            outline = Color(0xFFF5F5F5),
            onBackground = Color.White,
        )
    ),
    MIDNIGHT(
        "Midnight", colorScheme(
            primary = Color(0xFF10203A),
            onPrimary = Color(0xFF7FB2F0),
            secondary = Color(0xFF0A1526),
            onSecondary = Color(0xFF5C93D6),
            outline = Color(0xFFF5F5F5),
            onBackground = Color.White,
        )
    ),
    PLUM(
        "Plum", colorScheme(
            primary = Color(0xFF2B1B33),
            onPrimary = Color(0xFFE9A6C9),
            secondary = Color(0xFF1C1123),
            onSecondary = Color(0xFFC87FA8),
            outline = Color(0xFFF5F5F5),
            onBackground = Color.White,
        )
    ),
    PAPER(
        "Paper", colorScheme(
            primary = Color(0xFFF2EDE3),
            onPrimary = Color(0xFF2F4F45),
            secondary = Color(0xFFE2D9C8),
            onSecondary = Color(0xFF33302A),
            outline = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1F2421),
        )
    ),
    FROST(
        "Frost", colorScheme(
            primary = Color(0xFFEAF0F5),
            onPrimary = Color(0xFF1F4E79),
            secondary = Color(0xFFD6E2EC),
            onSecondary = Color(0xFF14293D),
            outline = Color(0xFFFFFFFF),
            onBackground = Color(0xFF12212E),
        )
    );

    companion object {
        val DEFAULT = FOREST
        fun fromKey(key: String?): ColorTheme = entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}

@Composable
fun AppTheme(
    theme: ColorTheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = theme.scheme,
        content = content
    )
}
