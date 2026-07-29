package com.example.puntodeventa.data.printer

import com.example.puntodeventa.data.model.PrinterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset

/** Sends ESC/POS jobs over a raw TCP connection to a configured LAN printer. */
object EscPosPrinterLan {
    private const val DEFAULT_PORT = 9100
    private const val DEFAULT_PAPER_SIZE = 80
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val OVERALL_TIMEOUT_MS = 15_000L
    private const val ESC_POS_PROTOCOL = "ESC/POS"
    private val CHARSET = Charset.forName("Cp850")

    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ESC_CUT = byteArrayOf(0x1D, 0x56, 0x00)
    private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10)
    private val ESC_NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00)
    private val PAPER_FEED = "\n\n\n".toByteArray(CHARSET)

    /** Legacy API: prints at port 9100 on 80 mm paper and cuts. */
    suspend fun printTicket(ipAddress: String, ticketText: String) =
        printTicket(ipAddress, ticketText, DEFAULT_PORT, DEFAULT_PAPER_SIZE, autoCut = true)

    /** Prints one ticket using explicit network and paper settings. */
    suspend fun printTicket(
        ipAddress: String,
        ticketText: String,
        port: Int,
        paperSize: Int,
        autoCut: Boolean
    ) = withPrinter(ipAddress, port) { output ->
        writeNormalTicket(output, fitToPaper(ticketText, paperSize), autoCut)
    }

    /** Legacy API: prints both tickets through one connection using historical defaults. */
    suspend fun printDoubleTicket(
        ipAddress: String,
        clientTicketText: String,
        internalTicketText: String
    ) = printDoubleTicket(
        ipAddress, clientTicketText, internalTicketText,
        DEFAULT_PORT, DEFAULT_PAPER_SIZE, autoCut = true
    )


    /** Prints two normal-size tickets with explicit settings over one TCP connection. */
    suspend fun printDoubleTicket(
        ipAddress: String,
        clientTicketText: String,
        internalTicketText: String,
        port: Int,
        paperSize: Int,
        autoCut: Boolean
    ) = withPrinter(ipAddress, port) { output ->
        writeNormalTicket(output, fitToPaper(clientTicketText, paperSize), autoCut)
        writeNormalTicket(output, fitToPaper(internalTicketText, paperSize), autoCut)
    }

    /** Legacy API: prints the internal items in double height with historical defaults. */
    suspend fun printInternalTicketWithDoubleHeight(
        ipAddress: String,
        headerText: String,
        itemsText: String,
        footerText: String
    ) = printInternalTicketWithDoubleHeight(
        ipAddress, headerText, itemsText, footerText,
        DEFAULT_PORT, DEFAULT_PAPER_SIZE, autoCut = true
    )

    /** Prints a segmented internal ticket with double-height items. */
    suspend fun printInternalTicketWithDoubleHeight(
        ipAddress: String,
        headerText: String,
        itemsText: String,
        footerText: String,
        port: Int,
        paperSize: Int,
        autoCut: Boolean
    ) = withPrinter(ipAddress, port) { output ->
        writeInternalTicket(
            output,
            fitToPaper(headerText, paperSize),
            fitToPaper(itemsText, paperSize),
            fitToPaper(footerText, paperSize),
            autoCut
        )
    }

    /**
     * Prints the client and internal tickets through exactly one connection to [printer].
     * The internal item section uses double-height text.
     */
    suspend fun printOrder(
        printer: PrinterConfig,
        clientTicketText: String,
        internalHeader: String,
        internalItems: String,
        internalFooter: String
    ) {
        requireEscPos(printer)
        withPrinter(printer.ipAddress, printer.port) { output ->
            writeNormalTicket(
                output,
                fitToPaper(clientTicketText, printer.paperSize),
                printer.autoCut
            )
            writeInternalTicket(
                output,
                fitToPaper(internalHeader, printer.paperSize),
                fitToPaper(internalItems, printer.paperSize),
                fitToPaper(internalFooter, printer.paperSize),
                printer.autoCut
            )
        }
    }

    /** Legacy API: tests port 9100 on 80 mm paper and cuts. */
    suspend fun testConnection(ipAddress: String) =
        testConnection(ipAddress, DEFAULT_PORT, DEFAULT_PAPER_SIZE, autoCut = true)

    /** Tests a printer endpoint using explicit settings. */
    suspend fun testConnection(
        ipAddress: String,
        port: Int,
        paperSize: Int,
        autoCut: Boolean
    ) = printTicket(
        ipAddress,
        "Prueba de Conexion Exitosa",
        port,
        paperSize,
        autoCut
    )

    /** Tests the configured printer and rejects unsupported protocols before connecting. */
    suspend fun testConfiguredPrinter(printer: PrinterConfig) {
        requireEscPos(printer)
        testConnection(printer.ipAddress, printer.port, printer.paperSize, printer.autoCut)
    }


    private suspend fun withPrinter(
        ipAddress: String,
        port: Int,
        block: (OutputStream) -> Unit
    ) {
        require(ipAddress.isNotBlank()) { "La dirección IP de la impresora está vacía" }
        require(port in 1..65535) { "El puerto debe estar entre 1 y 65535" }
        withTimeout(OVERALL_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(ipAddress, port), CONNECT_TIMEOUT_MS)
                    socket.soTimeout = CONNECT_TIMEOUT_MS
                    socket.getOutputStream().use { output ->
                        block(output)
                        output.flush()
                    }
                } finally {
                    runCatching { socket.close() }
                }
            }
        }
    }

    private fun writeNormalTicket(output: OutputStream, text: String, autoCut: Boolean) {
        output.write(ESC_INIT)
        output.write(text.toByteArray(CHARSET))
        finishTicket(output, autoCut)
    }

    private fun writeInternalTicket(
        output: OutputStream,
        header: String,
        items: String,
        footer: String,
        autoCut: Boolean
    ) {
        output.write(ESC_INIT)
        output.write(header.toByteArray(CHARSET))
        output.write(ESC_DOUBLE_HEIGHT)
        output.write(items.toByteArray(CHARSET))
        output.write(ESC_NORMAL_SIZE)
        output.write(footer.toByteArray(CHARSET))
        finishTicket(output, autoCut)
    }

    private fun finishTicket(output: OutputStream, autoCut: Boolean) {
        output.write(PAPER_FEED)
        if (autoCut) output.write(ESC_CUT)
    }

    private fun fitToPaper(text: String, paperSize: Int): String {
        if (paperSize == 80) return text // Preserve the established 48-column output byte-for-byte.
        require(paperSize == 58) { "Tamaño de papel no compatible: $paperSize mm. Use 58 u 80 mm." }

        return text.split('\n').flatMap { rawLine ->
            val line = rawLine.removeSuffix("\r")
            if (line.isEmpty()) listOf("") else line.chunked(32)
        }.joinToString("\n")
    }

    private fun requireEscPos(printer: PrinterConfig) {
        require(printer.protocol.trim().equals(ESC_POS_PROTOCOL, ignoreCase = true)) {
            "Protocolo no compatible: ${printer.protocol}. Solo se admite ESC/POS."
        }
    }
}
