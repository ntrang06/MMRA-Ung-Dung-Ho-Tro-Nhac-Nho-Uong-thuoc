package com.example.project_lttbd.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.notifications.model.LocalNotificationItem
import com.example.project_lttbd.ui.theme.MmraBackground
import com.example.project_lttbd.ui.theme.MmraPrimary
import com.example.project_lttbd.ui.theme.MmraPrimaryContainer
import com.example.project_lttbd.ui.theme.MmraSurface
import com.example.project_lttbd.ui.theme.MmraTextPrimary
import com.example.project_lttbd.ui.theme.MmraTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    modifier: Modifier = Modifier,
    notifications: List<LocalNotificationItem>,
    onNotificationClick: (LocalNotificationItem) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    actionsEnabled: Boolean = true,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MmraBackground)
    ) {
        NotificationTopBar(
            actionsEnabled = actionsEnabled,
            hasItems = notifications.isNotEmpty(),
            hasUnread = notifications.any { !it.isRead },
            onBackClick = onBackClick,
            onMarkAllAsRead = onMarkAllAsRead,
            onClearAll = onClearAll
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isLoading && actionsEnabled) {
                    onRefresh()
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    item(key = "error") {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                when {
                    isLoading -> {
                        items(3) {
                            NotificationSkeletonRow()
                        }
                    }

                    notifications.isEmpty() && errorMessage == null -> {
                        item(key = "empty") {
                            Box(Modifier.fillParentMaxSize()) {
                                NotificationEmptyState()
                            }
                        }
                    }

                    else -> {
                        items(
                            items = notifications,
                            key = { "notification_${it.id}" }
                        ) { item ->
                            NotificationItemRow(
                                item = item,
                                enabled = actionsEnabled,
                                onClick = {
                                    onNotificationClick(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTopBar(
    actionsEnabled: Boolean,
    hasItems: Boolean,
    hasUnread: Boolean,
    onBackClick: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MmraSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Quay lại",
                tint = MmraTextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = "Thông báo",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MmraTextPrimary
            ),
            modifier = Modifier.weight(1f)
        )

        if (hasUnread) {
            TextButton(
                onClick = onMarkAllAsRead,
                enabled = actionsEnabled
            ) {
                Text(
                    text = "Đã đọc tất cả",
                    fontSize = 13.sp,
                    color = MmraPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (hasItems) {
            TextButton(
                onClick = onClearAll,
                enabled = actionsEnabled
            ) {
                Text(
                    text = "Xóa hết",
                    fontSize = 13.sp,
                    color = MmraTextSecondary
                )
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    item: LocalNotificationItem,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val cardBackground = if (item.isRead) {
        MmraSurface
    } else {
        MmraPrimaryContainer.copy(alpha = 0.45f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (item.isRead) 1.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isRead) {
                            Color(0xFFE0E0E0)
                        } else {
                            MmraPrimary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.chuong),
                    contentDescription = null,
                    tint = if (item.isRead) {
                        MmraTextSecondary
                    } else {
                        Color.White
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = if (item.isRead) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Bold
                            },
                            color = MmraTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatNotificationTime(
                            item.timestampMillis
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MmraTextSecondary
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = MmraTextSecondary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!item.isRead) {
                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MmraPrimary)
                )
            }
        }
    }
}

@Composable
private fun NotificationSkeletonRow() {
    val placeholder = Color(0xFFE3E8E6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MmraSurface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(placeholder)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholder)
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholder)
            )

            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholder)
            )
        }
    }
}

@Composable
private fun NotificationEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MmraPrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.chuong),
                contentDescription = null,
                tint = MmraPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Bạn chưa có thông báo nào",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MmraTextPrimary
            )
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Các thông báo nhắc giờ uống thuốc và cảnh báo sức khỏe sẽ xuất hiện tại đây.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                color = MmraTextSecondary
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatNotificationTime(timestampMillis: Long): String {
    return SimpleDateFormat(
        "HH:mm - dd/MM",
        Locale.getDefault()
    ).format(Date(timestampMillis))
}