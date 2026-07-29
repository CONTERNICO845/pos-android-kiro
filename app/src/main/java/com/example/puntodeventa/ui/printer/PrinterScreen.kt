package com.example.puntodeventa.ui.printer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository

/**
 * Main entry point for the printer configuration screen.
 *
 * Implements a two-column layout (Requirements 2.1–2.5):
 *  - Left column (weight = 1f): [ControlPanel] with CardBackground color
 *  - Right column (weight = 1f): [StatusPanel] with StatusPanelBackground color
 *
 * State is collected from [PrinterConfigViewModel] via [collectAsStateWithLifecycle]
 * and forwarded to [ControlPanel] as individual parameters (Requirements 12.1, 12.2).
 *
 * Shows Snackbar feedback when the printer test succeeds or fails (Requirement 7.5).
 *
 * When no [viewModel] is provided the function creates one using [LocalContext] so
 * it can be used directly in previews and instrumented tests without an explicit factory.
 */
@Composable
fun PrinterScreen(
    viewModel: PrinterConfigViewModel = run {
        val context = LocalContext.current
        viewModel(
            factory = PrinterConfigViewModel.Factory(
                PrinterPreferencesRepository(context)
            )
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error Snackbar when errorMessage becomes non-null (Requirement 7.5)
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Show success Snackbar when a successful test completes (Requirement 7.5)
    LaunchedEffect(uiState.lastTestResult) {
        uiState.lastTestResult?.let { result ->
            if (result.success) {
                snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Left column — Control_Panel (50% width, weight = 1f, Requirement 2.2 & 2.4)
            ControlPanel(
                ipAddress         = uiState.ipAddress,
                onIpAddressChange = viewModel::updateIpAddress,
                onTestClick       = viewModel::testPrinter,
                onSaveClick       = viewModel::saveIpAddress,
                modifier          = Modifier.weight(1f)
            )

            // Right column — Status_Panel (50% width, weight = 1f, Requirement 2.3 & 2.5)
            StatusPanel(
                modifier = Modifier.weight(1f)
            )
        }
    }
}
