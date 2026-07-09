package com.example.puntodeventa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// All colors come from Color.kt — dynamicColor is intentionally disabled
// so the brand green palette always shows (even on Android 12+).
private val AppColorScheme = lightColorScheme(
    primary            = NavRailIconSelected,
    onPrimary          = CardText,
    primaryContainer   = CardBackground,
    onPrimaryContainer = CardText,
    secondary          = ButtonConfirm,
    onSecondary        = ButtonConfirmText,
    background         = BackgroundPrimary,
    onBackground       = CardText,
    surface            = ModalSurface,
    onSurface          = ModalTitleText,
    error              = ButtonCancel,
    onError            = ButtonCancelText,
)

@Composable
fun PuntoDeVentaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}
