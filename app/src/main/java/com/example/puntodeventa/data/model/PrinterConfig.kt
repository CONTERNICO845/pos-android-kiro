package com.example.puntodeventa.data.model

import kotlinx.serialization.Serializable

/** Persisted configuration for one ESC/POS network printer. */
@Serializable
data class PrinterConfig(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 9100,
    val paperSize: Int = 80,
    val autoCut: Boolean = true,
    val protocol: String = "ESC/POS",
    val isActive: Boolean = true
) {
    companion object {
        const val DEFAULT_ID = "default-printer"

        /** Creates the stable initial configuration used during legacy migration. */
        fun default(ipAddress: String) = PrinterConfig(
            id = DEFAULT_ID,
            name = "Impresora principal",
            ipAddress = ipAddress
        )
    }
}
