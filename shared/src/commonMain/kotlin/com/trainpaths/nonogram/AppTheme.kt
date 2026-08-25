package com.trainpaths.nonogram

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Widest the app's content ever gets.
 * Apply as `Modifier.widthIn(max = MAX_CONTENT_WIDTH)` *before* any `fillMax*`,
 * and center from the parent Column's `horizontalAlignment`.
 */
val MAX_CONTENT_WIDTH = 1000.dp

/**
 * Corner rounding for the app's buttons — card-like. Pass as `shape = BUTTON_SHAPE`.
 */
val BUTTON_SHAPE = RoundedCornerShape(12.dp)

private fun colorScheme(
    primary: Color,
    onPrimary: Color,
    secondary: Color,
    onSecondary: Color,
    outline: Color,
    onBackground: Color,
    warning: Color = Color(0xFFD7B400),
    success: Color = Color(0xFF6DB85C),
    error: Color = Color(0xFFCE0C0C),
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    outline = outline,
    onBackground = onBackground,
    background = primary,
    tertiary = warning,
    onTertiary = success,
    tertiaryFixed = error,
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
            primary = Color(0xFF510363),
            onPrimary = Color(0xFF01A79C),
            secondary = Color(0xFF210128),
            onSecondary = Color(0xFF018E85),
            outline = Color(0xFFF5F5F5),
            onBackground = Color.White,
        )
    ),
    PAPER(
        "Paper", colorScheme(
            primary = Color(0xFFC0BC72),
            onPrimary = Color(0xFF02312C),
            secondary = Color(0xFF8D893F),
            onSecondary = Color(0xFF021D1A),
            outline = Color(0xFFD5D5D5),
            onBackground = Color(0xFF021D1A),
            warning = Color(0xFFFFDC42),
            success = Color(0xFF5CFF6F),
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
    );

    companion object {
        val DEFAULT = FOREST
        fun fromKey(key: String?): ColorTheme = entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}

fun Color.darken(fraction: Float): Color = lerp(this, Color.Black, fraction)

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

@Composable
fun switchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSecondary,
    disabledCheckedThumbColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f),
    disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f),

    checkedTrackColor = MaterialTheme.colorScheme.onTertiary,
    uncheckedTrackColor = MaterialTheme.colorScheme.tertiary,
    disabledCheckedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0f),
    disabledUncheckedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0f),

    checkedBorderColor = MaterialTheme.colorScheme.onPrimary,
    uncheckedBorderColor = MaterialTheme.colorScheme.onPrimary,
    disabledCheckedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
    disabledUncheckedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),

    checkedIconColor = MaterialTheme.colorScheme.primary,
    uncheckedIconColor = MaterialTheme.colorScheme.primary,
    disabledCheckedIconColor = MaterialTheme.colorScheme.primary,
    disabledUncheckedIconColor = MaterialTheme.colorScheme.primary,
)
