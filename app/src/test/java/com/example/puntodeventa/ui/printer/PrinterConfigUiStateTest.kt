package com.example.puntodeventa.ui.printer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for PrinterConfigUiState data class and supporting enums.
 * Tests data class functionality, enum values, and TestResult data class.
 */
class PrinterConfigUiStateTest : FunSpec({

    test("PrinterConfigUiState should have correct default values") {
        val uiState = PrinterConfigUiState()

        uiState.ipAddress shouldBe ""
        uiState.isLoading shouldBe false
        uiState.errorMessage shouldBe null
        uiState.connectionStatus shouldBe ConnectionStatus.Disconnected
        uiState.lastTestResult shouldBe null
    }

    test("PrinterConfigUiState copy should work correctly") {
        val initialState = PrinterConfigUiState()
        val newIpAddress = "192.168.1.100"

        val updatedState = initialState.copy(ipAddress = newIpAddress)

        updatedState.ipAddress shouldBe newIpAddress
        updatedState.isLoading shouldBe false
        updatedState.errorMessage shouldBe null
        updatedState.connectionStatus shouldBe ConnectionStatus.Disconnected
    }

    test("PrinterConfigUiState should support all properties being set") {
        val testResult = TestResult(
            success = true,
            timestamp = System.currentTimeMillis(),
            message = "Test successful"
        )

        val uiState = PrinterConfigUiState(
            ipAddress = "192.168.1.100",
            isLoading = true,
            errorMessage = "Connection error",
            connectionStatus = ConnectionStatus.Connected,
            lastTestResult = testResult
        )

        uiState.ipAddress shouldBe "192.168.1.100"
        uiState.isLoading shouldBe true
        uiState.errorMessage shouldBe "Connection error"
        uiState.connectionStatus shouldBe ConnectionStatus.Connected
        uiState.lastTestResult shouldBe testResult
    }

    test("ConnectionStatus enum should have all required values") {
        val allStatuses = ConnectionStatus.values()

        allStatuses.contains(ConnectionStatus.Connected) shouldBe true
        allStatuses.contains(ConnectionStatus.Disconnected) shouldBe true
        allStatuses.contains(ConnectionStatus.Testing) shouldBe true
        allStatuses.contains(ConnectionStatus.Error) shouldBe true
        allStatuses.size shouldBe 4
    }

    test("TestResult data class should work correctly") {
        val currentTime = System.currentTimeMillis()
        val testMessage = "Printer test completed successfully"

        val successResult = TestResult(
            success = true,
            timestamp = currentTime,
            message = testMessage
        )

        successResult.success shouldBe true
        successResult.timestamp shouldBe currentTime
        successResult.message shouldBe testMessage

        val failureResult = TestResult(
            success = false,
            timestamp = currentTime,
            message = "Printer test failed"
        )

        failureResult.success shouldBe false
        failureResult.timestamp shouldBe currentTime
        failureResult.message shouldBe "Printer test failed"
    }

    test("TestResult should support copy functionality") {
        val originalResult = TestResult(
            success = false,
            timestamp = 12345L,
            message = "Original message"
        )

        val updatedResult = originalResult.copy(
            success = true,
            message = "Updated message"
        )

        updatedResult.success shouldBe true
        updatedResult.timestamp shouldBe 12345L
        updatedResult.message shouldBe "Updated message"
    }

    test("PrinterConfigUiState equality should work correctly") {
        val state1 = PrinterConfigUiState(ipAddress = "192.168.1.100")
        val state2 = PrinterConfigUiState(ipAddress = "192.168.1.100")
        val state3 = PrinterConfigUiState(ipAddress = "192.168.1.200")

        state1 shouldBe state2
        state1 shouldNotBe state3
    }
})
