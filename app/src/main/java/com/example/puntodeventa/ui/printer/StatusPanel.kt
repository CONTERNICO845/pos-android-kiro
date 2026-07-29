package com.example.puntodeventa.ui.printer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

/**
 * Right-column status panel for the printer configuration screen.
 *
 * Displays:
 *  - Header: "Estado de conexion" (Task 5.1, Requirements 7.1–7.3)
 *  - Five status info rows showing printer specifications (Task 5.2, Requirements 8.1–8.5)
 *  - Description text explaining the test print behaviour (Task 5.3, Requirements 9.1–9.4)
 *
 * This composable accepts no parameters — it renders static data only (Requirement 12.6).
 */
@Composable
fun StatusPanel(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // ── Task 5.1: Header ──────────────────────────────────────────────────
        // Requirements 7.1, 7.2, 7.3
        Text(
            text       = "Estado de conexion",
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize   = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Task 5.2: Status Information Rows ────────────────────────────────
        // Requirements 8.1–8.5  (label/value styling enforced inside StatusInfoRow per 8.6–8.7)
        StatusInfoRow(
            label = "Modelo",
            value = PrinterSpecs.MODEL
        )
        StatusInfoRow(
            label = "Papel",
            value = PrinterSpecs.PAPER_SIZE
        )
        StatusInfoRow(
            label = "Conexion",
            value = PrinterSpecs.CONNECTION_TYPE
        )
        StatusInfoRow(
            label = "Puerto",
            value = PrinterSpecs.PORT
        )
        StatusInfoRow(
            label = "Cortador",
            value = PrinterSpecs.CUTTER
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Task 5.3: Description Text ────────────────────────────────────────
        // Requirements 9.1, 9.2, 9.3, 9.4
        Text(
            text       = "Cuando presiones imprimir prueba, se enviara el ticket real por red usando la clase Java ESC/POS.",
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp,
            // softWrap is true by default — text wraps across multiple lines (Requirement 9.4)
            softWrap   = true
        )
    }
}
