package com.example.puntodeventa.ui.theme

import com.example.puntodeventa.data.repository.ThemePreferencesRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for ThemeViewModel.
 *
 * Validates: Requirements 3.3, 3.4, 10.2
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    test("initial value is DEFAULT_GREEN when repository emits DEFAULT") {
        val repository = mockk<ThemePreferencesRepository>()
        coEvery { repository.themeFlow } returns flowOf(AppTheme.DEFAULT_GREEN)

        val viewModel = ThemeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.currentTheme.value shouldBe AppTheme.DEFAULT_GREEN
    }

    test("currentTheme reflects repository emission") {
        val repository = mockk<ThemePreferencesRepository>()
        coEvery { repository.themeFlow } returns flowOf(AppTheme.OCEAN_BLUE)

        val viewModel = ThemeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.currentTheme.value shouldBe AppTheme.OCEAN_BLUE
    }

    test("ViewModel falls back to DEFAULT_GREEN on repository error") {
        val repository = mockk<ThemePreferencesRepository>()
        coEvery { repository.themeFlow } returns flow {
            throw RuntimeException("DataStore corrupted")
        }

        val viewModel = ThemeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.currentTheme.value shouldBe AppTheme.DEFAULT_GREEN
    }

    test("selectTheme triggers repository saveTheme") {
        val repository = mockk<ThemePreferencesRepository>(relaxed = true)
        coEvery { repository.themeFlow } returns flowOf(AppTheme.DEFAULT_GREEN)

        val viewModel = ThemeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTheme(AppTheme.DARK_NEON)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.saveTheme(AppTheme.DARK_NEON) }
    }

    test("selectTheme with SUNSET_ORANGE calls saveTheme with correct value") {
        val repository = mockk<ThemePreferencesRepository>(relaxed = true)
        coEvery { repository.themeFlow } returns flowOf(AppTheme.DEFAULT_GREEN)

        val viewModel = ThemeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTheme(AppTheme.SUNSET_ORANGE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.saveTheme(AppTheme.SUNSET_ORANGE) }
    }
})
