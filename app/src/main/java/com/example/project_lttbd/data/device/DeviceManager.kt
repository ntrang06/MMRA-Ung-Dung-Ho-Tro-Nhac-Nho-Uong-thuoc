package com.example.project_lttbd.data.device

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.dto.DeviceDto
import com.google.firebase.Timestamp

object DeviceManager {
    private const val TAG = "DeviceManager"

    fun createDeviceDto(
        deviceId: String,
        fcmToken: String,
        notificationsEnabled: Boolean
    ): DeviceDto {
        return DeviceDto(
            deviceId = deviceId,
            fcmToken = fcmToken,
            platform = "android",
            notificationsEnabled = notificationsEnabled,
            updatedAt = Timestamp.now()
        )
    }

    fun syncCurrentDevice(
        context: Context,
        uid: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (uid.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val deviceId = DeviceIdProvider.getDeviceId(context)
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        FirebaseProvider.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                onComplete?.invoke(false)
                return@addOnCompleteListener
            }

            val token = task.result.orEmpty()
            val dto = createDeviceDto(
                deviceId = deviceId,
                fcmToken = token,
                notificationsEnabled = notificationsEnabled
            )

            FirebaseProvider.devicesCollection(uid)
                .document(deviceId)
                .set(dto)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced device $deviceId for user $uid")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync device to Firestore", e)
                    onComplete?.invoke(false)
                }
        }
    }

    fun updateFcmToken(
        uid: String,
        deviceId: String,
        token: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (uid.isBlank() || deviceId.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val updates = mapOf(
            "fcmToken" to token,
            "updatedAt" to Timestamp.now()
        )

        FirebaseProvider.devicesCollection(uid)
            .document(deviceId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated for device $deviceId")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update FCM token", e)
                onComplete?.invoke(false)
            }
    }

    fun removeDevice(
        context: Context,
        uid: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (uid.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val deviceId = DeviceIdProvider.getDeviceId(context)
        FirebaseProvider.devicesCollection(uid)
            .document(deviceId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Device $deviceId removed for user $uid on sign out")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Could not delete device doc during sign out", e)
                onComplete?.invoke(false)
            }
    }
}
