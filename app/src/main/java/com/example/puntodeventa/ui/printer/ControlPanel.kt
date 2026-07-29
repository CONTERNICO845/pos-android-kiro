package com.example.puntodeventa.ui.printer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

/**
 * Left-column control panel for the printer configuration screen.
 *
 * Displays:
 *  - Header: "IMPRESORA" title and "POS-8360 LAN" subtitle (Task 4.1)
 *  - IP address input field with character filtering and error state (Task 4.2)
 *  - Four static printer setting rows: Puerto, Papel, Corte, Modo (Task 4.5)
 *  - Test and Save action buttons (Task 4.6)
 *
 * Requirements: 3.1–3.6, 4.1–4.7, 5.1, 6.1–6.7, 12.4
 */
@Composable
fun ControlPanel(
    ipAddress: String,
    onIpAddressChange: (String) -> Unit,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine error state: non-empty input that does not match a valid IP pattern
    val isIpError = ipAddress.isNotEmpty() && !isValidIpAddress(ipAddress)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Task 4.1: Header ─────────────────────────────────────────────────
        Text(
            text       = "IMPRESORA",
            color      = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize   = 24.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text       = "POS-8360 LAN",
            color      = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Normal,
            fontSize   = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Task 4.2: IP Address Input Field ─────────────────────────────────
        OutlinedTextField(
            value         = ipAddress,
            onValueChange = { newValue ->
                // Filter: accept only digits (0-9) and periods (.)
                val filtered = newValue.filter { it.isDigit() || it == '.' }
                onIpAddressChange(filtered)
            },
            label  = { Text(text = "IP local", color = MaterialTheme.colorScheme.onPrimaryContainer) },
            isError = isIpError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor     = MaterialTheme.colorScheme.surface,
                focusedBorderColor      = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor    = MaterialTheme.colorScheme.primary,
                errorBorderColor        = Color.Red,
                focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
                errorTextColor          = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor       = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                errorLabelColor         = Color.Red
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (isIpError) {
            Text(
                text      = "Formato de IP inválido",
                color     = Color.Red,
                fontSize  = 12.sp,
                modifier  = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp, top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Task 4.5: Static Printer Settings Rows ───────────────────────────
        StaticSettingRow(label = "Puerto", value = "9100")
        Spacer(modifier = Modifier.height(4.dp))
        StaticSettingRow(label = "Papel",  value = "80mm")
        Spacer(modifier = Modifier.height(4.dp))
        StaticSettingRow(label = "Corte",  value = "Automatico")
        Spacer(modifier = Modifier.height(4.dp))
        StaticSettingRow(label = "Modo",   value = "ESC/POS")

        Spacer(modifier = Modifier.weight(1f))

        // ── Task 4.6: Action Buttons ─────────────────────────────────────────
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Test_Button
            Button(
                onClick  = onTestClick,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor   = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text     = "Probar impresora",
                    fontSize = 14.sp
                )
            }

            // Save_Button
            Button(
                onClick  = onSaveClick,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor   = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text     = "Guardar",
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Returns true if [ip] matches a valid IPv4 pattern (e.g. "192.168.1.100").
 * Each octet must be 0–255.
 */
private fun isValidIpAddress(ip: String): Boolean {
    val parts = ip.split(".")
    if (parts.size != 4) return false
    return parts.all { part ->
        part.isNotEmpty() && part.all { it.isDigit() } && part.toIntOrNull()?.let { it in 0..255 } == true
    }
}
