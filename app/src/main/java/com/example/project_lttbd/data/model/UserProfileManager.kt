package com.example.project_lttbd.data.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.project_lttbd.data.remote.AuthRepository
import com.example.project_lttbd.data.remote.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

object UserProfileManager {

    private const val PREFS_NAME = "mmra_user_profile"

    private const val KEY_HAS_CUSTOM_AVATAR = "has_custom_avatar"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PHONE = "user_phone"

    private const val KEY_LARGE_TEXT = "is_large_text"
    private const val KEY_VOICE_GUIDANCE = "is_voice_guidance"
    private const val KEY_VOICE_VOLUME = "voice_volume"
    private const val KEY_MED_REMINDERS = "is_med_reminders"
    private const val KEY_FAMILY_ALERTS = "is_family_alerts"
    private const val KEY_NOTIFICATIONS_ENABLED = "is_notifications_enabled"

    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_SETUP_COMPLETED = "is_setup_completed"

    private const val AVATAR_FILENAME = "user_avatar.jpg"
    private const val DEFAULT_NAME = "Người dùng"

    var name by mutableStateOf(DEFAULT_NAME)
    var role by mutableStateOf("Người uống thuốc")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")

    var isLargeTextEnabled by mutableStateOf(true)
    var isVoiceGuidanceEnabled by mutableStateOf(true)
    var voiceVolume by mutableFloatStateOf(0.7f)

    var isMedicationReminderEnabled by mutableStateOf(true)
    var isFamilyAlertEnabled by mutableStateOf(true)
    var isNotificationsEnabled by mutableStateOf(true)

    // Ảnh đại diện đang hiển thị.
    var currentAvatarBitmap by mutableStateOf<Bitmap?>(null)

    // Dữ liệu tạm trong quá trình chụp, chọn và cắt ảnh.
    var tempCapturedBitmap by mutableStateOf<Bitmap?>(null)
    var tempSelectedUri by mutableStateOf<Uri?>(null)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        name = prefs.getString(KEY_NAME, DEFAULT_NAME)
            ?.trim()
            ?.ifBlank { DEFAULT_NAME }
            ?: DEFAULT_NAME

        email = prefs.getString(KEY_EMAIL, "").orEmpty()
        phone = prefs.getString(KEY_PHONE, "").orEmpty()

        isLargeTextEnabled = prefs.getBoolean(
            KEY_LARGE_TEXT,
            true
        )

        isVoiceGuidanceEnabled = prefs.getBoolean(
            KEY_VOICE_GUIDANCE,
            true
        )

        voiceVolume = prefs.getFloat(
            KEY_VOICE_VOLUME,
            0.7f
        )

        isMedicationReminderEnabled = prefs.getBoolean(
            KEY_MED_REMINDERS,
            true
        )

        isFamilyAlertEnabled = prefs.getBoolean(
            KEY_FAMILY_ALERTS,
            true
        )

        isNotificationsEnabled = prefs.getBoolean(
            KEY_NOTIFICATIONS_ENABLED,
            true
        )

        currentAvatarBitmap = getAvatarBitmap(context)

        // Đọc thông tin tài khoản Firebase hiện tại.
        val currentUser = AuthRepository.currentUser

        if (currentUser != null) {
            email = currentUser.email ?: email

            val firebaseName = currentUser.displayName

            if (!firebaseName.isNullOrBlank()) {
                name = firebaseName
            }

            // Đồng bộ tên và email từ hồ sơ Firestore.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val profile = UserProfileRepository.getProfile(
                        currentUser.uid
                    )

                    if (profile != null) {
                        if (profile.displayName.isNotBlank()) {
                            name = profile.displayName
                        }

                        if (profile.email.isNotBlank()) {
                            email = profile.email
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        // Phiên Firebase quyết định trạng thái đăng nhập.
        return AuthRepository.currentUser != null
    }

    fun setLoggedIn(
        context: Context,
        loggedIn: Boolean
    ) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, loggedIn)
            .apply()
    }

    fun isSetupCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return prefs.getBoolean(
            KEY_SETUP_COMPLETED,
            false
        )
    }

    fun setSetupCompleted(
        context: Context,
        completed: Boolean
    ) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putBoolean(KEY_SETUP_COMPLETED, completed)
            .apply()
    }

    fun saveProfileInfo(context: Context) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHONE, phone)
            .putBoolean(KEY_LARGE_TEXT, isLargeTextEnabled)
            .putBoolean(KEY_VOICE_GUIDANCE, isVoiceGuidanceEnabled)
            .putFloat(KEY_VOICE_VOLUME, voiceVolume)
            .putBoolean(KEY_MED_REMINDERS, isMedicationReminderEnabled)
            .putBoolean(KEY_FAMILY_ALERTS, isFamilyAlertEnabled)
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, isNotificationsEnabled)
            .apply()

        // Cập nhật hồ sơ Firestore khi đang đăng nhập.
        val uid = AuthRepository.currentUid

        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    UserProfileRepository.updateProfile(
                        uid,
                        mapOf(
                            "displayName" to name,
                            "email" to email
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun hasCustomAvatar(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val file = File(
            context.filesDir,
            AVATAR_FILENAME
        )

        return prefs.getBoolean(
            KEY_HAS_CUSTOM_AVATAR,
            false
        ) && file.exists()
    }

    fun saveAvatarBitmap(
        context: Context,
        bitmap: Bitmap
    ): Boolean {
        return try {
            val file = File(
                context.filesDir,
                AVATAR_FILENAME
            )

            FileOutputStream(file).use { out ->
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    out
                )
            }

            val prefs = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            prefs.edit()
                .putBoolean(KEY_HAS_CUSTOM_AVATAR, true)
                .apply()

            currentAvatarBitmap = bitmap

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteAvatar(context: Context): Boolean {
        return try {
            val file = File(
                context.filesDir,
                AVATAR_FILENAME
            )

            if (file.exists()) {
                file.delete()
            }

            val prefs = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            prefs.edit()
                .putBoolean(KEY_HAS_CUSTOM_AVATAR, false)
                .apply()

            currentAvatarBitmap = null

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getAvatarBitmap(context: Context): Bitmap? {
        if (!hasCustomAvatar(context)) {
            return null
        }

        return try {
            val file = File(
                context.filesDir,
                AVATAR_FILENAME
            )

            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadBitmapFromUri(
        context: Context,
        uri: Uri
    ): Bitmap? {
        return try {
            context.contentResolver
                .openInputStream(uri)
                ?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getInitials(): String {
        val targetName = name
            .trim()
            .ifBlank { DEFAULT_NAME }

        val parts = targetName
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        return when {
            parts.size >= 2 -> {
                "${parts.first().first()}${parts.last().first()}"
                    .uppercase()
            }

            parts.size == 1 -> {
                parts[0]
                    .take(2)
                    .uppercase()
            }

            else -> "ND"
        }
    }

    fun resetForTesting(context: Context) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_SETUP_COMPLETED)
            .apply()
    }
}