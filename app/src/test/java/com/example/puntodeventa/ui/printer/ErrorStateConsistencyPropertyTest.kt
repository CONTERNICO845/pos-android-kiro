package com.example.puntodeventa.ui.printer

import com.example.puntodeventa.data.printer.EscPosPrinterLan
import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
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
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Property-based test for Property 3: Error State Implies Error Message.
 *
 * For any state where connectionStatus == Error, the errorMessage field SHALL be
 * non-null and non-empty. Conversely, for any state where connectionStatus == Connected,
 * the errorMessage field SHALL be null.
 *
 * **Validates: Requirements 7.1, 7.2, 7.4**
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ErrorStateConsistencyPropertyTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
        mockkObject(EscPosPrinterLan)
    }

    afterSpec {
        unmockkObject(EscPosPrinterLan)
        Dispatchers.resetMain()
    }

    /**
     * Creates a fresh [PrinterConfigViewModel] with a mocked repository that
     * returns the given IP and accepts any save call.
     */
    fun createViewModel(ip: String): PrinterConfigViewModel {
        val repo = mockk<PrinterPreferencesRepository>()
        every { repo.getIpAddress() } returns ip
        every { repo.saveIpAddress(any()) } returns Unit
        return PrinterConfigViewModel(repo)
    }

    /**
     * **Feature: lan-printer-connection, Property 3: Error State Implies Error Message**
     *
     * After testPrinter() completes (both success and failure cases), verify:
     * 1. If connectionStatus == Error → errorMessage != null && errorMessage.isNotEmpty()
     * 2. If connectionStatus == Connected → errorMessage == null
     *
     * Generates random IPs and randomly decides whether the mock should succeed
     * or throw (SocketTimeoutException or IOException).
     *
     * **Validates: Requirements 7.1, 7.2, 7.4**
     */
    test("Feature: lan-printer-connection, Property 3: Error State Implies Error Message") {
        checkAll(
            PropTestConfig(iterations = 100),
            Arb.string(1..15),
            Arb.int(0..2)
        ) { ip, failureMode ->
            runTest(testDispatcher) {
                // Configure mock based on failure mode
                when (failureMode) {
                    0 -> {
                        // Success case
                        coEvery { EscPosPrinterLan.testConnection(any()) } returns Unit
                    }
                    1 -> {
                        // Timeout failure
                        coEvery { EscPosPrinterLan.testConnection(any()) } throws
                                SocketTimeoutException("timeout")
                    }
                    else -> {
                        // General IO failure
                        coEvery { EscPosPrinterLan.testConnection(any()) } throws
                                IOException("connection refused")
                    }
                }

                val viewModel = createViewModel(ip)
                viewModel.testPrinter()
                advanceUntilIdle()

                val state = viewModel.uiState.value

                // Verify the invariant
                when (state.connectionStatus) {
                    ConnectionStatus.Error -> {
                        state.errorMessage.shouldNotBeNull()
                        state.errorMessage shouldNotBe ""
                    }
                    ConnectionStatus.Connected -> {
                        state.errorMessage.shouldBeNull()
                    }
                    else -> {
                        // Testing or Disconnected states are not expected after completion,
                        // but we don't assert on them for this property
                    }
                }
            }
        }
    }
})
