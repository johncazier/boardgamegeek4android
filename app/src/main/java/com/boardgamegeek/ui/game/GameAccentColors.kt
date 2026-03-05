package com.boardgamegeek.ui.game

import android.graphics.Color as AndroidColor
import androidx.annotation.ColorInt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

private const val MIN_ICON_CONTRAST = 3.0

@Composable
fun rememberGameIconTint(@ColorInt gameColor: Int): Color {
    val colorScheme = MaterialTheme.colorScheme
    return remember(gameColor, colorScheme.surface, colorScheme.primary, colorScheme.onSurface) {
        val baseColor = sanitizeGameColor(gameColor, colorScheme.primary)
        ensureContrast(
            foreground = baseColor,
            background = colorScheme.surface,
            minContrast = MIN_ICON_CONTRAST,
            fallback = colorScheme.onSurface
        )
    }
}

@Composable
fun rememberGameFabColors(@ColorInt gameColor: Int): Pair<Color, Color> {
    val colorScheme = MaterialTheme.colorScheme
    return remember(gameColor, colorScheme.surface, colorScheme.primary, colorScheme.onSurface) {
        val baseColor = sanitizeGameColor(gameColor, colorScheme.primary)
        val containerColor = ensureContrast(
            foreground = baseColor,
            background = colorScheme.surface,
            minContrast = MIN_ICON_CONTRAST,
            fallback = colorScheme.primary
        )
        val contentColor = if (contrastRatio(Color.White, containerColor) >= contrastRatio(Color.Black, containerColor)) {
            Color.White
        } else {
            Color.Black
        }
        containerColor to contentColor
    }
}

private fun sanitizeGameColor(@ColorInt gameColor: Int, fallback: Color): Color {
    val isTransparent = gameColor == AndroidColor.TRANSPARENT || (gameColor ushr 24) == 0
    val isNearBlack = (gameColor and 0x00FFFFFF) == 0
    return if (isTransparent || isNearBlack) fallback else Color(gameColor)
}

private fun ensureContrast(
    foreground: Color,
    background: Color,
    minContrast: Double,
    fallback: Color,
): Color {
    if (contrastRatio(foreground, background) >= minContrast) return foreground

    val target = if (background.luminance() < 0.5f) Color.White else Color.Black
    for (step in 1..20) {
        val adjusted = lerp(foreground, target, step / 20f)
        if (contrastRatio(adjusted, background) >= minContrast) return adjusted
    }
    return if (contrastRatio(fallback, background) >= minContrast) fallback else foreground
}

private fun contrastRatio(foreground: Color, background: Color): Double {
    val lighter = maxOf(foreground.luminance(), background.luminance())
    val darker = minOf(foreground.luminance(), background.luminance())
    return ((lighter + 0.05f) / (darker + 0.05f)).toDouble()
}
