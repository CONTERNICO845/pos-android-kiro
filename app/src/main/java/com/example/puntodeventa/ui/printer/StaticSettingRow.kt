package com.example.puntodeventa.ui.printer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

/**
 * A reusable read-only row that displays a label on the left and a value on the right.
 *
 * Uses [MaterialTheme.colorScheme.primary] background with [MaterialTheme.colorScheme.onPrimary] text for both elements:
 * - label: [FontWeight.Bold], 16.sp
 * - value: [FontWeight.Normal], 16.sp
 *
 * Requirements: 5.2, 5.3, 5.4, 5.5, 12.7
 */
@Composable
fun StaticSettingRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            color      = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp
        )
        Text(
            text       = value,
            color      = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Normal,
            fontSize   = 16.sp
        )
    }
}
