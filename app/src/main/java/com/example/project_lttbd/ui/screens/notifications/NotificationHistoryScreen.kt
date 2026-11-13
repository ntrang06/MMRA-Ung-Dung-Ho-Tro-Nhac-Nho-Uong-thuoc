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

@Composable
fun NotificationHistoryScreen(
    modifier: Modifier = Modifier,
    notifications: List<LocalNotificationItem>,
    onNotificationClick: (LocalNotificationItem) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MmraBackground)
    ) {
        // Top App Bar
        NotificationTopBar(
            hasItems = notifications.isNotEmpty(),
            hasUnread = notifications.any { !it.isRead },
            onBackClick = onBackClick,
            onMarkAllAsRead = onMarkAllAsRead,
            onClearAll = onClearAll
        )

        if (notifications.isEmpty()) {
            NotificationEmptyState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    NotificationItemRow(
                        item = item,
                        onClick = { onNotificationClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationTopBar(
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
                painter = painterResource(id = R.drawable.ic_arrow_back),
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
            TextButton(onClick = onMarkAllAsRead) {
                Text(
                    text = "Đã đọc tất cả",
                    fontSize = 13.sp,
                    color = MmraPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (hasItems) {
            TextButton(onClick = onClearAll) {
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
    onClick: () -> Unit
) {
    val cardBg = if (item.isRead) MmraSurface else MmraPrimaryContainer.copy(alpha = 0.45f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isRead) 1.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (item.isRead) Color(0xFFE0E0E0) else MmraPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chuong),
                    contentDescription = null,
                    tint = if (item.isRead) MmraTextSecondary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                            color = MmraTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatNotificationTime(item.timestampMillis),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MmraTextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

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

            // Dot indicating unread
            if (!item.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
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
                painter = painterResource(id = R.drawable.chuong),
                contentDescription = null,
                tint = MmraPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bạn chưa có thông báo nào",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MmraTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

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
    val date = Date(timestampMillis)
    val sdf = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
    return sdf.format(date)
}
