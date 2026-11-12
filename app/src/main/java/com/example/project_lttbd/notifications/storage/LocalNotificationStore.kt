package com.example.project_lttbd.notifications.storage

import android.content.Context
import com.example.project_lttbd.notifications.model.LocalNotificationItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Quản lý lưu trữ thông báo cục bộ theo UID của người dùng.
 * Đảm bảo dữ liệu giữa các tài khoản hoàn toàn độc lập, không lẫn lộn.
 */
object LocalNotificationStore {

    private val gson = Gson()
    private val lock = Any()

    private fun sanitizeUid(uid: String): String {
        return uid.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    }

    private fun getFileForUid(baseDir: File, uid: String): File {
        val safeUid = sanitizeUid(uid)
        return File(baseDir, "mmra_notifications_$safeUid.json")
    }

    fun getNotifications(context: Context, uid: String): List<LocalNotificationItem> {
        return getNotificationsFromDir(context.filesDir, uid)
    }

    fun addNotification(context: Context, uid: String, item: LocalNotificationItem) {
        addNotificationToDir(context.filesDir, uid, item)
    }

    fun markAsRead(context: Context, uid: String, notificationId: String) {
        markAsReadInDir(context.filesDir, uid, notificationId)
    }

    fun markAllAsRead(context: Context, uid: String) {
        markAllAsReadInDir(context.filesDir, uid)
    }

    fun clearAll(context: Context, uid: String) {
        clearAllInDir(context.filesDir, uid)
    }

    // --- Directory-based methods for testability & decoupling ---

    fun getNotificationsFromDir(baseDir: File, uid: String): List<LocalNotificationItem> {
        if (uid.isBlank()) return emptyList()
        synchronized(lock) {
            val file = getFileForUid(baseDir, uid)
            if (!file.exists()) return emptyList()
            return try {
                val json = file.readText()
                val type = object : TypeToken<List<LocalNotificationItem>>() {}.type
                gson.fromJson<List<LocalNotificationItem>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun addNotificationToDir(baseDir: File, uid: String, item: LocalNotificationItem) {
        if (uid.isBlank()) return
        synchronized(lock) {
            val current = getNotificationsFromDir(baseDir, uid).toMutableList()
            // Loại bỏ trùng lặp id nếu có, đưa thông báo mới lên đầu
            current.removeAll { it.id == item.id }
            current.add(0, item)
            // Giữ tối đa 100 thông báo gần nhất
            val trimmed = if (current.size > 100) current.take(100) else current
            saveNotificationsToDir(baseDir, uid, trimmed)
        }
    }

    fun markAsReadInDir(baseDir: File, uid: String, notificationId: String) {
        if (uid.isBlank() || notificationId.isBlank()) return
        synchronized(lock) {
            val current = getNotificationsFromDir(baseDir, uid).map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            saveNotificationsToDir(baseDir, uid, current)
        }
    }

    fun markAllAsReadInDir(baseDir: File, uid: String) {
        if (uid.isBlank()) return
        synchronized(lock) {
            val current = getNotificationsFromDir(baseDir, uid).map { it.copy(isRead = true) }
            saveNotificationsToDir(baseDir, uid, current)
        }
    }

    fun clearAllInDir(baseDir: File, uid: String) {
        if (uid.isBlank()) return
        synchronized(lock) {
            val file = getFileForUid(baseDir, uid)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun saveNotificationsToDir(baseDir: File, uid: String, items: List<LocalNotificationItem>) {
        val file = getFileForUid(baseDir, uid)
        try {
            if (!baseDir.exists()) baseDir.mkdirs()
            file.writeText(gson.toJson(items))
        } catch (e: Exception) {
            // Log/ignore in production
        }
    }
}
