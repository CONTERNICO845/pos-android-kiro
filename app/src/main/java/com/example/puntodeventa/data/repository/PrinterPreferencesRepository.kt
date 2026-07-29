package com.example.puntodeventa.data.repository

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for persisting printer configuration settings using SharedPreferences.
 *
 * Only the printer's "IP local" is saved and loaded in this phase.
 * Future network settings (port, protocol) may be added here as new key constants.
 */
class PrinterPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the saved printer IP address, or the default IP if none has been saved. */
    fun getIpAddress(): String =
        prefs.getString(KEY_IP_ADDRESS, DEFAULT_IP) ?: DEFAULT_IP

    /**
     * Persists [ipAddress] to SharedPreferences.
     * Writes are applied synchronously so the value is immediately available on the next read.
     */
    fun saveIpAddress(ipAddress: String) {
        prefs.edit()
            .putString(KEY_IP_ADDRESS, ipAddress)
            .apply()
    }

    companion object {
        private const val PREFS_NAME    = "printer_config"
        private const val KEY_IP_ADDRESS = "ip_address"
        private const val DEFAULT_IP     = "192.168.1.248"
    }
}
