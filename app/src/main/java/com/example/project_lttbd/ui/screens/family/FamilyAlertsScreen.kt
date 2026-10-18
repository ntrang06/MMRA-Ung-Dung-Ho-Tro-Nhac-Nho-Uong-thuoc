package com.example.project_lttbd.ui.screens.family

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.data.mock.AlertSeverity
import com.example.project_lttbd.data.mock.CaregiverAlert
import com.example.project_lttbd.data.mock.MockAlertData

@Composable
fun FamilyAlertsScreen(
    onBackClick: () -> Unit,
    onAlertClick: (CaregiverAlert) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var alertsList by remember { mutableStateOf(MockAlertData.mockAlerts) }

    val filteredAlerts = when (selectedFilter) {
        "Chưa xử lý" -> alertsList.filter { !it.isRead }
        "Đã đọc" -> alertsList.filter { it.isRead }
        else -> alertsList
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF9))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F2F0))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Cảnh báo người thân",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "Cùng theo dõi, thêm an tâm",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Nội dung cuộn
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card tóm tắt cảnh báo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF8F6)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1 cảnh báo cần kiểm tra",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F6053)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Liều của bố quá giờ nhưng chưa có xác nhận. Hãy liên hệ để biết tình hình.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF1E8272),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            // Thanh lọc Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTab(
                    title = "Tất cả • ${alertsList.size}",
                    isSelected = selectedFilter == "Tất cả",
                    onClick = { selectedFilter = "Tất cả" },
                    modifier = Modifier.weight(1f)
                )
                FilterTab(
                    title = "Chưa xử lý • 1",
                    isSelected = selectedFilter == "Chưa xử lý",
                    onClick = { selectedFilter = "Chưa xử lý" },
                    modifier = Modifier.weight(1.1f)
                )
                FilterTab(
                    title = "Đã đọc • 1",
                    isSelected = selectedFilter == "Đã đọc",
                    onClick = { selectedFilter = "Đã đọc" },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Thông báo gần đây",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )

            // Danh sách các thông báo
            filteredAlerts.forEach { alert ->
                AlertCard(
                    alert = alert,
                    onClick = { onAlertClick(alert) },
                    onCallClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${alert.memberPhone.replace(" ", "")}"))
                        context.startActivity(intent)
                    },
                    onRemindClick = {
                        Toast.makeText(context, "Đã gửi thông báo nhắc nhở khẩn cấp!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Gợi ý về trạng thái đọc
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF3F4F6)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Đọc thông báo ≠ xác nhận liều",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đánh dấu đã đọc chỉ thay đổi trạng thái thông báo, không xác nhận uống thuốc hay xử lý trạng thái liều.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FilterTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                color = if (isSelected) Color.White else Color(0xFF4B5563),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun AlertCard(
    alert: CaregiverAlert,
    onClick: () -> Unit = {},
    onCallClick: () -> Unit,
    onRemindClick: () -> Unit
) {
    val isLate = alert.minutesLate > 0
    val borderColor = if (isLate && !alert.isRead) Color(0xFFFECDD3) else Color(0xFFE5E7EB)
    val bgColor = if (isLate && !alert.isRead) Color(0xFFFFF7F7) else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (!alert.isRead) Color(0xFFFEE2E2) else Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = if (!alert.isRead) "Chưa xử lý" else "Đã đọc",
                        color = if (!alert.isRead) Color(0xFFDC2626) else Color(0xFF6B7280),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = alert.timestamp,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF), fontSize = 11.sp)
                )
            }

            Text(
                text = "Liều ${alert.scheduledTime} chưa được xác nhận",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )

            Text(
                text = "${alert.memberName} (${alert.relationship}) • ${alert.medicineName}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4B5563))
            )

            Text(
                text = "Quá giờ ${alert.minutesLate} phút • Chưa có xác nhận từ người thân.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium
                )
            )

            // Nút hành động nhanh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("📞 Gọi điện", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRemindClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("💬 Gửi nhắc", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        }
    }
}
