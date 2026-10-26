package com.example.mmra_medicationmanager.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream

object UserProfileManager {
    private const val PREFS_NAME = "mmra_user_profile"
    private const val KEY_HAS_CUSTOM_AVATAR = "has_custom_avatar"
    private const val AVATAR_FILENAME = "user_avatar.jpg"

    var name by mutableStateOf("Nguyễn Thị Lan")
    var role by mutableStateOf("Người uống thuốc")
    var email by mutableStateOf("lan.n•••••@gmail.com")
    var phone by mutableStateOf("0912 ••• 678")
    
    // In-memory state for current avatar Bitmap (reloaded on launch/save/delete)
    var currentAvatarBitmap by mutableStateOf<Bitmap?>(null)
    
    // Temporary Bitmaps for the flow: Capture -> Review -> Crop
    var tempCapturedBitmap by mutableStateOf<Bitmap?>(null)
    var tempSelectedUri by mutableStateOf<Uri?>(null)

    fun init(context: Context) {
        currentAvatarBitmap = getAvatarBitmap(context)
    }

    fun hasCustomAvatar(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val file = File(context.filesDir, AVATAR_FILENAME)
        return prefs.getBoolean(KEY_HAS_CUSTOM_AVATAR, false) && file.exists()
    }

    fun saveAvatarBitmap(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val file = File(context.filesDir, AVATAR_FILENAME)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_HAS_CUSTOM_AVATAR, true).apply()
            currentAvatarBitmap = bitmap
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteAvatar(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, AVATAR_FILENAME)
            if (file.exists()) {
                file.delete()
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_HAS_CUSTOM_AVATAR, false).apply()
            currentAvatarBitmap = null
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getAvatarBitmap(context: Context): Bitmap? {
        if (!hasCustomAvatar(context)) return null
        return try {
            val file = File(context.filesDir, AVATAR_FILENAME)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}