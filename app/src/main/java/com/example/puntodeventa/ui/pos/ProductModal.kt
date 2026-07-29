package com.example.puntodeventa.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.puntodeventa.data.local.CustomizationGroupEntity
import com.example.puntodeventa.data.local.CustomizationOptionEntity
import com.example.puntodeventa.data.model.Product
import androidx.compose.material3.MaterialTheme
import java.util.UUID

/**
 * Product detail modal for configuring and adding a product to the cart.
 *
 * Left side displays product emoji, name, and base price.
 * Right side displays customization groups (checkboxes or radio buttons).
 * Bottom section has extra notes field, quantity selector, and action buttons.
 *
 * @param product The product being configured
 * @param customizationGroups List of customization groups for this product
 * @param customizationOptions Map of groupId to list of options for that group
 * @param onAddToCart Callback invoked with the built CartItem when "Agregar"/"Actualizar" is pressed
 * @param onDismiss Callback invoked when "Cancelar" is pressed or modal is dismissed
 * @param editingCartItem When non-null, the modal operates in edit mode pre-filled with this item's data
 */
@Composable
fun ProductModal(
    product: Product,
    customizationGroups: List<CustomizationGroupEntity>,
    customizationOptions: Map<String, List<CustomizationOptionEntity>>,
    onAddToCart: (CartItem) -> Unit,
    onDismiss: () -> Unit,
    editingCartItem: CartItem? = null
) {
    val isEditMode = editingCartItem != null

    // ── Internal state ───────────────────────────────────────────────────────
    var quantity by remember { mutableIntStateOf(editingCartItem?.quantity ?: 1) }
    var extraNotes by remember { mutableStateOf(editingCartItem?.extraNotes ?: "") }

    // For "multiple_checkboxes": track selected option IDs per group
    val checkboxSelections = remember {
        mutableStateMapOf<String, Boolean>().apply {
            if (editingCartItem != null) {
                val selectedOptionIds = editingCartItem.selectedCustomizations.map { it.optionId }.toSet()
                customizationGroups.filter { it.selectionType == "multiple_checkboxes" }.forEach { group ->
                    customizationOptions[group.id].orEmpty().forEach { option ->
                        if (option.id in selectedOptionIds) {
                            put(option.id, true)
                        }
                    }
                }
            }
        }
    }

    // For "single_option": track selected option ID per group
    val radioSelections = remember {
        mutableStateMapOf<String, String>().apply {
            if (editingCartItem != null) {
                val selectedOptionIds = editingCartItem.selectedCustomizations.map { it.optionId }.toSet()
                customizationGroups.filter { it.selectionType == "single_option" }.forEach { group ->
                    customizationOptions[group.id].orEmpty().forEach { option ->
                        if (option.id in selectedOptionIds) {
                            put(group.id, option.id)
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Product info (left side concept in vertical layout) ──────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.emoji,
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = product.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$${String.format("%.2f", product.basePrice)}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Customization groups (right side concept) ────────────────
                if (customizationGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    customizationGroups.forEach { group ->
                        val options = customizationOptions[group.id].orEmpty()

                        Text(
                            text = group.groupName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        when (group.selectionType) {
                            "multiple_checkboxes" -> {
                                options.forEach { option ->
                                    val isChecked = checkboxSelections[option.id] ?: false
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .toggleable(
                                                value = isChecked,
                                                role = Role.Checkbox,
                                                onValueChange = { checked ->
                                                    checkboxSelections[option.id] = checked
                                                }
                                            )
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = buildOptionLabel(option),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            "single_option" -> {
                                val selectedOptionId = radioSelections[group.id]
                                options.forEach { option ->
                                    val isSelected = selectedOptionId == option.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = isSelected,
                                                role = Role.RadioButton,
                                                onClick = {
                                                    radioSelections[group.id] = option.id
                                                }
                                            )
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = buildOptionLabel(option),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Extra notes text field ───────────────────────────────────
                OutlinedTextField(
                    value = extraNotes,
                    onValueChange = { newValue ->
                        if (newValue.length <= 200) {
                            extraNotes = newValue
                        }
                    },
                    label = { Text("Comentario extra") },
                    placeholder = { Text("Agregar nota...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Quantity selector ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { quantity = clampQuantity(quantity, -1) },
                        enabled = quantity > 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("−", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Text(
                        text = quantity.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    Button(
                        onClick = { quantity = clampQuantity(quantity, 1) },
                        enabled = quantity < 99,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("+", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Action buttons ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val selectedCustomizations = buildSelectedCustomizations(
                                customizationGroups = customizationGroups,
                                customizationOptions = customizationOptions,
                                checkboxSelections = checkboxSelections,
                                radioSelections = radioSelections
                            )

                            val extraPrices = selectedCustomizations.map { it.extraPrice }
                            val totalPrice = calculateItemTotal(
                                basePrice = product.basePrice,
                                extraPrices = extraPrices,
                                quantity = quantity
                            )

                            val cartItem = CartItem(
                                id = editingCartItem?.id ?: UUID.randomUUID().toString(),
                                productId = product.id,
                                productName = product.name,
                                emoji = product.emoji,
                                basePrice = product.basePrice,
                                quantity = quantity,
                                selectedCustomizations = selectedCustomizations,
                                extraNotes = extraNotes,
                                totalPrice = totalPrice
                            )

                            onAddToCart(cartItem)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isEditMode) "Actualizar" else "Agregar")
                    }
                }
            }
        }
    }
}

/**
 * Builds the display label for a customization option.
 * Shows "optionName (+$X.XX)" if extraPrice > 0, otherwise just "optionName".
 */
private fun buildOptionLabel(option: CustomizationOptionEntity): String {
    return if (option.extraPrice > 0) {
        "${option.optionName} (+$${String.format("%.2f", option.extraPrice)})"
    } else {
        option.optionName
    }
}

/**
 * Collects all selected customizations from checkbox and radio state maps.
 */
private fun buildSelectedCustomizations(
    customizationGroups: List<CustomizationGroupEntity>,
    customizationOptions: Map<String, List<CustomizationOptionEntity>>,
    checkboxSelections: Map<String, Boolean>,
    radioSelections: Map<String, String>
): List<SelectedCustomization> {
    val selected = mutableListOf<SelectedCustomization>()

    customizationGroups.forEach { group ->
        val options = customizationOptions[group.id].orEmpty()

        when (group.selectionType) {
            "multiple_checkboxes" -> {
                options.forEach { option ->
                    if (checkboxSelections[option.id] == true) {
                        selected.add(
                            SelectedCustomization(
                                optionId = option.id,
                                optionName = option.optionName,
                                extraPrice = option.extraPrice
                            )
                        )
                    }
                }
            }

            "single_option" -> {
                val selectedId = radioSelections[group.id]
                if (selectedId != null) {
                    val option = options.find { it.id == selectedId }
                    if (option != null) {
                        selected.add(
                            SelectedCustomization(
                                optionId = option.id,
                                optionName = option.optionName,
                                extraPrice = option.extraPrice
                            )
                        )
                    }
                }
            }
        }
    }

    return selected
}
