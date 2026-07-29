package com.example.puntodeventa.data.printer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.coroutines.coroutineContext

/** Discovers hosts exposing a TCP printer port on the active private IPv4 /24 subnet. */
object LanPrinterDiscovery {
    private const val MAX_CONCURRENCY = 32
    private const val CONNECT_TIMEOUT_MS = 250

    /**
     * Returns numerically sorted IP addresses whose requested TCP [port] accepted a connection.
     * An open port does not imply any particular printer model or protocol.
     */
    suspend fun scan(port: Int = 9100): List<String> = coroutineScope {
        require(port in 1..65535) { "El puerto debe estar entre 1 y 65535" }
        val localAddress = findActivePrivateIpv4() ?: return@coroutineScope emptyList()
        val octets = localAddress.address.map { it.toInt() and 0xFF }
        val prefix = "${octets[0]}.${octets[1]}.${octets[2]}"
        val localHost = octets[3]
        val semaphore = Semaphore(MAX_CONCURRENCY)

        (1..254)
            .filter { it != localHost }
            .map { host ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        coroutineContext.ensureActive()
                        val ipAddress = "$prefix.$host"
                        if (isPortOpen(ipAddress, port)) ipAddress else null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
            .sortedWith(compareBy(::ipv4NumericValue))
    }

    private fun findActivePrivateIpv4(): Inet4Address? =
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .asSequence()
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
            .firstOrNull(::isPrivate)


    private fun isPrivate(address: Inet4Address): Boolean {
        val bytes = address.address.map { it.toInt() and 0xFF }
        return bytes[0] == 10 ||
            (bytes[0] == 172 && bytes[1] in 16..31) ||
            (bytes[0] == 192 && bytes[1] == 168)
    }

    private fun isPortOpen(ipAddress: String, port: Int): Boolean =
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), CONNECT_TIMEOUT_MS)
            }
            true
        }.getOrDefault(false)

    private fun ipv4NumericValue(ipAddress: String): Long =
        ipAddress.split('.').fold(0L) { value, octet ->
            (value shl 8) or octet.toLong()
        }
}
