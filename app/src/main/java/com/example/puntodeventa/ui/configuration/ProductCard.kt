package com.example.puntodeventa.ui.configuration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puntodeventa.data.model.Product
import androidx.compose.material3.MaterialTheme

@Composable
fun ProductCard(
    product: Product,
    isMenuExpanded: Boolean,
    onToggleActive: (Product) -> Unit,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onEditar: (Product) -> Unit,
    onDuplicar: (Product) -> Unit,
    onEliminar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Emoji
            Text(
                text = product.emoji,
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // 2. Name + price column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = formatPrice(product.basePrice),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }

            // 3. Switch
            Switch(
                checked = product.isActive,
                onCheckedChange = { onToggleActive(product) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    uncheckedTrackColor = MaterialTheme.colorScheme.error
                )
            )

            // 4. Settings icon button + anchored dropdown menu
            Box {
                IconButton(onClick = onMenuOpen) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Opciones de ${product.name}",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                ProductActionMenu(
                    expanded = isMenuExpanded,
                    onDismiss = onMenuDismiss,
                    onEditar = { onEditar(product) },
                    onDuplicar = { onDuplicar(product) },
                    onEliminar = { onEliminar(product.id) }
                )
            }
        }
    }
}

@Composable
private fun ProductActionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEditar: () -> Unit,
    onDuplicar: () -> Unit,
    onEliminar: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Editar") }, onClick = { onEditar(); onDismiss() })
        DropdownMenuItem(text = { Text("Duplicar") }, onClick = { onDuplicar(); onDismiss() })
        DropdownMenuItem(
            text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
            onClick = { onEliminar(); onDismiss() }
        )
    }
}

internal fun formatPrice(price: Double): String =
    "$" + "%.2f".format(price)
