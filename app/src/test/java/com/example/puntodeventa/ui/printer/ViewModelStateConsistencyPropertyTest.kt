package com.example.puntodeventa.ui.printer

import com.example.puntodeventa.data.repository.PrinterPreferencesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Property-based test for Property 7: ViewModel State Update Consistency.
 *
 * For any string input to [PrinterConfigViewModel.updateIpAddress], the ViewModel
 * shall update the ipAddress property in uiState to exactly match the input parameter.
 *
 * **Validates: Requirements 10.5**
 */
class ViewModelStateConsistencyPropertyTest : FunSpec({

    /**
     * Creates a fresh [PrinterConfigViewModel] with a mocked repository that
     * returns an empty initial IP address and accepts any save call.
     */
    fun createViewModel(): PrinterConfigViewModel {
        val repo = mockk<PrinterPreferencesRepository>()
        every { repo.getIpAddress() } returns ""
        every { repo.saveIpAddress(any()) } returns Unit
        return PrinterConfigViewModel(repo)
    }

    /**
     * **Feature: printer-config-ui, Property 7: ViewModel State Update Consistency**
     *
     * For any string input to updateIpAddress, uiState.ipAddress must equal that
     * exact string immediately after the call returns.
     *
     * Uses a fresh ViewModel per iteration to ensure no state leaks between inputs.
     *
     * **Validates: Requirements 10.5**
     */
    test("Feature: printer-config-ui, Property 7: ViewModel State Update Consistency") {
        runTest {
            checkAll(PropTestConfig(iterations = 100), Arb.string()) { ipAddress ->
                val viewModel = createViewModel()
                viewModel.updateIpAddress(ipAddress)
                viewModel.uiState.value.ipAddress shouldBe ipAddress
            }
        }
    }
})
