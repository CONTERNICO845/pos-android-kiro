package com.example.puntodeventa.data.local

/**
 * Enumeration of the allowed selection modes for a customization group.
 * Stored as its [value] string in the customization_groups table.
 */
enum class SelectionType(val value: String) {
    MULTIPLE_CHECKBOXES("multiple_checkboxes"),
    SINGLE_OPTION("single_option");

    companion object {
        private val byValue = entries.associateBy { it.value }

        /** Returns null if [raw] is not a recognized value. */
        fun fromValue(raw: String): SelectionType? = byValue[raw]
    }
}
