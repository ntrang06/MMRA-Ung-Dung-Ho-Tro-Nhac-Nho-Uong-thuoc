package com.example.project_lttbd.data.device

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.dto.DeviceDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

object DeviceManager {

    private const val TAG = "DeviceManager"
    private val stateLock = Any()

    private var sessionUid: String? = null
    private var generation = 0L
    private var removalBlocked = false
    private var pendingRemovalTask: Task<Void>? = null

    private val auth by lazy {
        FirebaseProvider.auth.also { firebaseAuth ->
            firebaseAuth.addAuthStateListener { changedAuth ->
                synchronized(stateLock) {
                    observeUidLocked(changedAuth.currentUser?.uid)
                }
            }
        }
    }

    private fun observeUidLocked(uid: String?) {
        if (uid != sessionUid) {
            sessionUid = uid
            generation++
            removalBlocked = false
            pendingRemovalTask = null
        }
    }

    private fun captureGeneration(uid: String): Long? {
        if (uid.isBlank() || '/' in uid) return null

        return synchronized(stateLock) {
            observeUidLocked(auth.currentUser?.uid)

            if (sessionUid != uid || removalBlocked) {
                null
            } else {
                generation
            }
        }
    }

    private fun isCurrentLocked(
        uid: String,
        version: Long
    ): Boolean {
        observeUidLocked(auth.currentUser?.uid)

        return sessionUid == uid &&
                generation == version &&
                !removalBlocked
    }

    fun createDeviceDto(
        deviceId: String,
        fcmToken: String,
        notificationsEnabled: Boolean
    ): DeviceDto =
        DeviceDto(
            deviceId = deviceId,
            fcmToken = fcmToken,
            platform = "android",
            notificationsEnabled = notificationsEnabled
        )

    fun syncCurrentDevice(
        context: Context,
        uid: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val version = captureGeneration(uid)

        if (version == null) {
            onComplete?.invoke(false)
            return
        }

        val appContext = context.applicationContext

        val deviceId = DeviceIdProvider.getDeviceId(
            appContext
        )

        val enabled = NotificationManagerCompat
            .from(appContext)
            .areNotificationsEnabled()

        FirebaseProvider.messaging.token.addOnCompleteListener { task ->
            val token = if (task.isSuccessful) {
                task.result.orEmpty()
            } else {
                ""
            }

            if (token.isBlank()) {
                Log.w(
                    TAG,
                    "Chưa lấy được token FCM.",
                    task.exception
                )

                onComplete?.invoke(false)
                return@addOnCompleteListener
            }

            val dto = createDeviceDto(
                deviceId,
                token,
                enabled
            )

            val fields = mapOf(
                "deviceId" to dto.deviceId,
                "fcmToken" to dto.fcmToken,
                "platform" to dto.platform,
                "notificationsEnabled" to dto.notificationsEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val writeTask = synchronized(stateLock) {
                if (isCurrentLocked(uid, version)) {
                    FirebaseProvider.devicesCollection(uid)
                        .document(deviceId)
                        .set(fields, SetOptions.merge())
                } else {
                    null
                }
            }

            if (writeTask == null) {
                onComplete?.invoke(false)
                return@addOnCompleteListener
            }

            writeTask
                .addOnSuccessListener {
                    val stillCurrent = synchronized(stateLock) {
                        isCurrentLocked(uid, version)
                    }

                    onComplete?.invoke(stillCurrent)
                }
                .addOnFailureListener { error ->
                    Log.w(
                        TAG,
                        "Chưa đồng bộ được thiết bị.",
                        error
                    )

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
        val version = captureGeneration(uid)

        if (
            version == null ||
            deviceId.isBlank() ||
            '/' in deviceId ||
            token.isBlank()
        ) {
            onComplete?.invoke(false)
            return
        }

        val updates = mapOf(
            "fcmToken" to token,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val writeTask = synchronized(stateLock) {
            if (isCurrentLocked(uid, version)) {
                FirebaseProvider.devicesCollection(uid)
                    .document(deviceId)
                    .update(updates)
            } else {
                null
            }
        }

        if (writeTask == null) {
            onComplete?.invoke(false)
            return
        }

        writeTask
            .addOnSuccessListener {
                val stillCurrent = synchronized(stateLock) {
                    isCurrentLocked(uid, version)
                }

                onComplete?.invoke(stillCurrent)
            }
            .addOnFailureListener { error ->
                Log.w(
                    TAG,
                    "Chưa cập nhật được token FCM.",
                    error
                )

                onComplete?.invoke(false)
            }
    }

    fun removeDevice(
        context: Context,
        uid: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (uid.isBlank() || '/' in uid) {
            onComplete?.invoke(false)
            return
        }

        val deviceId = DeviceIdProvider.getDeviceId(
            context.applicationContext
        )

        val deleteTask = synchronized(stateLock) {
            observeUidLocked(auth.currentUser?.uid)

            if (sessionUid != uid) {
                null
            } else {
                // Lần thử lại nhận kết quả của tác vụ gỡ đang có.
                pendingRemovalTask ?: run {
                    val task = FirebaseProvider.devicesCollection(uid)
                        .document(deviceId)
                        .delete()

                    generation++
                    removalBlocked = true
                    pendingRemovalTask = task

                    task
                }
            }
        }

        if (deleteTask == null) {
            onComplete?.invoke(false)
            return
        }

        deleteTask
            .addOnSuccessListener {
                // Giữ chặn đồng bộ đến khi Firebase kết thúc phiên.
                onComplete?.invoke(true)
            }
            .addOnFailureListener { error ->
                synchronized(stateLock) {
                    observeUidLocked(auth.currentUser?.uid)

                    if (
                        sessionUid == uid &&
                        pendingRemovalTask === deleteTask
                    ) {
                        pendingRemovalTask = null
                        removalBlocked = false
                    }
                }

                Log.w(
                    TAG,
                    "Chưa gỡ được liên kết thiết bị.",
                    error
                )

                onComplete?.invoke(false)
            }
    }
}