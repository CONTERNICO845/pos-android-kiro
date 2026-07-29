package com.example.puntodeventa.ui.printer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Full stateless editor used by PrinterScreen. */
@Composable
fun ControlPanel(
    state: PrinterConfigUiState,
    onNameChange: (String) -> Unit,
    onIpAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onPaperSizeChange: (Int) -> Unit,
    onAutoCutChange: (Boolean) -> Unit,
    onProtocolChange: (String) -> Unit,
    onDiscoverClick: () -> Unit,
    onDiscoveredIpClick: (String) -> Unit,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    onPrinterMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "IMPRESORA",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "POS-8360 LAN",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 16.sp
                )
            }
            OutlinedButton(
                onClick = onPrinterMenuClick,
                modifier = Modifier.semantics { contentDescription = "Menú de impresoras" }
            ) { Text("Impresoras") }
        }

        OutlinedTextField(
            value = state.draft.name,
            onValueChange = onNameChange,
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.draft.ipAddress,
            onValueChange = onIpAddressChange,
            label = { Text("IP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = state.draft.ipAddress.isNotEmpty() && !isValidIpAddress(state.draft.ipAddress),
            supportingText = if (state.draft.ipAddress.isNotEmpty() && !isValidIpAddress(state.draft.ipAddress)) {
                { Text("Formato de IP inválido", color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.portInput,
            onValueChange = onPortChange,
            label = { Text("Puerto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tamaño de papel", modifier = Modifier.weight(1f))
            listOf(58, 80).forEach { size ->
                FilterChip(
                    selected = state.draft.paperSize == size,
                    onClick = { onPaperSizeChange(size) },
                    label = { Text("$size mm") },
                    modifier = Modifier.semantics { contentDescription = "Papel $size milímetros" }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Autocorte")
            Switch(
                checked = state.draft.autoCut,
                onCheckedChange = onAutoCutChange,
                modifier = Modifier.semantics { contentDescription = "Autocorte" }
            )
        }
        OutlinedTextField(
            value = state.draft.protocol,
            onValueChange = onProtocolChange,
            label = { Text("Protocolo/Modelo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = onDiscoverClick,
            enabled = !state.isDiscovering,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isDiscovering) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp).height(20.dp),
                    strokeWidth = 2.dp
                )
                Text("Buscando impresoras…")
            } else Text("Buscar Impresoras")
        }
        state.discoveredIps.forEach { ip ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDiscoveredIpClick(ip) }
                    .semantics { contentDescription = "Usar impresora $ip" },
                shape = MaterialTheme.shapes.small,
                tonalElevation = 2.dp
            ) {
                Text(ip, modifier = Modifier.padding(12.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onTestClick, enabled = !state.isLoading, modifier = Modifier.weight(1f)) {
                Text("Probar impresora")
            }
            Button(onClick = onSaveClick, modifier = Modifier.weight(1f)) {
                Text("Guardar")
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

/** Legacy overload retained so existing androidTest sources continue to compile. */
@Composable
fun ControlPanel(
    ipAddress: String,
    onIpAddressChange: (String) -> Unit,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIpError = ipAddress.isNotEmpty() && !isValidIpAddress(ipAddress)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "IMPRESORA",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Spacer(Modifier.height(8.dp))
        Text("POS-8360 LAN", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 16.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = ipAddress,
            onValueChange = { onIpAddressChange(it.filter { char -> char.isDigit() || char == '.' }) },
            label = { Text("IP local") },
            isError = isIpError,
            modifier = Modifier.fillMaxWidth()
        )
        if (isIpError) {
            Text(
                "Formato de IP inválido",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        StaticSettingRow("Puerto", "9100")
        Spacer(Modifier.height(4.dp))
        StaticSettingRow("Papel", "80mm")
        Spacer(Modifier.height(4.dp))
        StaticSettingRow("Corte", "Automatico")
        Spacer(Modifier.height(4.dp))
        StaticSettingRow("Modo", "ESC/POS")
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTestClick, modifier = Modifier.weight(1f)) { Text("Probar impresora") }
            Button(onClick = onSaveClick, modifier = Modifier.weight(1f)) { Text("Guardar") }
        }
    }
}

private fun isValidIpAddress(ip: String): Boolean {
    val parts = ip.split(".")
    return parts.size == 4 && parts.all { part ->
        part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull()?.let { it in 0..255 } == true
    }
}
