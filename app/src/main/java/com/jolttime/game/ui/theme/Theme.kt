package com.jolttime.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class JoltColors(
    val background: Color = Color(0xFF0E1015),
    val surface: Color = Color(0xFF181B22),
    val surfaceElevated: Color = Color(0xFF252A34),
    val textPrimary: Color = Color(0xFFF4F4F5),
    val textSecondary: Color = Color(0xFFAEB4C0),
    val textMuted: Color = Color(0xFF858C99),
    val goldAccent: Color = Color(0xFFE5B95C),
    val temporalBlue: Color = Color(0xFF5AC8E8),
    val danger: Color = Color(0xFFE85D58),
    val success: Color = Color(0xFF64D28B),
    val sand: Color = Color(0xFFD39A55),
    val nile: Color = Color(0xFF268AA4),
)

val LocalJoltColors = staticCompositionLocalOf { JoltColors() }
val JoltPalette: JoltColors @Composable get() = LocalJoltColors.current

// Compatibility aliases for existing UI while all new visuals use semantic colors.
val Ink = JoltColors().background
val Card = JoltColors().surface
val Gold = JoltColors().goldAccent
val Text = JoltColors().textPrimary
val Muted = JoltColors().textSecondary

private val colors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF17120A),
    background = Ink,
    onBackground = Text,
    surface = Card,
    onSurface = Text,
    surfaceVariant = JoltColors().surfaceElevated,
    onSurfaceVariant = JoltColors().textSecondary,
    outline = Color(0xFF434A57),
    secondary = JoltColors().temporalBlue,
    error = JoltColors().danger,
)

@Composable
fun JoltTheme(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalJoltColors provides JoltColors()) {
        MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
    }
}
