package com.example.puntodeventa.ui.pos

import com.example.puntodeventa.data.model.Category
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll

// Feature: pos-main-screen, Property 1: Category Tab Ordering

class CategoryTabOrderPropertyTest : StringSpec({

    /**
     * Property 1: Category Tab Ordering
     *
     * For any list of categories returned by the repository, the tab order SHALL always be
     * the "TODO" tab first (represented as null), followed by the remaining categories sorted
     * alphabetically by name (case-insensitive).
     *
     * **Validates: Requirements 3.2, 3.1**
     */
    "Property 1 - buildTabOrder always produces null first, followed by categories sorted alphabetically (case-insensitive)" {
        checkAll(
            PropTestConfig(iterations = 200),
            Arb.list(Arb.string(1..50), range = 0..20)
        ) { names ->
            val categories = names.mapIndexed { index, name ->
                Category(
                    id = "id-$index",
                    name = name,
                    associatedMenuId = "menu-1"
                )
            }

            val result = buildTabOrder(categories)

            // First element is always null (the "TODO" tab)
            result.first() shouldBe null

            // Result size is always input.size + 1
            result.size shouldBe categories.size + 1

            // Remaining elements are sorted alphabetically by name (case-insensitive)
            val remaining = result.drop(1).filterNotNull()
            val expectedSorted = categories.sortedBy { it.name.lowercase() }
            remaining shouldBe expectedSorted

            // All input categories appear in the result (none lost, none duplicated)
            remaining.size shouldBe categories.size
        }
    }
})
