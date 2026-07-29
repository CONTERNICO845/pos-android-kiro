package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

/**
 * Checkout panel composable with full "Calculator/Assistant" layout.
 * Uses a light mode theme (white background) and organizes sections top-to-bottom:
 * Customer name → PaymentStatusPills → TotalDisplay → BillsGrid →
 * Limpiar button → ChangeAssistant → "Completar Orden" button → "Cancelar" button.
 *
 * Satisfies Requirements: 6.1, 6.2, 7.1, 7.2, 7.3, 11.1, 11.2, 11.3, 11.4, 11.5
 */
@Composable
fun CheckoutPanel(
    checkoutState: CheckoutState,
    cartTotal: Double,
    isCompletarEnabled: Boolean,
    onCustomerNameChange: (String) -> Unit,
    onPaymentStatusSelected: (PaymentStatus) -> Unit,
    onDenominationPressed: (Int) -> Unit,
    onClearCashReceived: () -> Unit,
    onCompletarOrden: () -> Unit,
    onCancelar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Back button - always visible, not scrollable
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Regresar al catálogo"
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Customer name text field
            OutlinedTextField(
            value = checkoutState.customerName,
            onValueChange = { input ->
                onCustomerNameChange(input.take(40))
            },
            label = { Text("Nombre del cliente (obligatorio)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Payment status pills
        PaymentStatusPills(
            selectedStatus = checkoutState.paymentStatus,
            onStatusSelected = onPaymentStatusSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Total display section
        TotalDisplay(cartTotal = cartTotal)

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Bills grid
        BillsGrid(
            denominationCounts = checkoutState.denominationCounts,
            onDenominationPressed = onDenominationPressed
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Limpiar button (standalone, full-width)
        Button(
            onClick = onClearCashReceived,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Limpiar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Change assistant
        ChangeAssistant(
            cartTotal = cartTotal,
            cashReceived = checkoutState.cashReceived
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 7. "Completar Orden" button
        Button(
            onClick = onCompletarOrden,
            enabled = isCompletarEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.secondary,
                disabledContentColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .glowWhenEnabled(isCompletarEnabled)
        ) {
            Text(
                text = "Completar Orden",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 8. "Cancelar" button
        Button(
            onClick = onCancelar,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cancelar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        }
    }
}

/**
 * Displays "Total a cobrar" label in medium gray and the cart total
 * in bold 32.sp with "$" prefix and 2 decimal places, both centered.
 *
 * Satisfies Requirements: 7.1, 7.2, 7.3
 */
@Composable
private fun TotalDisplay(
    cartTotal: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Total a cobrar",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$${String.format("%,.2f", cartTotal)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Row of 3 pill-shaped mutually exclusive payment status buttons with equal width.
 * Selected pill: ButtonConfirm (green) background + white text.
 * Unselected pills: outlined with InputBorder color + default text.
 * Default selection: "Pagado" (determined by CheckoutState default).
 *
 * Satisfies Requirements: 6.3, 6.4, 6.5
 */
@Composable
fun PaymentStatusPills(
    selectedStatus: PaymentStatus,
    onStatusSelected: (PaymentStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(50) // Fully rounded corners (pill shape)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PaymentStatus.entries.forEach { status ->
            val isSelected = status == selectedStatus

            if (isSelected) {
                Button(
                    onClick = { onStatusSelected(status) },
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = status.displayText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onStatusSelected(status) },
                    shape = pillShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = status.displayText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
