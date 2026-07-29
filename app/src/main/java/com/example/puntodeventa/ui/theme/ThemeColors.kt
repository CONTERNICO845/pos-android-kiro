package com.example.puntodeventa.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Maps each [AppTheme] to its full Material3 [ColorScheme].
 *
 * - DEFAULT_GREEN reproduces the legacy AppColorScheme byte-identical (Req 4.1, 4.2).
 * - DARK_NEON uses darkColorScheme for low-light environments (Req 5.1, 5.2, 5.4).
 * - OCEAN_BLUE uses lightColorScheme with royal blue palette (Req 6.1, 6.3).
 * - SUNSET_ORANGE uses lightColorScheme with warm orange palette (Req 7.1, 7.3).
 *
 * The `when` is exhaustive — adding a new AppTheme value will produce a compile-time error (Req 1.3).
 */
fun AppTheme.toColorScheme(): ColorScheme = when (this) {
    AppTheme.DEFAULT_GREEN -> lightColorScheme(
        primary              = Color(0xFF4A8C1C),
        onPrimary            = Color(0xFFFFFFFF),
        primaryContainer     = Color(0xFF2D5A1B),
        onPrimaryContainer   = Color(0xFFFFFFFF),
        secondary            = Color(0xFF4CAF50),
        onSecondary          = Color(0xFFFFFFFF),
        secondaryContainer   = Color(0xFFA5D6A7),
        onSecondaryContainer = Color(0xFF1B5E20),
        tertiary             = Color(0xFFE65100),
        onTertiary           = Color(0xFFFFFFFF),
        tertiaryContainer    = Color(0xFFFFE0B2),
        onTertiaryContainer  = Color(0xFFE65100),
        background           = Color(0xFF6BBF3E),
        onBackground         = Color(0xFFFFFFFF),
        surface              = Color(0xFFFFFFFF),
        onSurface            = Color(0xFF1A1A1A),
        surfaceVariant       = Color(0xFFF5F5F5),
        onSurfaceVariant     = Color(0xFF616161),
        outline              = Color(0xFFE0E0E0),
        error                = Color(0xFFE53935),
        onError              = Color(0xFFFFFFFF),
    )

    AppTheme.DARK_NEON -> darkColorScheme(
        primary              = Color(0xFF39FF14),
        onPrimary            = Color(0xFF003300),
        primaryContainer     = Color(0xFF004D00),
        onPrimaryContainer   = Color(0xFF39FF14),
        secondary            = Color(0xFF00FFFF),
        onSecondary          = Color(0xFF003333),
        secondaryContainer   = Color(0xFF004D4D),
        onSecondaryContainer = Color(0xFF00FFFF),
        tertiary             = Color(0xFFFFAB40),
        onTertiary           = Color(0xFF3E2723),
        tertiaryContainer    = Color(0xFF4E342E),
        onTertiaryContainer  = Color(0xFFFFAB40),
        background           = Color(0xFF121212),
        onBackground         = Color(0xFFE0E0E0),
        surface              = Color(0xFF1E1E1E),
        onSurface            = Color(0xFFE0E0E0),
        surfaceVariant       = Color(0xFF2C2C2C),
        onSurfaceVariant     = Color(0xFFBDBDBD),
        outline              = Color(0xFF424242),
        error                = Color(0xFFE53935),
        onError              = Color(0xFFFFFFFF),
    )

    AppTheme.OCEAN_BLUE -> lightColorScheme(
        primary              = Color(0xFF1565C0),
        onPrimary            = Color(0xFFFFFFFF),
        primaryContainer     = Color(0xFFBBDEFB),
        onPrimaryContainer   = Color(0xFF0D47A1),
        secondary            = Color(0xFF42A5F5),
        onSecondary          = Color(0xFFFFFFFF),
        secondaryContainer   = Color(0xFF90CAF9),
        onSecondaryContainer = Color(0xFF0D47A1),
        tertiary             = Color(0xFFFF6F00),
        onTertiary           = Color(0xFFFFFFFF),
        tertiaryContainer    = Color(0xFFFFE0B2),
        onTertiaryContainer  = Color(0xFFE65100),
        background           = Color(0xFFFFFFFF),
        onBackground         = Color(0xFF1A1A1A),
        surface              = Color(0xFFFFFFFF),
        onSurface            = Color(0xFF1A1A1A),
        surfaceVariant       = Color(0xFFF0F4F8),
        onSurfaceVariant     = Color(0xFF546E7A),
        outline              = Color(0xFFB0BEC5),
        error                = Color(0xFFE53935),
        onError              = Color(0xFFFFFFFF),
    )

    AppTheme.SUNSET_ORANGE -> lightColorScheme(
        primary              = Color(0xFFE65100),
        onPrimary            = Color(0xFFFFFFFF),
        primaryContainer     = Color(0xFFFFF3E0),
        onPrimaryContainer   = Color(0xFFBF360C),
        secondary            = Color(0xFFFFC107),
        onSecondary          = Color(0xFF3E2723),
        secondaryContainer   = Color(0xFFFFE082),
        onSecondaryContainer = Color(0xFF5D4037),
        tertiary             = Color(0xFFD84315),
        onTertiary           = Color(0xFFFFFFFF),
        tertiaryContainer    = Color(0xFFFFCCBC),
        onTertiaryContainer  = Color(0xFFBF360C),
        background           = Color(0xFFFFFBF5),
        onBackground         = Color(0xFF1A1A1A),
        surface              = Color(0xFFFFFFFF),
        onSurface            = Color(0xFF1A1A1A),
        surfaceVariant       = Color(0xFFFFF8E1),
        onSurfaceVariant     = Color(0xFF6D4C41),
        outline              = Color(0xFFD7CCC8),
        error                = Color(0xFFB71C1C),
        onError              = Color(0xFFFFFFFF),
    )
}
