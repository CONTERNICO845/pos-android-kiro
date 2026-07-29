package com.example.puntodeventa.ui.home

import com.example.puntodeventa.data.model.MenuItem
import com.example.puntodeventa.data.repository.MenuRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Property-based tests for HomeViewModel.
 *
 * Properties 6–10 from the local-data-persistence spec.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : StringSpec({

    val dispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(dispatcher) }
    afterSpec { Dispatchers.resetMain() }

    /**
     * Property 6: ViewModel mirrors repository items
     */
    "Property 6 — uiState.menuItems mirrors repository emission" {
        checkAll(PropTestConfig(iterations = 50), Arb.list(Arb.string(1..10), 0..5)) { names ->
            val items = names.mapIndexed { i, name ->
                MenuItem(id = "id-$i", emoji = "🌮", name = name)
            }
            val repository = mockk<MenuRepository>()
            coEvery { repository.menuItems } returns flowOf(items)

            val vm = HomeViewModel(repository)
            dispatcher.scheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assert(state.menuItems == items) {
                "Expected menuItems=$items, got ${state.menuItems}"
            }
        }
    }

    /**
     * Property 7: saveMenu new item — calls insert with correct emoji and trimmed name
     */
    "Property 7 — saveMenu in create mode calls insert with non-empty id, correct emoji and trimmed name" {
        checkAll(
            PropTestConfig(iterations = 50),
            Arb.string(1..10).filter { it.isNotBlank() },
            Arb.string(1..20).filter { it.isNotBlank() }
        ) { emoji, name ->
            val captured = slot<MenuItem>()
            val repository = mockk<MenuRepository>(relaxed = true)
            coEvery { repository.menuItems } returns flowOf(emptyList())
            coEvery { repository.insert(capture(captured)) } returns Unit

            val vm = HomeViewModel(repository)
            dispatcher.scheduler.advanceUntilIdle()

            vm.saveMenu(emoji, name)
            dispatcher.scheduler.advanceUntilIdle()

            assert(captured.isCaptured) { "insert was never called" }
            val item = captured.captured
            assert(item.id.isNotEmpty()) { "id should not be empty" }
            assert(item.emoji == emoji) { "emoji mismatch: expected $emoji, got ${item.emoji}" }
            assert(item.name == name.trim()) { "name should be trimmed: expected '${name.trim()}', got '${item.name}'" }
        }
    }

    /**
     * Property 8: saveMenu in edit mode preserves the existing item's id
     */
    "Property 8 — saveMenu in edit mode preserves original id" {
        checkAll(
            PropTestConfig(iterations = 50),
            Arb.string(5..15),              // existing id
            Arb.string(1..5).filter { it.isNotBlank() },   // emoji
            Arb.string(1..20).filter { it.isNotBlank() }   // name
        ) { existingId, emoji, name ->
            val captured = slot<MenuItem>()
            val repository = mockk<MenuRepository>(relaxed = true)
            val existing = MenuItem(id = existingId, emoji = "🔥", name = "Old")
            coEvery { repository.menuItems } returns flowOf(listOf(existing))
            coEvery { repository.insert(capture(captured)) } returns Unit

            val vm = HomeViewModel(repository)
            dispatcher.scheduler.advanceUntilIdle()

            vm.openEditDialog(existing)
            vm.saveMenu(emoji, name)
            dispatcher.scheduler.advanceUntilIdle()

            assert(captured.isCaptured) { "insert was never called" }
            assert(captured.captured.id == existingId) {
                "id should be preserved: expected $existingId, got ${captured.captured.id}"
            }
        }
    }

    /**
     * Property 9: deleteMenu delegates to repository with correct id
     */
    "Property 9 — deleteMenu calls repository.deleteById with exact id" {
        checkAll(PropTestConfig(iterations = 50), Arb.string(5..20)) { id ->
            val repository = mockk<MenuRepository>(relaxed = true)
            coEvery { repository.menuItems } returns flowOf(emptyList())

            val vm = HomeViewModel(repository)
            dispatcher.scheduler.advanceUntilIdle()

            vm.deleteMenu(id)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { repository.deleteById(id) }
        }
    }

    /**
     * Property 10: Blank-input validation gate — insert is never called
     */
    "Property 10 — saveMenu with blank name does not call insert" {
        checkAll(PropTestConfig(iterations = 50), Arb.string(1..5).filter { it.isNotBlank() }) { emoji ->
            val repository = mockk<MenuRepository>(relaxed = true)
            coEvery { repository.menuItems } returns flowOf(emptyList())

            val vm = HomeViewModel(repository)
            dispatcher.scheduler.advanceUntilIdle()

            vm.saveMenu(emoji, "   ")  // blank name
            dispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { repository.insert(any()) }
        }
    }

    "Property 10b — saveMenu with blank emoji does not call insert" {
        checkAll(PropTestConfig(iterations = 50), Arb.string(1..20).filter { it.isNotBlank() }) { name ->
            val repository = mockk<MenuRepository>(relaxed = true)
            coEvery { repository.menuItems } returns flowOf(emptyList())

            val vm = HomeViewModel(repository)
            dispatcher.scheduler.advanceUntilIdle()

            vm.saveMenu("", name)  // blank emoji
            dispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { repository.insert(any()) }
        }
    }
})
