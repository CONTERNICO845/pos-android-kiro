package com.example.puntodeventa.ui.newproduct

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.puntodeventa.data.local.SelectionType
import androidx.compose.material3.MaterialTheme

/** Human-readable labels for each [SelectionType] value. */
private val selectionTypeLabels = mapOf(
    SelectionType.MULTIPLE_CHECKBOXES to "Casillas (múltiple)",
    SelectionType.SINGLE_OPTION        to "Opción única"
)

/**
 * An [ExposedDropdownMenuBox] that lets the user choose a [SelectionType] for a
 * customization group.
 *
 * Displays two entries:
 * - "Casillas (múltiple)" → [SelectionType.MULTIPLE_CHECKBOXES]
 * - "Opción única"        → [SelectionType.SINGLE_OPTION]
 *
 * Uses [groupFieldColors] for consistent styling inside a [CardBackground] card.
 *
 * Requirements: 7.1, 7.2, 7.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTypeDropdown(
    selectedType: SelectionType,
    onTypeSelected: (SelectionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier         = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value         = selectionTypeLabels[selectedType] ?: "",
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Comportamiento", color = MaterialTheme.colorScheme.onPrimaryContainer) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors        = groupFieldColors(),
            modifier      = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SelectionType.entries.forEach { type ->
                DropdownMenuItem(
                    text    = { Text(selectionTypeLabels[type] ?: type.value) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
