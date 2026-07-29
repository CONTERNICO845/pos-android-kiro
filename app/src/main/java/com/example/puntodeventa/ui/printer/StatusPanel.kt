package com.example.puntodeventa.ui.printer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusPanel(
    modifier: Modifier = Modifier,
    uiState: PrinterConfigUiState? = null
) {
    val printer = uiState?.draft
    val connection = when (uiState?.connectionStatus) {
        ConnectionStatus.Connected -> "Conectada"
        ConnectionStatus.Testing -> "Probando…"
        ConnectionStatus.Error -> "Error"
        else -> "Desconectada"
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Estado de conexion",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(connection, color = MaterialTheme.colorScheme.onSurface)
        uiState?.statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        StatusInfoRow("Modelo", printer?.protocol ?: PrinterSpecs.MODEL)
        StatusInfoRow("Papel", printer?.let { "${it.paperSize}mm" } ?: PrinterSpecs.PAPER_SIZE)
        StatusInfoRow("Conexion", PrinterSpecs.CONNECTION_TYPE)
        StatusInfoRow("Puerto", printer?.port?.toString() ?: PrinterSpecs.PORT)
        StatusInfoRow(
            "Cortador",
            printer?.let { if (it.autoCut) "Automatico" else "Manual" } ?: PrinterSpecs.CUTTER
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Cuando presiones imprimir prueba, se enviara el ticket real por red usando la clase Java ESC/POS.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp,
            softWrap = true
        )
    }
}
