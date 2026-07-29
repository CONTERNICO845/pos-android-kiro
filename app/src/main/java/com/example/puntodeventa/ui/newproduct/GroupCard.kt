package com.example.puntodeventa.ui.newproduct

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.puntodeventa.data.local.SelectionType
import androidx.compose.material3.MaterialTheme

/**
 * Returns [OutlinedTextFieldDefaults.colors] configured for the group/option fields
 * inside a [GroupCard] (white-on-dark-green styling matching the card surface).
 *
 * Shared by [GroupCard], [OptionRow], and [SelectionTypeDropdown].
 */
@Composable
fun groupFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = MaterialTheme.colorScheme.onPrimaryContainer,
    unfocusedBorderColor    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
    cursorColor             = MaterialTheme.colorScheme.onPrimaryContainer,
    focusedLabelColor       = MaterialTheme.colorScheme.onPrimaryContainer,
    unfocusedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
    focusedTextColor        = MaterialTheme.colorScheme.onPrimaryContainer,
    unfocusedTextColor      = MaterialTheme.colorScheme.onPrimaryContainer,
    errorBorderColor        = MaterialTheme.colorScheme.primary,
    errorLabelColor         = MaterialTheme.colorScheme.primary,
    errorTextColor          = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor   = MaterialTheme.colorScheme.primaryContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    errorContainerColor     = MaterialTheme.colorScheme.primaryContainer
)

/**
 * A card that represents one customization [GroupDraft] inside the New Product Modal.
 *
 * Contains:
 * - Group name [OutlinedTextField] (max 120 chars, shows [GroupDraft.groupNameError])
 * - [SelectionTypeDropdown] for the group's selection behaviour
 * - Trash [IconButton] to remove the group
 * - List of [OptionRow]s, each keyed by [OptionDraft.draftId]
 * - "+ Agregar opción" [TextButton]
 *
 * Requirements: 6.5, 7.1, 8.1, 10.2
 */
@Composable
fun GroupCard(
    group: GroupDraft,
    groupIndex: Int,
    onGroupNameChange: (String) -> Unit,
    onSelectionTypeChange: (SelectionType) -> Unit,
    onRemoveGroup: () -> Unit,
    onAddOption: () -> Unit,
    onOptionNameChange: (Int, String) -> Unit,
    onOptionPriceChange: (Int, String) -> Unit,
    onRemoveOption: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ── Header row: group name field + trash button ──────────────────
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = group.groupName,
                    onValueChange = { if (it.length <= 120) onGroupNameChange(it) },
                    label = { Text("Nombre del grupo") },
                    singleLine = true,
                    isError = group.groupNameError != null,
                    supportingText = group.groupNameError?.let { err ->
                        { Text(text = err, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    },
                    colors = groupFieldColors(),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onRemoveGroup,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar grupo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Selection type dropdown ──────────────────────────────────────
            SelectionTypeDropdown(
                selectedType = group.selectionType,
                onTypeSelected = onSelectionTypeChange,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Option rows, keyed by stable draftId (Req 10.2) ─────────────
            group.options.forEachIndexed { optionIndex, option ->
                key(option.draftId) {
                    OptionRow(
                        option = option,
                        onNameChange = { onOptionNameChange(optionIndex, it) },
                        onPriceChange = { onOptionPriceChange(optionIndex, it) },
                        onRemove = { onRemoveOption(optionIndex) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── Add option button ────────────────────────────────────────────
            TextButton(
                onClick = onAddOption,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = "+ Agregar opción",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * A single option row inside a [GroupCard].
 *
 * Contains:
 * - Option name [OutlinedTextField] (max 120 chars, shows [OptionDraft.optionNameError])
 * - Extra price [OutlinedTextField] (Decimal keyboard, width = 120.dp, shows [OptionDraft.optionPriceError])
 * - "X" [IconButton] to remove this option
 *
 * Requirements: 8.1
 */
@Composable
private fun OptionRow(
    option: OptionDraft,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth()
    ) {
        // Option name field
        OutlinedTextField(
            value = option.optionName,
            onValueChange = { if (it.length <= 120) onNameChange(it) },
            label = { Text("Nombre de la opción") },
            singleLine = true,
            isError = option.optionNameError != null,
            supportingText = option.optionNameError?.let { err ->
                { Text(text = err, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            },
            colors = groupFieldColors(),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Extra price field (fixed width)
        OutlinedTextField(
            value = option.extraPriceText,
            onValueChange = onPriceChange,
            label = { Text("Precio extra ($)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = option.optionPriceError != null,
            supportingText = option.optionPriceError?.let { err ->
                { Text(text = err, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            },
            colors = groupFieldColors(),
            modifier = Modifier.width(120.dp)
        )

        // Remove option button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Eliminar opción",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
