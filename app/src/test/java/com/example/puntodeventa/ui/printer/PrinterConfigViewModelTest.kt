package com.example.puntodeventa.ui.printer

import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for PrinterConfigViewModel.
 * Tests ViewModel state management and method implementations.
 */
class PrinterConfigViewModelTest : FunSpec({

    lateinit var viewModel: PrinterConfigViewModel

    beforeEach {
        val repo = mockk<PrinterPreferencesRepository>()
        every { repo.getIpAddress() } returns ""
        every { repo.saveIpAddress(any()) } returns Unit
        viewModel = PrinterConfigViewModel(repo)
    }

    test("initial state should have empty ip address and default values") {
        runTest {
            val initialState = viewModel.uiState.first()

            initialState.ipAddress shouldBe ""
            initialState.isLoading shouldBe false
            initialState.errorMessage shouldBe null
            initialState.connectionStatus shouldBe ConnectionStatus.Disconnected
            initialState.lastTestResult shouldBe null
        }
    }

    test("updateIpAddress should update state correctly") {
        runTest {
            val testIpAddress = "192.168.1.100"

            viewModel.updateIpAddress(testIpAddress)

            val updatedState = viewModel.uiState.first()
            updatedState.ipAddress shouldBe testIpAddress

            // Other state values should remain unchanged
            updatedState.isLoading shouldBe false
            updatedState.errorMessage shouldBe null
            updatedState.connectionStatus shouldBe ConnectionStatus.Disconnected
            updatedState.lastTestResult shouldBe null
        }
    }

    test("updateIpAddress should handle empty string") {
        runTest {
            // First set a value
            viewModel.updateIpAddress("192.168.1.100")
            // Then update to empty string
            viewModel.updateIpAddress("")

            viewModel.uiState.first().ipAddress shouldBe ""
        }
    }

    test("updateIpAddress should handle multiple sequential updates") {
        runTest {
            val firstIp  = "192.168.1.100"
            val secondIp = "10.0.0.1"
            val thirdIp  = "172.16.1.50"

            viewModel.updateIpAddress(firstIp)
            viewModel.uiState.first().ipAddress shouldBe firstIp

            viewModel.updateIpAddress(secondIp)
            viewModel.uiState.first().ipAddress shouldBe secondIp

            viewModel.updateIpAddress(thirdIp)
            viewModel.uiState.first().ipAddress shouldBe thirdIp
        }
    }

    test("testPrinter method should exist and not throw exception") {
        // Method should exist and be callable (placeholder implementation)
        viewModel.testPrinter()
    }

    test("saveIpAddress method should exist and not throw exception") {
        // Method should exist and be callable (placeholder implementation)
        viewModel.saveIpAddress()
    }
})
