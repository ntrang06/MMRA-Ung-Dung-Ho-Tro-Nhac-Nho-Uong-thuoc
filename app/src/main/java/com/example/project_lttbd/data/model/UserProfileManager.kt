package com.example.project_lttbd.data.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.project_lttbd.data.remote.AuthRepository
import com.example.project_lttbd.data.remote.UserProfileRepository
import com.example.project_lttbd.data.remote.dto.UserProfileDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object UserProfileManager {

    private const val PREFS_NAME = "mmra_user_profile"
    private const val DEFAULT_NAME = "Người dùng"
    private const val DEFAULT_ROLE = "Người uống thuốc"
    private const val TAG = "UserProfileManager"

    private const val KEY_READY = "uid_cache_ready"
    private const val KEY_HAS_CUSTOM_AVATAR = "has_custom_avatar"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PHONE = "user_phone"
    private const val KEY_ROLE = "user_role"
    private const val KEY_LARGE_TEXT = "is_large_text"
    private const val KEY_VOICE_GUIDANCE = "is_voice_guidance"
    private const val KEY_VOICE_VOLUME = "voice_volume"
    private const val KEY_MED_REMINDERS = "is_med_reminders"
    private const val KEY_FAMILY_ALERTS = "is_family_alerts"
    private const val KEY_NOTIFICATIONS_ENABLED = "is_notifications_enabled"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_SETUP_COMPLETED = "is_setup_completed"

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var loadJob: Job? = null
    private var activeUid by mutableStateOf<String?>(null)
    private var initialized = false
    private var revision = 0L

    var name by mutableStateOf(DEFAULT_NAME)
    var role by mutableStateOf(DEFAULT_ROLE)
    var email by mutableStateOf("")
    var phone by mutableStateOf("")

    var isLargeTextEnabled by mutableStateOf(true)
    var isVoiceGuidanceEnabled by mutableStateOf(true)
    var voiceVolume by mutableFloatStateOf(0.7f)
    var isMedicationReminderEnabled by mutableStateOf(true)
    var isFamilyAlertEnabled by mutableStateOf(true)
    var isNotificationsEnabled by mutableStateOf(true)

    var currentAvatarBitmap by mutableStateOf<Bitmap?>(null)
    var tempCapturedBitmap by mutableStateOf<Bitmap?>(null)
    var tempSelectedUri by mutableStateOf<Uri?>(null)

    private fun storageKey(uid: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(uid.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun prefs(
        context: Context,
        uid: String
    ): SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "${PREFS_NAME}_${storageKey(uid)}",
            Context.MODE_PRIVATE
        )

    private fun avatarFile(
        context: Context,
        uid: String
    ): File =
        File(
            context.applicationContext.filesDir,
            "user_avatar_${storageKey(uid)}.jpg"
        )

    private fun resetMemory() {
        name = DEFAULT_NAME
        role = DEFAULT_ROLE
        email = ""
        phone = ""

        isLargeTextEnabled = true
        isVoiceGuidanceEnabled = true
        voiceVolume = 0.7f
        isMedicationReminderEnabled = true
        isFamilyAlertEnabled = true
        isNotificationsEnabled = true

        currentAvatarBitmap = null
        tempCapturedBitmap = null
        tempSelectedUri = null
    }

    // Chuyển dữ liệu cũ khi email khớp tài khoản Firebase hiện tại.
    // Ảnh dùng chung trước đây không ghi chủ sở hữu nên cần chọn lại.
    private fun migrateLegacy(
        context: Context,
        uid: String
    ) {
        val target = prefs(context, uid)

        if (target.getBoolean(KEY_READY, false)) return

        val legacy = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val authEmail = AuthRepository.currentUser?.email.orEmpty()
        val savedEmail = legacy.getString(KEY_EMAIL, "").orEmpty()
        val editor = target.edit()

        if (
            authEmail.isNotBlank() &&
            authEmail.equals(savedEmail, ignoreCase = true)
        ) {
            val allowedKeys = setOf(
                KEY_NAME,
                KEY_EMAIL,
                KEY_PHONE,
                KEY_LARGE_TEXT,
                KEY_VOICE_GUIDANCE,
                KEY_VOICE_VOLUME,
                KEY_MED_REMINDERS,
                KEY_FAMILY_ALERTS,
                KEY_NOTIFICATIONS_ENABLED,
                KEY_IS_LOGGED_IN,
                KEY_SETUP_COMPLETED
            )

            legacy.all
                .filterKeys { it in allowedKeys }
                .forEach { (key, value) ->
                    when (value) {
                        is String -> editor.putString(
                            key,
                            if (key == KEY_PHONE && '•' in value) {
                                ""
                            } else {
                                value
                            }
                        )

                        is Boolean -> editor.putBoolean(key, value)
                        is Float -> editor.putFloat(key, value)
                    }
                }
        }

        editor.putBoolean(KEY_READY, true).apply()
    }

    private fun bindCurrentAccount(context: Context): Boolean {
        val user = AuthRepository.currentUser

        if (initialized && activeUid == user?.uid) {
            return false
        }

        loadJob?.cancel()
        revision++

        activeUid = user?.uid
        initialized = true

        resetMemory()

        if (user == null) return true

        migrateLegacy(context, user.uid)

        val saved = prefs(context, user.uid)

        name = saved.getString(
            KEY_NAME,
            user.displayName ?: DEFAULT_NAME
        )
            .orEmpty()
            .trim()
            .ifBlank { DEFAULT_NAME }

        email = saved.getString(
            KEY_EMAIL,
            user.email.orEmpty()
        ).orEmpty()

        phone = saved.getString(
            KEY_PHONE,
            user.phoneNumber.orEmpty()
        ).orEmpty()

        role = saved.getString(
            KEY_ROLE,
            DEFAULT_ROLE
        ).orEmpty().ifBlank { DEFAULT_ROLE }

        isLargeTextEnabled = saved.getBoolean(KEY_LARGE_TEXT, true)
        isVoiceGuidanceEnabled = saved.getBoolean(KEY_VOICE_GUIDANCE, true)
        voiceVolume = saved.getFloat(KEY_VOICE_VOLUME, 0.7f)
        isMedicationReminderEnabled = saved.getBoolean(KEY_MED_REMINDERS, true)
        isFamilyAlertEnabled = saved.getBoolean(KEY_FAMILY_ALERTS, true)
        isNotificationsEnabled = saved.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

        val file = avatarFile(context, user.uid)

        if (
            saved.getBoolean(KEY_HAS_CUSTOM_AVATAR, false) &&
            file.isFile
        ) {
            currentAvatarBitmap = BitmapFactory.decodeFile(
                file.absolutePath
            )
        }

        return true
    }

    fun init(context: Context) {
        if (!bindCurrentAccount(context)) return

        val uid = activeUid ?: return
        val expectedRevision = revision
        val appContext = context.applicationContext

        loadJob = scope.launch {
            try {
                val profile = UserProfileRepository
                    .getProfile(uid)
                    ?: return@launch

                if (
                    AuthRepository.currentUid == uid &&
                    activeUid == uid &&
                    revision == expectedRevision
                ) {
                    acceptProfile(appContext, uid, profile)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(
                    TAG,
                    "Chưa tải được hồ sơ. Đang dùng dữ liệu đã lưu.",
                    error
                )
            }
        }
    }

    private fun acceptProfile(
        context: Context,
        uid: String,
        profile: UserProfileDto
    ) {
        name = profile.displayName
            .trim()
            .ifBlank { DEFAULT_NAME }

        email = profile.email.ifBlank {
            AuthRepository.currentUser?.email.orEmpty()
        }

        val modes = profile.usageMode
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .toSet()

        role = when {
            "PATIENT" in modes && "CAREGIVER" in modes ->
                "Cả bản thân và người thân"

            "CAREGIVER" in modes ->
                "Người chăm sóc"

            else -> DEFAULT_ROLE
        }

        persistLocal(context, uid)

        prefs(context, uid).edit()
            .putBoolean(
                KEY_SETUP_COMPLETED,
                !profile.usageMode.isNullOrBlank()
            )
            .apply()
    }

    fun applyProfile(
        context: Context,
        uid: String,
        profile: UserProfileDto
    ) {
        check(AuthRepository.currentUid == uid) {
            "Tài khoản đã thay đổi. Vui lòng đăng nhập lại."
        }

        bindCurrentAccount(context)

        loadJob?.cancel()
        revision++

        acceptProfile(
            context.applicationContext,
            uid,
            profile
        )
    }

    private fun persistLocal(
        context: Context,
        uid: String
    ) {
        prefs(context, uid).edit()
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHONE, phone)
            .putString(KEY_ROLE, role)
            .putBoolean(KEY_LARGE_TEXT, isLargeTextEnabled)
            .putBoolean(KEY_VOICE_GUIDANCE, isVoiceGuidanceEnabled)
            .putFloat(KEY_VOICE_VOLUME, voiceVolume)
            .putBoolean(KEY_MED_REMINDERS, isMedicationReminderEnabled)
            .putBoolean(KEY_FAMILY_ALERTS, isFamilyAlertEnabled)
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, isNotificationsEnabled)
            .apply()
    }

    @Suppress("UNUSED_PARAMETER")
    fun isLoggedIn(context: Context): Boolean =
        AuthRepository.currentUser != null

    fun setLoggedIn(
        context: Context,
        loggedIn: Boolean
    ) {
        bindCurrentAccount(context)

        val uid = activeUid ?: return

        prefs(context, uid).edit()
            .putBoolean(KEY_IS_LOGGED_IN, loggedIn)
            .apply()
    }

    fun isSetupCompleted(context: Context): Boolean {
        bindCurrentAccount(context)

        val uid = activeUid ?: return false

        return prefs(context, uid).getBoolean(
            KEY_SETUP_COMPLETED,
            false
        )
    }

    fun setSetupCompleted(
        context: Context,
        completed: Boolean
    ) {
        bindCurrentAccount(context)

        val uid = activeUid ?: return

        loadJob?.cancel()
        revision++

        prefs(context, uid).edit()
            .putBoolean(KEY_SETUP_COMPLETED, completed)
            .apply()
    }

    fun saveProfileInfo(context: Context) {
        val uid = activeUid ?: return

        if (AuthRepository.currentUid != uid) {
            bindCurrentAccount(context)
            return
        }

        loadJob?.cancel()
        revision++

        persistLocal(context, uid)

        // Chụp dữ liệu ngay để tác vụ chạy sau
        // không đọc nhầm thông tin của tài khoản mới.
        val updates = mapOf(
            "displayName" to name,
            "email" to email
        )

        val expectedRevision = revision

        scope.launch {
            try {
                if (
                    AuthRepository.currentUid == uid &&
                    activeUid == uid &&
                    revision == expectedRevision
                ) {
                    UserProfileRepository.updateProfile(
                        uid,
                        updates
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(
                    TAG,
                    "Chưa đồng bộ được thay đổi hồ sơ.",
                    error
                )
            }
        }
    }

    fun hasCustomAvatar(context: Context): Boolean {
        bindCurrentAccount(context)

        val uid = activeUid ?: return false

        return prefs(context, uid).getBoolean(
            KEY_HAS_CUSTOM_AVATAR,
            false
        ) && avatarFile(context, uid).isFile
    }

    fun saveAvatarBitmap(
        context: Context,
        bitmap: Bitmap
    ): Boolean {
        bindCurrentAccount(context)

        val uid = activeUid ?: return false

        return try {
            val file = avatarFile(context, uid)

            val temporary = File(
                file.parentFile,
                file.name + ".tmp"
            )

            FileOutputStream(temporary).use { output ->
                check(
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        90,
                        output
                    )
                )
            }

            check(temporary.renameTo(file))

            prefs(context, uid).edit()
                .putBoolean(KEY_HAS_CUSTOM_AVATAR, true)
                .apply()

            currentAvatarBitmap = bitmap
            tempCapturedBitmap = null
            tempSelectedUri = null

            true
        } catch (error: Exception) {
            Log.w(
                TAG,
                "Chưa lưu được ảnh đại diện.",
                error
            )
            false
        }
    }

    fun deleteAvatar(context: Context): Boolean {
        bindCurrentAccount(context)

        val uid = activeUid ?: return false

        return try {
            val file = avatarFile(context, uid)

            check(!file.exists() || file.delete())

            prefs(context, uid).edit()
                .putBoolean(KEY_HAS_CUSTOM_AVATAR, false)
                .apply()

            currentAvatarBitmap = null
            tempCapturedBitmap = null
            tempSelectedUri = null

            true
        } catch (error: Exception) {
            Log.w(
                TAG,
                "Chưa xóa được ảnh đại diện.",
                error
            )
            false
        }
    }

    fun loadBitmapFromUri(
        context: Context,
        uri: Uri
    ): Bitmap? =
        try {
            context.contentResolver
                .openInputStream(uri)
                ?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
        } catch (error: Exception) {
            Log.w(
                TAG,
                "Chưa đọc được ảnh đã chọn.",
                error
            )
            null
        }

    fun getInitials(): String {
        val parts = name
            .trim()
            .ifBlank { DEFAULT_NAME }
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        return when {
            parts.size >= 2 ->
                "${parts.first().first()}${parts.last().first()}"
                    .uppercase()

            parts.size == 1 ->
                parts[0].take(2).uppercase()

            else -> "ND"
        }
    }

    fun resetForTesting(context: Context) {
        bindCurrentAccount(context)

        val uid = activeUid ?: return

        prefs(context, uid).edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_SETUP_COMPLETED)
            .apply()
    }
}