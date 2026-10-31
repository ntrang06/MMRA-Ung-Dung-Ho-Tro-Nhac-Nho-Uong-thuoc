package com.example.project_lttbd.data.device

import android.content.Context
import java.util.UUID

object DeviceIdProvider {
    private const val PREFS_NAME = "mmra_device_preferences"
    private const val KEY_DEVICE_ID = "device_installation_id"

    @Volatile
    private var cachedDeviceId: String? = null

    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { return it }

        synchronized(this) {
            cachedDeviceId?.let { return it }

            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var id = prefs.getString(KEY_DEVICE_ID, null)

            if (id.isNullOrBlank() || !isValidDeviceId(id)) {
                id = generateNewDeviceId()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }

            cachedDeviceId = id
            return id
        }
    }

    internal fun generateNewDeviceId(): String {
        return UUID.randomUUID().toString()
    }

    internal fun isValidDeviceId(id: String): Boolean {
        if (id.isBlank() || '/' in id) return false
        return try {
            UUID.fromString(id)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
