package com.example.puntodeventa.ui.printer

import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.common.ExperimentalKotest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

/**
 * Property-based test for Property 1: Default IP Round-Trip.
 *
 * For any fresh PrinterPreferencesRepository (no saved IP), getIpAddress() returns "192.168.1.248";
 * for any saved IP string via saveIpAddress(ip), getIpAddress() returns that string unchanged.
 *
 * **Validates: Requirements 2.1, 2.2**
 */
@OptIn(ExperimentalKotest::class)
class DefaultIpRoundTripPropertyTest : FunSpec({

    /**
     * **Feature: lan-printer-connection, Property 1: Default IP Round-Trip**
     *
     * For a fresh repository (no saved IP), the ViewModel initializes with
     * the default IP "192.168.1.248".
     *
     * **Validates: Requirements 2.1**
     */
    test("Feature: lan-printer-connection, Property 1: Default IP Round-Trip — fresh repo returns default") {
        val repo = mockk<PrinterPreferencesRepository>()
        every { repo.getIpAddress() } returns "192.168.1.248"

        val viewModel = PrinterConfigViewModel(repo)

        viewModel.uiState.value.ipAddress shouldBe "192.168.1.248"
    }

    /**
     * **Feature: lan-printer-connection, Property 1: Default IP Round-Trip**
     *
     * For any arbitrary string IP, after saving via the ViewModel's saveIpAddress(),
     * a new ViewModel instance reading from the same repo sees that IP unchanged.
     *
     * This simulates the persistence round-trip: user types IP → save → app restart → IP loaded.
     *
     * **Validates: Requirements 2.2**
     */
    test("Feature: lan-printer-connection, Property 1: Default IP Round-Trip — saved IP round-trip") {
        checkAll(PropTestConfig(iterations = 100), Arb.string()) { ip ->
            // Capture what gets saved to SharedPreferences
            val savedSlot = slot<String>()
            val repo = mockk<PrinterPreferencesRepository>()
            every { repo.getIpAddress() } returns ""
            every { repo.saveIpAddress(capture(savedSlot)) } returns Unit

            // First ViewModel: user types and saves an IP
            val viewModel1 = PrinterConfigViewModel(repo)
            viewModel1.updateIpAddress(ip)
            viewModel1.saveIpAddress()

            // Verify the repo received the exact IP
            savedSlot.captured shouldBe ip

            // Simulate app restart: repo now returns the saved IP
            every { repo.getIpAddress() } returns savedSlot.captured

            // Second ViewModel: reads the persisted IP on construction
            val viewModel2 = PrinterConfigViewModel(repo)
            viewModel2.uiState.value.ipAddress shouldBe ip
        }
    }
})
