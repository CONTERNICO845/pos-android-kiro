# Requirements Document

## Introduction

UX/UI Polish Sprint para la aplicación PuntoDeVenta. Este sprint cubre cuatro áreas de mejora de usabilidad: navegación directa desde el Home Screen al POS, lógica inteligente del botón de tijeras (divisor de cuenta), limpieza y ajustes al panel de checkout, y un estado visual "glow" para los botones de completar orden cuando están listos para ser presados.

## Glossary

- **Home_Screen**: The main "Inicio" screen that displays menu item cards (MenuItemCard) and the AddMenuCard in a grid layout. Composable: `HomeScreen.kt`.
- **POS_Screen**: The Point of Sale screen where the cashier selects products, manages the cart, and completes orders. Composable: `PosScreen.kt`.
- **NavController**: The navigation state manager (`currentDestination: NavDestination`) hosted in `MainActivity` that determines which screen is rendered.
- **Menu_Card**: A visual card on the Home_Screen representing a created menu (MenuItem). Composable: `MenuItemCard.kt`.
- **Cart**: The in-memory list of CartItem objects managed by `PosViewModel._cartItems`.
- **Divider**: A special CartItem where `isDivider == true`, rendered as a dashed horizontal line in the Cart to visually separate order groups.
- **Scissors_Button**: The IconButton with `Icons.Default.ContentCut` in the CategoryTabBar that triggers the divider add/remove logic.
- **Checkout_Panel**: The composable (`CheckoutPanel.kt`) that displays customer name, payment status, denomination grid, change assistant, and action buttons.
- **ExactPaymentInput**: The composable containing the "Pago impar/exacto" text field and "Agregar"/"Limpiar" buttons.
- **Limpiar_Button**: The "Limpiar" button that resets cash received state.
- **Completar_Orden_Button**: The main green button in Checkout_Panel that finalizes the order.
- **Total_Button**: The green TOTAL button at the bottom of CartPanel that triggers the checkout flow.
- **isCompletarOrdenEnabled**: A boolean function in PosViewModel that returns true when the order can be completed (cash covers total or "No pagó" is selected, and customer name is not blank).
- **CheckoutState**: Data class holding checkout session state: customerName, paymentStatus, denominationCounts, cashReceived, customAmounts, printAttempts, isPrinting.

## Requirements

### Requirement 1: Navigation from Home Screen to POS

**User Story:** As a cashier, I want to tap a menu card on the Home Screen to navigate directly to the POS screen, so that I can quickly start taking orders without using the navigation rail.

#### Acceptance Criteria

1. WHEN the user taps a Menu_Card on the Home_Screen, THE NavController SHALL set currentDestination to NavDestination.Pos within 300 milliseconds of the tap event.
2. WHEN the user taps a Menu_Card on the Home_Screen, THE POS_Screen SHALL load with the menuId of the tapped Menu_Card applied as the active menu filter, replacing any previously active menuId.
3. THE Home_Screen SHALL render each Menu_Card as a clickable element with Material 3 ripple indication on touch feedback.
4. WHEN the Home_Screen contains zero Menu_Cards, THE Home_Screen SHALL display only the AddMenuCard with no navigation targets.
5. IF the user taps the same Menu_Card that is already the active menu filter while on the POS_Screen, THEN THE NavController SHALL still navigate to the POS_Screen without error and retain the current menuId filter.

### Requirement 2: Smart Scissors Toggle Logic (Divider Spam Prevention)

**User Story:** As a cashier, I want the scissors button to intelligently toggle dividers in the cart, so that I cannot accidentally create consecutive divider lines that clutter the order.

#### Acceptance Criteria

1. WHEN the Scissors_Button is pressed AND the last item in the Cart is a CartItem where isDivider == false, THE PosViewModel SHALL append a new Divider to the end of the Cart.
2. WHEN the Scissors_Button is pressed AND the last item in the Cart is a Divider (isDivider == true), THE PosViewModel SHALL remove that last Divider from the Cart and SHALL NOT append a new Divider.
3. WHEN the Scissors_Button is pressed AND the Cart contains zero items, THE PosViewModel SHALL not modify the Cart.
4. WHEN a Divider item in the Cart is being edited (startEditingItem has been called with that Divider's id) AND the Scissors_Button is pressed, THE PosViewModel SHALL remove that specific Divider from the Cart regardless of its position.
5. WHEN the Scissors_Button is pressed AND the Cart is modified, THE Cart SHALL never contain two consecutive items where isDivider == true.
6. WHEN a Divider is removed via criterion 2 or criterion 4, THE PosViewModel SHALL preserve the order and content of all remaining Cart items unchanged.

### Requirement 3: Remove ExactPaymentInput Component

**User Story:** As a developer, I want to remove the unused "Pago impar (cantidad exacta)" input component from the Checkout_Panel, so that the UI is cleaner and the dead code is eliminated.

#### Acceptance Criteria

1. THE Checkout_Panel SHALL not render the ExactPaymentInput composable or any of its child elements (the "Pago impar/exacto" text field and the "Agregar" button).
2. THE Checkout_Panel SHALL not accept an `onAddCustomAmount` callback parameter in its function signature.
3. THE Checkout_Panel SHALL render the Limpiar_Button as a standalone full-width button positioned between the BillsGrid section and the ChangeAssistant section, retaining its existing `onClearCashReceived` callback and ButtonCancel color styling.
4. THE ExactPaymentInput.kt source file SHALL be deleted from the codebase.

### Requirement 4: "Limpiar" Button Behavior Adjustment

**User Story:** As a cashier, I want the "Limpiar" button to only reset the cash received amount, so that I do not accidentally lose the customer name or payment status I already entered.

#### Acceptance Criteria

1. WHEN the Limpiar_Button is pressed, THE PosViewModel SHALL reset cashReceived to 0.0.
2. WHEN the Limpiar_Button is pressed, THE PosViewModel SHALL reset denominationCounts to an empty map.
3. WHEN the Limpiar_Button is pressed, THE PosViewModel SHALL reset customAmounts to an empty list.
4. WHEN the Limpiar_Button is pressed, THE PosViewModel SHALL preserve the current customerName value unchanged.
5. WHEN the Limpiar_Button is pressed, THE PosViewModel SHALL preserve the current paymentStatus, printAttempts, and isPrinting values unchanged.

### Requirement 5: Checkout Panel Back Button

**User Story:** As a cashier, I want a back arrow button at the top of the Checkout Panel, so that I can quickly return to the POS catalog view without scrolling to find the cancel button.

#### Acceptance Criteria

1. THE Checkout_Panel SHALL display an IconButton with `Icons.Default.ArrowBack` as the first element before the scrollable content area, positioned at the start (left) of the panel with a contentDescription of "Regresar al catálogo".
2. WHEN the back arrow IconButton is pressed, THE PosViewModel SHALL set isCheckoutVisible to false.
3. WHEN the back arrow IconButton is pressed, THE POS_Screen SHALL display the catalog view (CatalogPanel) in place of the Checkout_Panel.
4. WHEN the back arrow IconButton is pressed, THE PosViewModel SHALL preserve all Cart items unchanged.
5. THE back arrow IconButton SHALL remain visible at the top of the Checkout_Panel without requiring the user to scroll, regardless of the scroll position of the panel content.

### Requirement 6: Glow State for Completion Buttons

**User Story:** As a cashier, I want the "Completar Orden" button to visually glow when it becomes enabled, so that I instinctively know the order is ready to be finalized without reading text or checking numbers.

#### Acceptance Criteria

1. WHILE isCompletarOrdenEnabled returns true, THE Completar_Orden_Button SHALL display with full opacity (alpha 1.0) using the ButtonConfirm color as background and an elevation of at least 6dp.
2. WHILE isCompletarOrdenEnabled returns false, THE Completar_Orden_Button SHALL display with reduced opacity (alpha 0.38) and a default elevation of 0dp.
3. WHILE isCompletarOrdenEnabled returns true, THE Total_Button in CartPanel SHALL display with full opacity (alpha 1.0) using the ButtonConfirm color as background and an elevation of at least 6dp, matching the Completar_Orden_Button enabled treatment.
4. WHILE isCompletarOrdenEnabled returns false, THE Total_Button in CartPanel SHALL display with reduced opacity (alpha 0.38) and a default elevation of 0dp.
5. THE glow visual treatment SHALL use color values defined in `Color.kt` and not hardcode color literals in the composable.
6. WHEN isCompletarOrdenEnabled transitions from false to true, THE Completar_Orden_Button and Total_Button SHALL update their visual state without requiring user interaction.
