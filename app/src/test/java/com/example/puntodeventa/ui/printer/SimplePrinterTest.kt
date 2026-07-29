package com.example.puntodeventa.ui.printer

import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk

class SimplePrinterTest : FunSpec({

    test("printer classes are accessible and have correct default state") {
        val repo = mockk<PrinterPreferencesRepository>()
        every { repo.getIpAddress() } returns ""

        val viewModel = PrinterConfigViewModel(repo)
        viewModel shouldNotBe null
        viewModel.uiState shouldNotBe null

        val uiState = PrinterConfigUiState()
        uiState.ipAddress shouldBe ""
        uiState.connectionStatus shouldBe ConnectionStatus.Disconnected
    }
})
