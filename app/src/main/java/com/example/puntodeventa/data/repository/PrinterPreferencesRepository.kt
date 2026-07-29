package com.example.puntodeventa.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.puntodeventa.data.model.PrinterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** One process-wide Preferences DataStore dedicated to printer configuration. */
private val Context.printerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "printer_preferences"
)

/** Persists the configured LAN printer collection as JSON in Preferences DataStore. */
class PrinterPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.printerDataStore
    private val legacyPrefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        LEGACY_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val printerListSerializer = ListSerializer(PrinterConfig.serializer())
    private val printersKey = stringPreferencesKey(KEY_PRINTERS)

    /**
     * Synchronous compatibility API. DataStore I/O runs on [Dispatchers.IO], while the caller waits
     * for completion so existing consumers do not need to change to suspend APIs.
     */
    fun getPrinters(): List<PrinterConfig> = synchronized(ACCESS_LOCK) {
        runBlocking(Dispatchers.IO) { readPrintersOrRecover() }
    }

    /** Replaces the complete printer collection, which is the DataStore source of truth. */
    fun savePrinters(printers: List<PrinterConfig>) = synchronized(ACCESS_LOCK) {
        runBlocking(Dispatchers.IO) { writePrinters(printers) }
    }

    /** Inserts [printer], or replaces the printer with the same stable ID in place. */
    fun upsertPrinter(printer: PrinterConfig) = synchronized(ACCESS_LOCK) {
        runBlocking(Dispatchers.IO) {
            val printers = readPrintersOrRecover().toMutableList()
            val index = printers.indexOfFirst { it.id == printer.id }
            if (index >= 0) printers[index] = printer else printers += printer
            writePrinters(printers)
        }
    }

    /** Activates or deactivates the printer identified by [printerId]. */
    fun setPrinterActive(printerId: String, isActive: Boolean) = synchronized(ACCESS_LOCK) {
        runBlocking(Dispatchers.IO) {
            val printers = readPrintersOrRecover()
            if (printers.any { it.id == printerId }) {
                writePrinters(printers.map {
                    if (it.id == printerId) it.copy(isActive = isActive) else it
                })
            }
        }
    }

    /** Legacy compatibility API backed exclusively by the DataStore printer collection. */
    fun getIpAddress(): String = synchronized(ACCESS_LOCK) {
        runBlocking(Dispatchers.IO) {
            val printers = readPrintersOrRecover()
            printers.firstOrNull { it.isActive }?.ipAddress
                ?: printers.firstOrNull()?.ipAddress
                ?: DEFAULT_IP
        }
    }

    /**
     * Legacy compatibility API. Updates the active (or first) printer in the DataStore collection.
     */
    fun saveIpAddress(ipAddress: String) = synchronized(ACCESS_LOCK) {
        runBlocking(Dispatchers.IO) {
            val printers = readPrintersOrRecover()
            val target = printers.firstOrNull { it.isActive } ?: printers.firstOrNull()
            val updated = if (target == null) {
                listOf(PrinterConfig.default(ipAddress))
            } else {
                printers.map { if (it.id == target.id) it.copy(ipAddress = ipAddress) else it }
            }
            writePrinters(updated)
        }
    }

    /** Reads once, then recovers/migrates and writes without recursively calling a public API. */
    private suspend fun readPrintersOrRecover(): List<PrinterConfig> {
        val storedJson = dataStore.data.first()[printersKey]
        decodePrinters(storedJson)?.let { return it }

        val recovered = if (storedJson == null) migrateLegacyPrinters() else initialPrinters()
        writePrinters(recovered)
        return recovered
    }

    /** Legacy priority: printers_json, then ip_address, then the stable default IP. */
    private fun migrateLegacyPrinters(): List<PrinterConfig> {
        val legacyJson = legacyPrefs.getString(LEGACY_KEY_PRINTERS, null)
        return decodePrinters(legacyJson) ?: initialPrinters()
    }

    private fun initialPrinters(): List<PrinterConfig> {
        val legacyIp = legacyPrefs.getString(LEGACY_KEY_IP_ADDRESS, null)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_IP
        return listOf(PrinterConfig.default(legacyIp))
    }

    private fun decodePrinters(encoded: String?): List<PrinterConfig>? = encoded?.let {
        runCatching { json.decodeFromString(printerListSerializer, it) }.getOrNull()
    }

    private suspend fun writePrinters(printers: List<PrinterConfig>) {
        val encoded = json.encodeToString(printerListSerializer, printers)
        dataStore.edit { preferences -> preferences[printersKey] = encoded }
    }

    companion object {
        private val ACCESS_LOCK = Any()
        private const val LEGACY_PREFS_NAME = "printer_config"
        private const val KEY_PRINTERS = "printers_json"
        private const val LEGACY_KEY_PRINTERS = "printers_json"
        private const val LEGACY_KEY_IP_ADDRESS = "ip_address"
        private const val DEFAULT_IP = "192.168.1.248"
    }
}
