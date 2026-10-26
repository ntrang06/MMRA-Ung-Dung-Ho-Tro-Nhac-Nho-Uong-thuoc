package com.example.project_lttbd.ui.screens.family

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R

@Composable
fun FamilyPermissionsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    memberName: String = "Nguyễn Minh Anh",
    memberRole: String = "Con gái • Người chăm sóc"
) {
    val context = LocalContext.current

    var canViewMedicineList by remember { mutableStateOf(true) }
    var canViewSchedule by remember { mutableStateOf(true) }
    var canViewHistory by remember { mutableStateOf(true) }
    var canReceiveAlerts by remember { mutableStateOf(true) }
    var canSendReminder by remember { mutableStateOf(false) }

    val activeCount = listOf(
        canViewMedicineList,
        canViewSchedule,
        canViewHistory,
        canReceiveAlerts,
        canSendReminder
    ).count { it }

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
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Quản lý quyền chia sẻ",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        }

        // Nội dung cuộn
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card chủ dữ liệu
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF8F6)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bạn: Chủ dữ liệu",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F6053)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bạn quyết định cấp hoặc thu hồi từng quyền truy cập dữ liệu thuốc của mình.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF1E8272),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            // Card thông tin người nhận quyền
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2ECEA))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "NGƯỜI NHẬN QUYỀN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDF2EE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MA",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = memberName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = memberRole,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F7F2)
                        ) {
                            Text(
                                text = "Đã liên kết",
                                color = Color(0xFF0F6053),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "$activeCount quyền đang được cấp",
                                color = Color(0xFF1D4ED8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Danh sách các quyền chia sẻ
            Text(
                text = "Quyền bạn chia sẻ với $memberName",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )

            PermissionSwitchCard(
                iconResId = R.drawable.ic_pill,
                title = "Xem danh sách thuốc",
                description = "Tên thuốc, liều lượng và hướng dẫn đã lưu.",
                checked = canViewMedicineList,
                onCheckedChange = { canViewMedicineList = it }
            )

            PermissionSwitchCard(
                iconResId = R.drawable.ic_calendar,
                title = "Xem lịch uống thuốc",
                description = "Giờ uống và trạng thái xác nhận từng liều.",
                checked = canViewSchedule,
                onCheckedChange = { canViewSchedule = it }
            )

            PermissionSwitchCard(
                iconResId = R.drawable.ic_clock,
                title = "Xem lịch sử dùng thuốc",
                description = "Các xác nhận và hoạt động dùng thuốc theo ngày.",
                checked = canViewHistory,
                onCheckedChange = { canViewHistory = it }
            )

            PermissionSwitchCard(
                iconResId = R.drawable.ic_bell,
                title = "Nhận cảnh báo",
                description = "Thông báo liều quá giờ chưa xác nhận.",
                checked = canReceiveAlerts,
                onCheckedChange = { canReceiveAlerts = it }
            )

            PermissionSwitchCard(
                iconResId = R.drawable.ic_message_circle,
                title = "Gửi nhắc nhở",
                description = "$memberName có thể gửi nhắc nhở uống thuốc cho bạn.",
                checked = canSendReminder,
                onCheckedChange = { canSendReminder = it }
            )

            // Lưu ý không bao gồm quyền quản lý
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF3F4F6)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Không bao gồm quyền quản lý thuốc",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$memberName không được sửa hướng dẫn uống hoặc xác nhận liều thay bạn.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            // Nút thu hồi toàn bộ quyền
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        canViewMedicineList = false
                        canViewSchedule = false
                        canViewHistory = false
                        canReceiveAlerts = false
                        canSendReminder = false
                        Toast.makeText(context, "Đã thu hồi tất cả quyền!", Toast.LENGTH_SHORT).show()
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEE2E2)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Thu hồi toàn bộ quyền",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Save Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Đã lưu cài đặt phân quyền!", Toast.LENGTH_SHORT).show()
                        onBackClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Lưu cài đặt",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionSwitchCard(
    iconResId: Int,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2ECEA))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
