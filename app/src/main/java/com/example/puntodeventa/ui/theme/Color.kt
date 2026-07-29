package com.example.puntodeventa.ui.theme

import androidx.compose.ui.graphics.Color

// ── Backgrounds ──────────────────────────────────────────────────────────────
val BackgroundPrimary   = Color(0xFF6BBF3E)   // Main screen background (bright green)
val BackgroundSecondary = Color(0xFF5AAD30)   // Subtle variant / gradient end

// ── Navigation Rail ──────────────────────────────────────────────────────────
val NavRailBackground   = Color(0xFFF5F0E8)   // Off-white / cream rail surface
val NavRailIconDefault  = Color(0xFF2D2D2D)   // Unselected icon tint
val NavRailIconSelected = Color(0xFF4A8C1C)   // Selected icon tint (dark green)

// ── Cards ─────────────────────────────────────────────────────────────────────
val CardBackground      = Color(0xFF2D5A1B)   // Dark green menu card surface
val CardText            = Color(0xFFFFFFFF)   // White card label / placeholder text
val CardIconTint        = Color(0xFFFFFFFF)   // White "+" icon tint

// ── Modal / Dialog ───────────────────────────────────────────────────────────
val ModalSurface        = Color(0xFFFFFFFF)   // Dialog background
val ModalTitleText      = Color(0xFF1A1A1A)   // Dialog title color
val ModalBodyText       = Color(0xFF333333)   // Labels inside dialog
val EmojiPickerBorder   = Color(0xFFE0E0E0)   // Emoji cell border (unselected)
val EmojiPickerSelected = Color(0xFF1565C0)   // Emoji cell border (selected, blue)
val SearchBarBorder     = Color(0xFF4A8C1C)   // "BUSCAR EMOJI" bar border

// ── Action Buttons ───────────────────────────────────────────────────────────
val ButtonConfirm       = Color(0xFF4CAF50)   // "GUARDAR CAMBIOS" green button
val ButtonConfirmText   = Color(0xFFFFFFFF)
val ButtonCancel        = Color(0xFFE53935)   // "DESCARTAR CAMBIOS" red button
val ButtonCancelText    = Color(0xFFFFFFFF)
val ButtonDelete        = Color(0xFFB71C1C)   // "ELIMINAR" deep red — edit mode only
val ButtonDeleteText    = Color(0xFFFFFFFF)

// ── Text Input ───────────────────────────────────────────────────────────────
val InputBorder         = Color(0xFF4A8C1C)   // Name text field border
val InputBackground     = Color(0xFFFFFFFF)
val InputText           = Color(0xFF1A1A1A)
val InputHint           = Color(0xFF9E9E9E)   // Placeholder "Tu Nombre Aqui"

// ── Printer Config ──────────────────────────────────────────────────────────
val StatusPanelBackground = Color(0xFFE0E0E0)   // Light gray status panel surface

// ── Checkout Panel (Light Theme) ─────────────────────────────────────────────
val CheckoutBackground    = Color(0xFFFFFFFF)   // Panel background
val CheckoutSectionBg     = Color(0xFFF5F5F5)   // Section backgrounds
val CheckoutChangePanel   = Color(0xFFFFFFFF)   // Change assistant panel — white for max contrast
val CheckoutAlertBg       = Color(0xFFFFE0B2)   // Warm orange highlight for alert box
val ChangeAssistantLabel  = Color(0xFF616161)   // Medium-dark gray for "Total", "Recibido", "Cambio" labels
val ChangeAssistantValue  = Color(0xFF1B5E20)   // Deep green for currency values
val ChangeAlertText       = Color(0xFFE65100)   // Strong orange for alert text
val CoinButtonBg          = Color(0xFFA5D6A7)   // Lighter green for coin buttons
val ButtonGolden          = Color(0xFFFFC107)   // Bright golden for "ready to charge" TOTAL button
val ButtonGoldenText      = Color(0xFF3E2723)   // Dark brown text on golden background
