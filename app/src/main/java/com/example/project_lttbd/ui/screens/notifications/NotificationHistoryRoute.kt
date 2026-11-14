package com.example.project_lttbd.ui.screens.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.notifications.model.LocalNotificationItem
import com.example.project_lttbd.notifications.storage.LocalNotificationStore
import com.example.project_lttbd.ui.screens.schedule.rememberScheduleOwnerUid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NotificationHistoryRoute(
    onNotificationClick: (LocalNotificationItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ownerUid = rememberScheduleOwnerUid()

    if (ownerUid.isNullOrBlank()) {
        Column(modifier.padding(20.dp)) {
            Text("Vui lòng đăng nhập để xem thông báo.")

            TextButton(onClick = onBackClick) {
                Text("Quay lại")
            }
        }
        return
    }

    key(ownerUid) {
        val context = LocalContext.current.applicationContext
        val lifecycleOwner = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()

        var notifications by remember {
            mutableStateOf<List<LocalNotificationItem>>(emptyList())
        }

        var initialLoading by remember {
            mutableStateOf(true)
        }

        var busy by remember {
            mutableStateOf(false)
        }

        var errorMessage by remember {
            mutableStateOf<String?>(null)
        }

        fun ensureOwner() {
            if (FirebaseProvider.auth.currentUser?.uid != ownerUid) {
                throw CancellationException("Tài khoản đã thay đổi.")
            }
        }

        fun reload(
            mutation: () -> Unit = {},
            onFinished: () -> Unit = {}
        ) {
            if (
                busy ||
                FirebaseProvider.auth.currentUser?.uid != ownerUid
            ) {
                return
            }

            busy = true
            errorMessage = null

            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        ensureOwner()
                        mutation()

                        LocalNotificationStore.getNotifications(
                            context,
                            ownerUid
                        )
                    }

                    ensureOwner()
                    notifications = result
                    onFinished()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    errorMessage =
                        "Chưa cập nhật được thông báo. Kéo xuống để thử lại."
                } finally {
                    initialLoading = false
                    busy = false
                }
            }
        }

        LaunchedEffect(Unit) {
            reload()
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    reload()
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        NotificationHistoryScreen(
            modifier = modifier,
            notifications = notifications,
            isLoading = initialLoading,
            isRefreshing = busy && !initialLoading,
            actionsEnabled = !busy && !initialLoading,
            errorMessage = errorMessage,
            onRefresh = {
                reload()
            },
            onNotificationClick = { item ->
                reload(
                    mutation = {
                        LocalNotificationStore.markAsRead(
                            context,
                            ownerUid,
                            item.id
                        )
                    },
                    onFinished = {
                        onNotificationClick(item.copy(isRead = true))
                    }
                )
            },
            onMarkAllAsRead = {
                reload(
                    mutation = {
                        LocalNotificationStore.markAllAsRead(
                            context,
                            ownerUid
                        )
                    }
                )
            },
            onClearAll = {
                reload(
                    mutation = {
                        LocalNotificationStore.clearAll(
                            context,
                            ownerUid
                        )
                    }
                )
            },
            onBackClick = onBackClick
        )
    }
}