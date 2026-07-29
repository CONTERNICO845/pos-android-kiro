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

    // Premium dark theme: slate canvas, soft indigo primary, teal accent.
    AppTheme.MIDNIGHT_SLATE -> darkColorScheme(
        primary              = Color(0xFF8AB4FF),
        onPrimary            = Color(0xFF06264D),
        primaryContainer     = Color(0xFF26385C),
        onPrimaryContainer   = Color(0xFFD6E3FF),
        secondary            = Color(0xFF7FE0D4),
        onSecondary          = Color(0xFF00352E),
        secondaryContainer   = Color(0xFF14453F),
        onSecondaryContainer = Color(0xFFA6F0E6),
        tertiary             = Color(0xFFFFB59D),
        onTertiary           = Color(0xFF5A1B0A),
        tertiaryContainer    = Color(0xFF5A2A1C),
        onTertiaryContainer  = Color(0xFFFFDBCF),
        background           = Color(0xFF0E1116),
        onBackground         = Color(0xFFE4E7EC),
        surface              = Color(0xFF171B22),
        onSurface            = Color(0xFFE4E7EC),
        surfaceVariant       = Color(0xFF2A2F38),
        onSurfaceVariant     = Color(0xFFC3C8D1),
        outline              = Color(0xFF3A4049),
        error                = Color(0xFFFF6B6B),
        onError              = Color(0xFF3D0000),
    )

    // Premium dark theme: warm charcoal canvas, luxury amber primary, sage accent.
    AppTheme.CHARCOAL_AMBER -> darkColorScheme(
        primary              = Color(0xFFFFCA6B),
        onPrimary            = Color(0xFF3A2A00),
        primaryContainer     = Color(0xFF5C4300),
        onPrimaryContainer   = Color(0xFFFFE4A8),
        secondary            = Color(0xFFF2C1A0),
        onSecondary          = Color(0xFF422A12),
        secondaryContainer   = Color(0xFF5A3D28),
        onSecondaryContainer = Color(0xFFFFDCC4),
        tertiary             = Color(0xFFC7D98F),
        onTertiary           = Color(0xFF2C3400),
        tertiaryContainer    = Color(0xFF404A16),
        onTertiaryContainer  = Color(0xFFE3F3A9),
        background           = Color(0xFF14120E),
        onBackground         = Color(0xFFEDE6DC),
        surface              = Color(0xFF1E1B16),
        onSurface            = Color(0xFFEDE6DC),
        surfaceVariant       = Color(0xFF342F27),
        onSurfaceVariant     = Color(0xFFD2C8B8),
        outline              = Color(0xFF4E483D),
        error                = Color(0xFFFF6B6B),
        onError              = Color(0xFF3D0000),
    )

    // Elegant light theme: soft blush canvas, deep boutique rose primary, warm bronze accent.
    AppTheme.ROSE_QUARTZ -> lightColorScheme(
        primary              = Color(0xFFB0235A),
        onPrimary            = Color(0xFFFFFFFF),
        primaryContainer     = Color(0xFFFFD9E2),
        onPrimaryContainer   = Color(0xFF3E0019),
        secondary            = Color(0xFF8C4A5E),
        onSecondary          = Color(0xFFFFFFFF),
        secondaryContainer   = Color(0xFFFFD9E2),
        onSecondaryContainer = Color(0xFF3A0A1B),
        tertiary             = Color(0xFF8A5A2B),
        onTertiary           = Color(0xFFFFFFFF),
        tertiaryContainer    = Color(0xFFFFDDB8),
        onTertiaryContainer  = Color(0xFF2E1500),
        background           = Color(0xFFFFF8F9),
        onBackground         = Color(0xFF201A1B),
        surface              = Color(0xFFFFFFFF),
        onSurface            = Color(0xFF201A1B),
        surfaceVariant       = Color(0xFFF3E0E4),
        onSurfaceVariant     = Color(0xFF524345),
        outline              = Color(0xFFD8C2C6),
        error                = Color(0xFFB71C1C),
        onError              = Color(0xFFFFFFFF),
    )

    // Professional light theme: cool mint canvas, deep emerald primary, slate-blue accent.
    AppTheme.EMERALD_TEAL -> lightColorScheme(
        primary              = Color(0xFF00695C),
        onPrimary            = Color(0xFFFFFFFF),
        primaryContainer     = Color(0xFFB2DFDB),
        onPrimaryContainer   = Color(0xFF00201C),
        secondary            = Color(0xFF00796B),
        onSecondary          = Color(0xFFFFFFFF),
        secondaryContainer   = Color(0xFFA7F0E4),
        onSecondaryContainer = Color(0xFF00201B),
        tertiary             = Color(0xFF4A6572),
        onTertiary           = Color(0xFFFFFFFF),
        tertiaryContainer    = Color(0xFFCDE7F0),
        onTertiaryContainer  = Color(0xFF051F27),
        background           = Color(0xFFF5FBF9),
        onBackground         = Color(0xFF191C1B),
        surface              = Color(0xFFFFFFFF),
        onSurface            = Color(0xFF191C1B),
        surfaceVariant       = Color(0xFFDBE5E1),
        onSurfaceVariant     = Color(0xFF3F4946),
        outline              = Color(0xFF6F7976),
        error                = Color(0xFFBA1A1A),
        onError              = Color(0xFFFFFFFF),
    )

    // Rich light theme: airy lilac canvas, regal plum primary, rose accent.
    AppTheme.ROYAL_PLUM -> lightColorScheme(
        primary              = Color(0xFF6A1B9A),
        onPrimary            = Color(0xFFFFFFFF),
        primaryContainer     = Color(0xFFEEDCF7),
        onPrimaryContainer   = Color(0xFF2C0A45),
        secondary            = Color(0xFF7B4B9E),
        onSecondary          = Color(0xFFFFFFFF),
        secondaryContainer   = Color(0xFFEEDCF7),
        onSecondaryContainer = Color(0xFF2A0F3D),
        tertiary             = Color(0xFF9C4368),
        onTertiary           = Color(0xFFFFFFFF),
        tertiaryContainer    = Color(0xFFFFD9E4),
        onTertiaryContainer  = Color(0xFF3E0022),
        background           = Color(0xFFFDF8FF),
        onBackground         = Color(0xFF1D1A20),
        surface              = Color(0xFFFFFFFF),
        onSurface            = Color(0xFF1D1A20),
        surfaceVariant       = Color(0xFFE9E0EC),
        onSurfaceVariant     = Color(0xFF4A454E),
        outline              = Color(0xFF7C7580),
        error                = Color(0xFFBA1A1A),
        onError              = Color(0xFFFFFFFF),
    )
}
