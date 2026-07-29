package com.example.puntodeventa.ui.printer

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
import com.example.puntodeventa.ui.theme.ModalBodyText

/**
 * A read-only row used inside [StatusPanel] to display a printer specification
 * as a label-value pair.
 *
 * Both texts use [ModalBodyText] color:
 * - label: [FontWeight.Bold]
 * - value: [FontWeight.Normal]
 *
 * Requirements: 8.6, 8.7
 */
@Composable
fun StatusInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            color      = ModalBodyText,
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp
        )
        Text(
            text       = value,
            color      = ModalBodyText,
            fontWeight = FontWeight.Normal,
            fontSize   = 16.sp
        )
    }
}
