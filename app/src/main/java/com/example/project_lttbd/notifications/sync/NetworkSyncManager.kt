package com.example.project_lttbd.notifications.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.project_lttbd.data.remote.AuthRepository
import java.util.concurrent.atomic.AtomicBoolean

object NetworkSyncManager {
    private const val TAG = "NetworkSyncManager"
    private val isRegistered = AtomicBoolean(false)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastSyncMillis = 0L
    private const val MIN_SYNC_INTERVAL_MILLIS = 10_000L // Throttling 10s

    fun registerNetworkCallback(context: Context) {
        if (isRegistered.getAndSet(true)) return

        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val now = System.currentTimeMillis()
                if (now - lastSyncMillis < MIN_SYNC_INTERVAL_MILLIS) {
                    Log.d(TAG, "Mạng phục hồi nhưng vừa đồng bộ gần đây, bỏ qua...")
                    return
                }
                lastSyncMillis = now

                Log.d(TAG, "Kết nối mạng được phục hồi, tiến hành đồng bộ lại lịch báo thức...")
                val currentUid = AuthRepository.currentUid
                if (!currentUid.isNullOrBlank()) {
                    MedicationReminderSync.triggerBackgroundResync(appContext, currentUid)
                }
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, networkCallback!!)
            Log.d(TAG, "Đã đăng ký lắng nghe phục hồi mạng thành công.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đăng ký NetworkCallback", e)
            isRegistered.set(false)
        }
    }

    fun unregisterNetworkCallback(context: Context) {
        if (!isRegistered.getAndSet(false)) return
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback?.let {
            try {
                cm?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi hủy đăng ký NetworkCallback", e)
            }
        }
        networkCallback = null
    }
}
