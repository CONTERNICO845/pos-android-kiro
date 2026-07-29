package com.example.puntodeventa.ui.printer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    Row(modifier = Modifier.fillMaxSize()) {
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
