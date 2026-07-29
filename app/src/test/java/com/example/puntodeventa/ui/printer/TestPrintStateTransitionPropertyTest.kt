package com.example.puntodeventa.ui.printer

import com.example.puntodeventa.data.printer.EscPosPrinterLan
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Property-based test for Property 2: Test Print State Transition Consistency.
 *
 * For any invocation of [PrinterConfigViewModel.testPrinter], the connectionStatus
 * SHALL transition from its current value to [ConnectionStatus.Testing] before the
 * network call, and then to either [ConnectionStatus.Connected] (on success) or
 * [ConnectionStatus.Error] (on failure) — never remaining in [ConnectionStatus.Testing]
 * after the operation completes.
 *
 * **Validates: Requirements 6.3, 6.7, 6.8**
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestPrintStateTransitionPropertyTest : FunSpec({

    /**
     * **Feature: lan-printer-connection, Property 2: Test Print State Transition Consistency**
     *
     * For each generated (IP, success/failure) pair:
     * 1. Mock EscPosPrinterLan.testConnection to either succeed or throw
     * 2. Call viewModel.testPrinter()
     * 3. Verify connectionStatus == Testing immediately after the synchronous portion
     * 4. Advance the coroutine dispatcher until idle
     * 5. Verify connectionStatus is either Connected or Error — never Testing
     *
     * **Validates: Requirements 6.3, 6.7, 6.8**
     */
    test("Feature: lan-printer-connection, Property 2: Test Print State Transition Consistency") {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        try {
            mockkObject(EscPosPrinterLan)
            try {
                runTest(testDispatcher) {
                    checkAll(
                        PropTestConfig(iterations = 100),
                        Arb.string(1..20),
                        Arb.boolean()
                    ) { ip, shouldSucceed ->
                        // Set up a fresh ViewModel for each iteration
                        val repo = mockk<PrinterPreferencesRepository>()
                        every { repo.getIpAddress() } returns ip
                        every { repo.saveIpAddress(any()) } returns Unit
                        val viewModel = PrinterConfigViewModel(repo)

                        // Mock testConnection based on generated outcome
                        if (shouldSucceed) {
                            coEvery { EscPosPrinterLan.testConnection(ip) } returns Unit
                        } else {
                            coEvery { EscPosPrinterLan.testConnection(ip) } throws
                                    RuntimeException("Connection failed to $ip")
                        }

                        // Act: call testPrinter
                        viewModel.testPrinter()

                        // Assert: immediately after calling testPrinter, the synchronous
                        // state update sets connectionStatus to Testing
                        viewModel.uiState.value.connectionStatus shouldBe ConnectionStatus.Testing

                        // Advance coroutines until the launched job completes
                        advanceUntilIdle()

                        // Assert: after completion, connectionStatus is NEVER Testing
                        val finalStatus = viewModel.uiState.value.connectionStatus
                        finalStatus shouldNotBe ConnectionStatus.Testing

                        // Assert: final status matches the mocked outcome
                        if (shouldSucceed) {
                            finalStatus shouldBe ConnectionStatus.Connected
                        } else {
                            finalStatus shouldBe ConnectionStatus.Error
                        }
                    }
                }
            } finally {
                unmockkObject(EscPosPrinterLan)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
})
