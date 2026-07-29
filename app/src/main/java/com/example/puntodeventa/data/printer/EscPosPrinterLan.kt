package com.example.puntodeventa.data.printer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset

/**
 * Handles raw ESC/POS printing over TCP to a POS-8360 thermal printer.
 *
 * Connects to the printer on port 9100, sends ESC/POS initialization,
 * text content, and a paper-cut command.
 */
object EscPosPrinterLan {

    private const val PORT = 9100
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val OVERALL_TIMEOUT_MS = 15_000L
    private val CHARSET = Charset.forName("Cp850")

    // ESC/POS command bytes
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)          // Initialize printer
    private val ESC_CUT = byteArrayOf(0x1D, 0x56, 0x00)    // Full cut
    private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10)  // ESC ! n (double height)
    private val ESC_NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00)    // ESC ! n (normal)

    /**
     * Prints [ticketText] to the thermal printer at [ipAddress].
     *
     * @param ipAddress The LAN IP address of the printer (e.g., "192.168.1.100")
     * @param ticketText The pre-formatted ticket text to print
     * @throws Exception if connection fails, times out, or any I/O error occurs
     */
    suspend fun printTicket(ipAddress: String, ticketText: String) {
        withTimeout(OVERALL_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(ipAddress, PORT), CONNECT_TIMEOUT_MS)
                    socket.soTimeout = CONNECT_TIMEOUT_MS

                    val outputStream: OutputStream = socket.getOutputStream()

                    // Initialize printer
                    outputStream.write(ESC_INIT)

                    // Send ticket text as bytes (charset compatible with ESC/POS)
                    outputStream.write(ticketText.toByteArray(CHARSET))

                    // Feed and cut paper
                    outputStream.write("\n\n\n".toByteArray(CHARSET))
                    outputStream.write(ESC_CUT)

                    outputStream.flush()
                } finally {
                    runCatching { socket.close() }
                }
            }
        }
    }

    /**
     * Prints two tickets (client and internal) sequentially over a single TCP connection.
     *
     * Each ticket is preceded by an ESC/POS init command and followed by paper feed + cut.
     *
     * @param ipAddress The LAN IP address of the printer (e.g., "192.168.1.100")
     * @param clientTicketText The pre-formatted client ticket text
     * @param internalTicketText The pre-formatted internal ticket text
     * @throws Exception if connection fails, times out, or any I/O error occurs
     */
    suspend fun printDoubleTicket(ipAddress: String, clientTicketText: String, internalTicketText: String) {
        withTimeout(OVERALL_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(ipAddress, PORT), CONNECT_TIMEOUT_MS)
                    socket.soTimeout = CONNECT_TIMEOUT_MS

                    val outputStream: OutputStream = socket.getOutputStream()

                    // First ticket: client
                    outputStream.write(ESC_INIT)
                    outputStream.write(clientTicketText.toByteArray(CHARSET))
                    outputStream.write("\n\n\n".toByteArray(CHARSET))
                    outputStream.write(ESC_CUT)

                    // Second ticket: internal
                    outputStream.write(ESC_INIT)
                    outputStream.write(internalTicketText.toByteArray(CHARSET))
                    outputStream.write("\n\n\n".toByteArray(CHARSET))
                    outputStream.write(ESC_CUT)

                    outputStream.flush()
                } finally {
                    runCatching { socket.close() }
                }
            }
        }
    }

    /**
     * Prints a standalone internal ticket with double-height text applied to the items section.
     *
     * The ticket is segmented into three parts:
     * - [headerText]: printed in normal size (includes ticket header, column titles, separator)
     * - [itemsText]: printed in double height (product rows, customization lines, notes, divider dashes)
     * - [footerText]: printed in normal size (article count, footer lines)
     *
     * @param ipAddress The LAN IP address of the printer (e.g., "192.168.1.100")
     * @param headerText The header portion of the internal ticket (normal size)
     * @param itemsText The items portion of the internal ticket (double height)
     * @param footerText The footer portion of the internal ticket (normal size)
     * @throws Exception if connection fails, times out, or any I/O error occurs
     */
    suspend fun printInternalTicketWithDoubleHeight(
        ipAddress: String,
        headerText: String,
        itemsText: String,
        footerText: String
    ) {
        withTimeout(OVERALL_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(ipAddress, PORT), CONNECT_TIMEOUT_MS)
                    socket.soTimeout = CONNECT_TIMEOUT_MS

                    val outputStream: OutputStream = socket.getOutputStream()

                    // Initialize printer
                    outputStream.write(ESC_INIT)

                    // Header in normal size
                    outputStream.write(headerText.toByteArray(CHARSET))

                    // Switch to double height for items
                    outputStream.write(ESC_DOUBLE_HEIGHT)
                    outputStream.write(itemsText.toByteArray(CHARSET))

                    // Switch back to normal size for footer
                    outputStream.write(ESC_NORMAL_SIZE)
                    outputStream.write(footerText.toByteArray(CHARSET))

                    // Feed and cut paper
                    outputStream.write("\n\n\n".toByteArray(CHARSET))
                    outputStream.write(ESC_CUT)

                    outputStream.flush()
                } finally {
                    runCatching { socket.close() }
                }
            }
        }
    }

    /**
     * Tests the connection to the thermal printer at [ipAddress] by sending
     * a fixed test message ("Prueba de Conexion Exitosa") and cutting the paper.
     *
     * @param ipAddress The LAN IP address of the printer (e.g., "192.168.1.100")
     * @throws Exception if connection fails, times out, or any I/O error occurs
     */
    suspend fun testConnection(ipAddress: String) {
        withTimeout(OVERALL_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val socket = Socket()
                try {
                    socket.connect(InetSocketAddress(ipAddress, PORT), CONNECT_TIMEOUT_MS)
                    socket.soTimeout = CONNECT_TIMEOUT_MS

                    val outputStream: OutputStream = socket.getOutputStream()

                    // Initialize printer
                    outputStream.write(ESC_INIT)

                    // Send test message
                    outputStream.write("Prueba de Conexion Exitosa".toByteArray(CHARSET))

                    // Feed and cut paper
                    outputStream.write("\n\n\n".toByteArray(CHARSET))
                    outputStream.write(ESC_CUT)

                    outputStream.flush()
                } finally {
                    runCatching { socket.close() }
                }
            }
        }
    }
}
