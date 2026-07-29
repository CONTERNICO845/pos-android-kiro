package com.example.puntodeventa.ui.pos

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

/**
 * The cart panel displaying the current in-memory order items and a total button.
 *
 * This panel occupies the right portion of the POS screen (width allocation handled by parent).
 * It shows a scrollable list of cart items with swipe-to-delete support and a green bottom
 * button displaying the running total.
 *
 * Satisfies Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7
 */
@Composable
fun CartPanel(
    cartItems: List<CartItem>,
    cartTotal: Double,
    onRemoveItem: (String) -> Unit,
    onItemClick: (CartItem) -> Unit,
    onCompleteOrder: () -> Unit,
    isCartEmpty: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            items(
                items = cartItems,
                key = { it.id }
            ) { cartItem ->
                if (cartItem.isDivider) {
                    CartDividerRow()
                } else {
                    CartItemRow(
                        cartItem = cartItem,
                        onRemove = { onRemoveItem(cartItem.id) },
                        onClick = { onItemClick(cartItem) }
                    )
                }
            }
        }

        Button(
            onClick = onCompleteOrder,
            enabled = !isCartEmpty,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "TOTAL: $${String.format("%.2f", cartTotal)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Renders a full-width horizontal dashed line for divider items in the cart.
 * No product name, price, quantity, emoji, or controls are displayed.
 *
 * Satisfies Requirements: 3.2
 */
@Composable
private fun CartDividerRow() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        drawLine(
            color = Color.Gray,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 2f,
            pathEffect = dashPathEffect
        )
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemove()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Color(0xFFE53935)
                } else {
                    Color.Transparent
                },
                label = "dismissBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.White
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClickLabel = "Editar artículo", onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: quantity, name, and customizations
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${cartItem.quantity}x ${cartItem.productName}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (cartItem.selectedCustomizations.isNotEmpty()) {
                    Text(
                        text = cartItem.selectedCustomizations.joinToString(", ") { it.optionName },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: row total price
            Text(
                text = "$${String.format("%.2f", cartItem.totalPrice)}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
