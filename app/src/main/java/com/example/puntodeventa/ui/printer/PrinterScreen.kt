package com.example.puntodeventa.ui.printer

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.puntodeventa.data.model.PrinterConfig
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(
    viewModel: PrinterConfigViewModel = run {
        val context = LocalContext.current
        viewModel(factory = PrinterConfigViewModel.Factory(PrinterPreferencesRepository(context)))
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPrinterSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.discoverPrinters()
        else viewModel.reportError("Se necesita permiso de red local para buscar impresoras")
    }

    val requestDiscovery: () -> Unit = {
        if (Build.VERSION.SDK_INT < 37 ||
            ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.discoverPrinters()
        } else {
            permissionLauncher.launch(ACCESS_LOCAL_NETWORK)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showPrinterSheet) {
        PrinterMenuSheet(
            printers = uiState.printers,
            selectedPrinterId = uiState.selectedPrinterId,
            onSelect = {
                viewModel.selectPrinter(it)
                showPrinterSheet = false
            },
            onActiveChange = viewModel::togglePrinterActive,
            onDelete = viewModel::deletePrinter,
            onAdd = {
                viewModel.startAdd()
                showPrinterSheet = false
            },
            onDismiss = { showPrinterSheet = false }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Row(Modifier.fillMaxSize().padding(paddingValues)) {
            ControlPanel(
                state = uiState,
                onNameChange = viewModel::updateName,
                onIpAddressChange = viewModel::updateIpAddress,
                onPortChange = viewModel::updatePort,
                onPaperSizeChange = viewModel::updatePaperSize,
                onAutoCutChange = viewModel::updateAutoCut,
                onProtocolChange = viewModel::updateProtocol,
                onDiscoverClick = requestDiscovery,
                onDiscoveredIpClick = viewModel::selectDiscoveredIp,
                onTestClick = viewModel::testPrinter,
                onSaveClick = viewModel::savePrinter,
                onPrinterMenuClick = { showPrinterSheet = true },
                modifier = Modifier.weight(1f)
            )
            StatusPanel(uiState = uiState, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinterMenuSheet(
    printers: List<PrinterConfig>,
    selectedPrinterId: String?,
    onSelect: (String) -> Unit,
    onActiveChange: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Impresoras guardadas", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(printers, key = { it.id }) { printer ->
                    ListItem(
                        headlineContent = { Text(printer.name) },
                        supportingContent = { Text("${printer.ipAddress}:${printer.port}") },
                        trailingContent = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Switch(
                                    checked = printer.isActive,
                                    onCheckedChange = { onActiveChange(printer.id, it) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Activar ${printer.name}"
                                    }
                                )
                                androidx.compose.material3.TextButton(
                                    onClick = { onDelete(printer.id) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Eliminar ${printer.name}"
                                    }
                                ) { Text("Eliminar") }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(printer.id) }
                            .semantics {
                                contentDescription = if (printer.id == selectedPrinterId) {
                                    "${printer.name}, seleccionada, editar"
                                } else "${printer.name}, editar"
                            }
                    )
                }
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) { Text("Agregar Nueva Impresora") }
        }
    }
}
