package com.example.puntodeventa.data.local

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.string
import io.kotest.property.assume
import io.kotest.property.checkAll

/**
 * Unit tests for [SelectionType.fromValue] (task 1.2).
 *
 * `selectionType` is persisted in `customization_groups` as a raw String because the schema
 * declares no TypeConverter. `fromValue` is therefore the only guard between the database and
 * the domain, and `ProductRepository.insertGroup` relies on it returning `null` for anything
 * unrecognized.
 *
 * _Requirements: 3.3_
 */
class SelectionTypeTest : StringSpec({

    // ── Valid values ─────────────────────────────────────────────────────────

    "fromValue maps 'multiple_checkboxes' to MULTIPLE_CHECKBOXES" {
        SelectionType.fromValue("multiple_checkboxes") shouldBe SelectionType.MULTIPLE_CHECKBOXES
    }

    "fromValue maps 'single_option' to SINGLE_OPTION" {
        SelectionType.fromValue("single_option") shouldBe SelectionType.SINGLE_OPTION
    }

    "every enum entry round-trips through its own value" {
        // Guards against a future entry being added without a matching `value` mapping.
        SelectionType.entries.forEach { entry ->
            SelectionType.fromValue(entry.value) shouldBe entry
        }
    }

    "the enum declares exactly the two documented entries" {
        SelectionType.entries.map { it.value } shouldBe
            listOf("multiple_checkboxes", "single_option")
    }

    // ── Invalid values ───────────────────────────────────────────────────────

    "fromValue returns null for the empty string" {
        SelectionType.fromValue("").shouldBeNull()
    }

    "fromValue returns null for whitespace-only input" {
        listOf(" ", "   ", "\t", "\n", " \t\n ").forEach { raw ->
            SelectionType.fromValue(raw).shouldBeNull()
        }
    }

    "fromValue returns null for untrimmed variants of valid values" {
        // Lookup is an exact map hit, so surrounding whitespace must NOT be tolerated.
        listOf(
            " multiple_checkboxes",
            "multiple_checkboxes ",
            " single_option ",
            "\tsingle_option"
        ).forEach { raw ->
            SelectionType.fromValue(raw).shouldBeNull()
        }
    }

    "fromValue is case-sensitive and rejects differently-cased valid values" {
        listOf(
            "MULTIPLE_CHECKBOXES",
            "Multiple_Checkboxes",
            "SINGLE_OPTION",
            "Single_Option"
        ).forEach { raw ->
            SelectionType.fromValue(raw).shouldBeNull()
        }
    }

    "fromValue returns null for enum constant names rather than their values" {
        // The persisted column stores `value`, not `name`; passing the name must fail.
        SelectionType.fromValue("MULTIPLE_CHECKBOXES").shouldBeNull()
        SelectionType.fromValue("SINGLE_OPTION").shouldBeNull()
    }

    "fromValue returns null for any arbitrary string that is not a declared value" {
        val validValues = SelectionType.entries.map { it.value }.toSet()

        checkAll(PropTestConfig(iterations = 100), Arb.string()) { raw ->
            assume(raw !in validValues)
            SelectionType.fromValue(raw).shouldBeNull()
        }
    }
})
