package com.example.project_lttbd

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project_lttbd.data.device.DeviceManager
import com.example.project_lttbd.data.remote.BackendSessionCheck
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.notifications.NotificationHelper
import com.example.project_lttbd.ui.components.MMRABottomBar
import com.example.project_lttbd.ui.navigation.MMRADestinations
import com.example.project_lttbd.ui.navigation.MMRANavHost
import com.example.project_lttbd.ui.navigation.mmraBottomNavItems
import com.example.project_lttbd.ui.theme.MMRATheme
import com.example.project_lttbd.data.debug.FirebaseAuthDebug

class MainActivity : ComponentActivity() {

    private val pendingDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Khởi tạo kênh thông báo hệ thống.
        NotificationHelper.ensureNotificationChannel(this)

        // Kiểm tra Firebase ID token với backend ở bản debug.
        BackendSessionCheck.run(this)
        FirebaseAuthDebug.printIdTokenForLocalTesting()
        // Thông tin phục vụ kiểm thử Firebase Cloud Messaging.
        android.util.Log.d(
            "MMRA_FCM_TEST",
            "PROJECT=" +
                    com.google.firebase.FirebaseApp.getInstance()
                        .options.projectId
        )

        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .token
            .addOnSuccessListener { token ->
                android.util.Log.d(
                    "MMRA_FCM_TEST",
                    "TOKEN=$token"
                )
            }
            .addOnFailureListener { error ->
                android.util.Log.e(
                    "MMRA_FCM_TEST",
                    "Không lấy được token",
                    error
                )
            }

        // Trích xuất đích đến nếu mở app từ thông báo.
        extractNotificationRoute(intent)

        // Đồng bộ thiết bị nếu đã có phiên đăng nhập.
        syncDeviceIfLoggedIn()

        setContent {
            MMRATheme {
                val navController = rememberNavController()

                val backStackEntry by
                navController.currentBackStackEntryAsState()

                val currentRoute =
                    backStackEntry?.destination?.route

                // Xin quyền thông báo trên Android 13 trở lên.
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU
                ) {
                    val permissionLauncher =
                        rememberLauncherForActivityResult(
                            contract =
                                ActivityResultContracts.RequestPermission()
                        ) { _ ->
                            syncDeviceIfLoggedIn()
                        }

                    LaunchedEffect(Unit) {
                        val hasPermission =
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            permissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                }

                // Chỉ mở nội dung thông báo khi đã đăng nhập.
                val targetRoute by pendingDestination

                LaunchedEffect(targetRoute, currentRoute) {
                    val route = targetRoute
                        ?.takeIf { it.isNotBlank() }
                        ?: return@LaunchedEffect

                    // Chờ NavHost khởi tạo xong.
                    if (currentRoute == null) {
                        return@LaunchedEffect
                    }

                    // Mỗi yêu cầu chỉ được xử lý một lần.
                    pendingDestination.value = null

                    if (FirebaseProvider.auth.currentUser == null) {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Vui lòng đăng nhập để xem thông báo.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        navController.navigate(
                            MMRADestinations.LOGIN
                        ) {
                            popUpTo(0) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                }

                val showBottomBar = mmraBottomNavItems.any {
                    it.route == currentRoute
                } && currentRoute != MMRADestinations.SCAN

                Scaffold(
                    modifier = Modifier.fillMaxSize(),

                    bottomBar = {
                        if (showBottomBar) {
                            MMRABottomBar(
                                currentRoute = currentRoute,

                                onItemClick = { route ->
                                    navController.navigate(route) {
                                        popUpTo(
                                            navController.graph
                                                .findStartDestination()
                                                .id
                                        ) {
                                            saveState = true
                                        }

                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },

                                modifier = Modifier
                                    .navigationBarsPadding()
                            )
                        }
                    }
                ) { innerPadding ->
                    MMRANavHost(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationRoute(intent)
    }

    private fun extractNotificationRoute(intent: Intent?) {
        if (
            intent == null ||
            intent.getBooleanExtra(
                "mmra_notification_route_consumed",
                false
            )
        ) {
            pendingDestination.value = null
            return
        }

        val requestedRoute = sequenceOf(
            intent.getStringExtra(
                NotificationHelper.EXTRA_DESTINATION
            ),
            intent.getStringExtra("destination")
        )
            .filterNotNull()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }

        val fromFcm =
            intent.hasExtra("gcm.n.analytics_data") ||
                    intent.hasExtra("google.message_id") ||
                    intent.hasExtra("message_id")

        if (requestedRoute == null && !fromFcm) {
            pendingDestination.value = null
            return
        }

        val allowedRoutes = setOf(
            MMRADestinations.HOME,
            MMRADestinations.MEDICINE,
            MMRADestinations.SCHEDULE,
            MMRADestinations.HISTORY,
            MMRADestinations.FAMILY,
            MMRADestinations.ACCOUNT,
            MMRADestinations.MY_ACCOUNT
        )

        pendingDestination.value = requestedRoute
            ?.takeIf { it in allowedRoutes }
            ?: MMRADestinations.HOME

        // Đánh dấu đã nhận đích đến,
        // giữ dữ liệu Firebase cho Analytics.
        intent.putExtra(
            "mmra_notification_route_consumed",
            true
        )
    }

    private fun syncDeviceIfLoggedIn() {
        val currentUid =
            FirebaseProvider.auth.currentUser?.uid

        if (!currentUid.isNullOrBlank()) {
            DeviceManager.syncCurrentDevice(
                this,
                currentUid
            )
        }
    }
}