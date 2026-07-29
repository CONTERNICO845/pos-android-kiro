package com.example.puntodeventa.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Enumeration of all available application themes.
 * Single source of truth for supported visual themes (Req 1.1).
 */
enum class AppTheme {
    DEFAULT_GREEN,
    DARK_NEON,
    OCEAN_BLUE,
    SUNSET_ORANGE,
    MIDNIGHT_SLATE,
    CHARCOAL_AMBER,
    ROSE_QUARTZ,
    EMERALD_TEAL,
    ROYAL_PLUM;

    companion object {
        val DEFAULT = DEFAULT_GREEN

        /**
         * Resolves an [AppTheme] from its [name] string.
         * Returns [DEFAULT] if the name doesn't match any entry (Req 2.5).
         */
        fun fromName(name: String): AppTheme =
            entries.find { it.name == name } ?: DEFAULT
    }
}

/**
 * Spanish display name for the theme selector UI (Req 9.1).
 */
val AppTheme.displayName: String
    get() = when (this) {
        AppTheme.DEFAULT_GREEN  -> "Verde por Defecto"
        AppTheme.DARK_NEON      -> "Neón Oscuro"
        AppTheme.OCEAN_BLUE     -> "Océano Azul"
        AppTheme.SUNSET_ORANGE  -> "Atardecer Naranja"
        AppTheme.MIDNIGHT_SLATE -> "Pizarra Medianoche"
        AppTheme.CHARCOAL_AMBER -> "Carbón Ámbar"
        AppTheme.ROSE_QUARTZ    -> "Cuarzo Rosa"
        AppTheme.EMERALD_TEAL   -> "Esmeralda"
        AppTheme.ROYAL_PLUM     -> "Ciruela Real"
    }

/**
 * Preview colors for theme cards: Triple(primary, background, accent) (Req 9.2).
 */
val AppTheme.previewColors: Triple<Color, Color, Color>
    get() = when (this) {
        AppTheme.DEFAULT_GREEN -> Triple(
            Color(0xFF4A8C1C), // primary
            Color(0xFF6BBF3E), // background
            Color(0xFF4CAF50)  // accent
        )
        AppTheme.DARK_NEON -> Triple(
            Color(0xFF39FF14), // primary
            Color(0xFF121212), // background
            Color(0xFF00FFFF)  // accent
        )
        AppTheme.OCEAN_BLUE -> Triple(
            Color(0xFF1565C0), // primary
            Color(0xFFFFFFFF), // background
            Color(0xFF42A5F5)  // accent
        )
        AppTheme.SUNSET_ORANGE -> Triple(
            Color(0xFFE65100), // primary
            Color(0xFFFFFBF5), // background
            Color(0xFFFFC107)  // accent
        )
        AppTheme.MIDNIGHT_SLATE -> Triple(
            Color(0xFF8AB4FF), // primary
            Color(0xFF0E1116), // background
            Color(0xFF7FE0D4)  // accent
        )
        AppTheme.CHARCOAL_AMBER -> Triple(
            Color(0xFFFFCA6B), // primary
            Color(0xFF14120E), // background
            Color(0xFFC7D98F)  // accent
        )
        AppTheme.ROSE_QUARTZ -> Triple(
            Color(0xFFB0235A), // primary
            Color(0xFFFFF8F9), // background
            Color(0xFF8A5A2B)  // accent
        )
        AppTheme.EMERALD_TEAL -> Triple(
            Color(0xFF00695C), // primary
            Color(0xFFF5FBF9), // background
            Color(0xFF4A6572)  // accent
        )
        AppTheme.ROYAL_PLUM -> Triple(
            Color(0xFF6A1B9A), // primary
            Color(0xFFFDF8FF), // background
            Color(0xFF9C4368)  // accent
        )
    }
