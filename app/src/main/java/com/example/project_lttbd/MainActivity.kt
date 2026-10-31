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
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.notifications.NotificationHelper
import com.example.project_lttbd.ui.components.MMRABottomBar
import com.example.project_lttbd.ui.navigation.MMRADestinations
import com.example.project_lttbd.ui.navigation.MMRANavHost
import com.example.project_lttbd.ui.navigation.mmraBottomNavItems
import com.example.project_lttbd.ui.theme.MMRATheme

class MainActivity : ComponentActivity() {

    private var pendingDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Khởi tạo kênh thông báo hệ thống
        NotificationHelper.ensureNotificationChannel(this)

        // Trích xuất intent điều hướng nếu mở từ thông báo
        extractNotificationRoute(intent)

        // Đồng bộ thiết bị nếu đã có phiên đăng nhập
        syncDeviceIfLoggedIn()

        setContent {
            MMRATheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                // Xử lý xin quyền thông báo trên Android 13+ (API 33+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ ->
                        syncDeviceIfLoggedIn()
                    }

                    LaunchedEffect(Unit) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Điều hướng khi có đích đến từ thông báo
                val targetRoute by pendingDestination
                LaunchedEffect(targetRoute) {
                    targetRoute?.let { route ->
                        if (route.isNotBlank()) {
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                        pendingDestination.value = null
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
                                                .findStartDestination().id
                                        ) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.navigationBarsPadding()
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
        val destination = intent?.getStringExtra(NotificationHelper.EXTRA_DESTINATION)
        if (!destination.isNullOrBlank()) {
            pendingDestination.value = destination
        }
    }

    private fun syncDeviceIfLoggedIn() {
        val currentUid = FirebaseProvider.auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            DeviceManager.syncCurrentDevice(this, currentUid)
        }
    }
}
