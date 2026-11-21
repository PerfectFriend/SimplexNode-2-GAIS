package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// [REAL] Этап 10: Система переключаемых тем (Matrix / Obsidian / Aurora) с сохранением контрастности всех экранов.

enum class NodeThemeProfile(
    val id: String,
    val displayName: String,
    val tagline: String
) {
    MATRIX("MATRIX", "Matrix", "Cyber Emerald"),
    OBSIDIAN("OBSIDIAN", "Obsidian", "Stealth Amber"),
    AURORA("AURORA", "Aurora", "Arctic Cyan");

    companion object {
        fun fromId(id: String?): NodeThemeProfile {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MATRIX
        }
    }
}

@Immutable
data class ParanoidPalette(
    val profile: NodeThemeProfile,
    val accent: Color,
    val secondaryAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val headerBg: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val warning: Color,
    val danger: Color
)

val MatrixPalette = ParanoidPalette(
    profile = NodeThemeProfile.MATRIX,
    accent = Color(0xFF00FF66),
    secondaryAccent = Color(0xFF00B0FF),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    surfaceElevated = Color(0xFF1E293B),
    headerBg = Color(0xFF0A0F1D),
    border = Color(0xFF1E293B),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFEF4444)
)

val ObsidianPalette = ParanoidPalette(
    profile = NodeThemeProfile.OBSIDIAN,
    accent = Color(0xFFF59E0B),
    secondaryAccent = Color(0xFFE2E8F0),
    background = Color(0xFF09090B),
    surface = Color(0xFF18181B),
    surfaceElevated = Color(0xFF27272A),
    headerBg = Color(0xFF111114),
    border = Color(0xFF27272A),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171)
)

val AuroraPalette = ParanoidPalette(
    profile = NodeThemeProfile.AURORA,
    accent = Color(0xFF38BDF8),
    secondaryAccent = Color(0xFFA78BFA),
    background = Color(0xFF06111E),
    surface = Color(0xFF0E1B2E),
    surfaceElevated = Color(0xFF172942),
    headerBg = Color(0xFF081524),
    border = Color(0xFF1E3A5F),
    textPrimary = Color(0xFFF0F9FF),
    textSecondary = Color(0xFF93C5FD),
    textMuted = Color(0xFF60A5FA),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFFB7185)
)

fun paletteFor(profile: NodeThemeProfile): ParanoidPalette = when (profile) {
    NodeThemeProfile.MATRIX -> MatrixPalette
    NodeThemeProfile.OBSIDIAN -> ObsidianPalette
    NodeThemeProfile.AURORA -> AuroraPalette
}

val LocalParanoidPalette = staticCompositionLocalOf { MatrixPalette }

@Composable
fun MyApplicationTheme(
    themeProfile: NodeThemeProfile = NodeThemeProfile.MATRIX,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(themeProfile)
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        secondary = palette.secondaryAccent,
        tertiary = palette.secondaryAccent,
        background = palette.background,
        surface = palette.surface,
        surfaceVariant = palette.surfaceElevated,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.border,
        error = palette.danger,
        onError = Color.Black
    )

    CompositionLocalProvider(LocalParanoidPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
