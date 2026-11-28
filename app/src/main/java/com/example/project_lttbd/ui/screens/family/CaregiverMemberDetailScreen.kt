package com.example.project_lttbd.ui.screens.family

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_lttbd.R
import com.example.project_lttbd.data.remote.dto.DoseUnitDto
import com.example.project_lttbd.data.remote.dto.ScheduleResponseDto
import com.example.project_lttbd.data.remote.dto.family.SharedDoseDto
import com.example.project_lttbd.ui.screens.schedule.ScheduleDoseStatus
import com.example.project_lttbd.ui.viewmodel.FamilyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CaregiverMemberDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    linkId: String = "",
    onViewHistoryClick: () -> Unit = {},
    onPermissionsClick: () -> Unit = {},
    familyViewModel: FamilyViewModel = viewModel()
) {

    val isLoadingMembers = familyViewModel.isLoadingMembers
    val membersError = familyViewModel.membersError
    val member = familyViewModel.members.firstOrNull { it.linkId == linkId }

    val canViewSchedule = member?.permissions?.viewSchedule == true
    val canViewDoseStatus = member?.permissions?.viewDoseStatus == true

    var selectedDate by remember { mutableStateOf(getTodayDateString()) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val sharedSchedules = familyViewModel.sharedSchedules
    val sharedDoses = familyViewModel.sharedDoses
    val isLoadingShared = familyViewModel.isLoadingSharedData
    val sharedError = familyViewModel.sharedDataError

    // Chỉ gọi API chia sẻ khi danh sách thành viên đã tải xong và thành viên tồn tại với quyền được cấp
    LaunchedEffect(linkId, selectedDate, member) {
        if (linkId.isNotBlank() && member != null) {
            if (canViewSchedule) {
                familyViewModel.loadSharedSchedules(linkId)
            }
            if (canViewDoseStatus) {
                familyViewModel.loadSharedDoses(linkId, selectedDate)
            }
        }
    }

    val displayUid = if (member != null) {
        val uid = member.memberUid
        if (uid.length > 8) "UID: ${uid.take(8)}..." else "UID: $uid"
    } else {
        if (linkId.length > 8) "Link: ${linkId.take(8)}..." else "Link: $linkId"
    }

    val displayRole = if (member != null) {
        if (member.role == "owner") "Chủ sở hữu lịch" else "Người chăm sóc"
    } else {
        "Thành viên liên kết"
    }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = {
                Text(
                    text = "Gửi lời nhắc kiểm tra",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Ứng dụng hiện chưa hỗ trợ gửi lời nhắc trực tiếp đến thiết bị của người thân. Bạn có thể liên hệ với người thân qua điện thoại hoặc ứng dụng nhắn tin.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF4B5563),
                        lineHeight = 20.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { showReminderDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Đã hiểu")
                }
            }
        )
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
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Theo dõi người thân",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = displayUid,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F2F0))
                    .clickable { onPermissionsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Cài đặt phân quyền",
                    tint = Color(0xFF0F6053),
                    modifier = Modifier.size(20.dp)
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
            // Trạng thái Tải danh sách thành viên / Lỗi / Hồ sơ thành viên
            if (isLoadingMembers && member == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2ECEA))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Đang kiểm tra thông tin liên kết...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563))
                            )
                        }
                    }
                }
            } else if (membersError != null && member == null) {
                ErrorCard(
                    message = membersError,
                    onRetry = { familyViewModel.loadMembers() }
                )
            } else if (member == null) {
                EmptyStateCard(
                    message = "Liên kết người thân không tồn tại hoặc đã bị hủy."
                )
            } else {
                // Thẻ hồ sơ người thân chính thức
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2ECEA))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDF2EE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FM",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayUid,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$displayRole • Hồ sơ kết nối",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F7F2)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_wifi),
                                    contentDescription = null,
                                    tint = Color(0xFF0F6053),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ĐÃ LIÊN KẾT",
                                    color = Color(0xFF0F6053),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Nút nhắn nhắc nhở
            OutlinedButton(
                onClick = { showReminderDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_message_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gửi lời nhắc kiểm tra", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }

            // Section 1: TRẠNG THÁI LIỀU UỐNG CỦA NGƯỜI THÂN
            Text(
                text = "Trạng thái liều uống",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            )

            // Selector ngày hiển thị
            DateSelectorRow(
                selectedDate = selectedDate,
                onPrevDay = { selectedDate = changeDateByDays(selectedDate, -1) },
                onNextDay = { selectedDate = changeDateByDays(selectedDate, 1) },
                onTodayClick = { selectedDate = getTodayDateString() }
            )

            if (isLoadingMembers && member == null) {
                // Đang tải thông tin thành viên, hiển thị khung chờ thay vì báo thiếu quyền
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (member == null) {
                EmptyStateCard(
                    message = "Không thể kiểm tra quyền do không tìm thấy thông tin liên kết."
                )
            } else if (!canViewDoseStatus) {
                PermissionWarningCard(
                    message = "Chủ sở hữu chưa cấp quyền xem trạng thái liều uống.",
                    onSettingsClick = onPermissionsClick
                )
            } else {
                if (isLoadingShared && sharedDoses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (sharedError != null && sharedDoses.isEmpty()) {
                    ErrorCard(
                        message = sharedError,
                        onRetry = { familyViewModel.loadSharedDoses(linkId, selectedDate) }
                    )
                } else if (sharedDoses.isEmpty()) {
                    EmptyStateCard(
                        message = "Không có liều uống thuốc trong ngày ${formatDisplayDate(selectedDate)}."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sharedDoses.forEach { dose ->
                            SharedDoseItemCard(dose = dose)
                        }
                    }
                }
            }

            // Section 2: LỊCH UỐNG THUỐC ĐƯỢC CHIA SẺ
            Text(
                text = "Lịch uống thuốc được chia sẻ",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            )

            if (isLoadingMembers && member == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (member == null) {
                EmptyStateCard(
                    message = "Không thể kiểm tra quyền do không tìm thấy thông tin liên kết."
                )
            } else if (!canViewSchedule) {
                PermissionWarningCard(
                    message = "Chủ sở hữu chưa cấp quyền xem lịch uống thuốc.",
                    onSettingsClick = onPermissionsClick
                )
            } else {
                if (isLoadingShared && sharedSchedules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (sharedError != null && sharedSchedules.isEmpty()) {
                    ErrorCard(
                        message = sharedError,
                        onRetry = { familyViewModel.loadSharedSchedules(linkId) }
                    )
                } else if (sharedSchedules.isEmpty()) {
                    EmptyStateCard(
                        message = "Chủ sở hữu chưa có lịch uống thuốc nào được lập."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sharedSchedules.forEach { schedule ->
                            SharedScheduleItemCard(schedule = schedule)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DateSelectorRow(
    selectedDate: String,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2ECEA))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable { onPrevDay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = "Ngày trước",
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTodayClick() }
            ) {
                Text(
                    text = formatDisplayDate(selectedDate),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                if (selectedDate == getTodayDateString()) {
                    Text(
                        text = "Hôm nay",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
                    .clickable { onNextDay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = "Ngày sau",
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SharedDoseItemCard(dose: SharedDoseDto) {
    val doseStatus = mapDoseStatus(dose.status)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    .background(doseStatus.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doseStatus.symbol,
                    color = doseStatus.foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Giờ uống: ${formatIsoTime(dose.scheduledAt)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                if (!dose.actionAt.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Thực hiện lúc: ${formatIsoTime(dose.actionAt)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF4B5563),
                            fontSize = 11.sp
                        )
                    )
                }

                if (!dose.nextReminderAt.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Nhắc tiếp theo: ${formatIsoTime(dose.nextReminderAt)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = doseStatus.background
            ) {
                Text(
                    text = doseStatus.label,
                    color = doseStatus.foreground,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SharedScheduleItemCard(schedule: ScheduleResponseDto) {
    val timesText = schedule.times.joinToString(", ") { formatMinuteOfDay(it) }
    val weekdaysText = formatWeekdays(schedule.weekdays)
    val quantityText = "${schedule.doseQuantity} ${formatUnit(schedule.doseUnit)}"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2ECEA))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Giờ uống: $timesText",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (schedule.enabled) Color(0xFFE8F7F2) else Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = if (schedule.enabled) "Đang bật" else "Đã tắt",
                        color = if (schedule.enabled) Color(0xFF0F6053) else Color(0xFF6B7280),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Liều lượng: $quantityText",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
            )

            Text(
                text = "Lịch lặp: $weekdaysText",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF4B5563)
                )
            )

            if (schedule.mealInstruction.isNotBlank()) {
                Text(
                    text = "Hướng dẫn: ${schedule.mealInstruction}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp
                    )
                )
            }

            Text(
                text = "Ngày bắt đầu: ${schedule.startDate}${if (!schedule.endDate.isNullOrBlank()) " • Kết thúc: ${schedule.endDate}" else ""}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun PermissionWarningCard(
    message: String,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFEF3C7),
        border = BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shield_check),
                    contentDescription = null,
                    tint = Color(0xFF92400E),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Chưa được cấp quyền",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF78350F),
                    lineHeight = 18.sp
                )
            )
            TextButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Quản lý phân quyền",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2ECEA))
    ) {
        Box(
            modifier = Modifier.padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFEE2E2),
        border = BorderStroke(1.dp, Color(0xFFFECDD3))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = Color(0xFFDC2626),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("Tải lại")
            }
        }
    }
}

private fun getTodayDateString(): String {
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(cal.time)
}

private fun formatDisplayDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else dateStr
    } catch (_: Exception) {
        dateStr
    }
}

private fun changeDateByDays(dateStr: String, days: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_MONTH, days)
        sdf.format(cal.time)
    } catch (_: Exception) {
        dateStr
    }
}

private fun formatMinuteOfDay(minute: Int): String {
    val h = minute / 60
    val m = minute % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
}

private fun formatIsoTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        if (isoString.contains("T")) {
            val timePart = isoString.split("T")[1]
            timePart.take(5)
        } else {
            isoString.take(5)
        }
    } catch (_: Exception) {
        isoString
    }
}

private fun formatUnit(doseUnit: DoseUnitDto): String {
    return when (doseUnit) {
        DoseUnitDto.TABLET -> "viên"
        DoseUnitDto.SACHET -> "gói"
        DoseUnitDto.ML -> "ml"
        DoseUnitDto.DROP -> "giọt"
        DoseUnitDto.DOSE -> "liều"
    }
}

private fun formatWeekdays(weekdays: List<Int>): String {
    if (weekdays.size == 7) return "Hằng ngày"
    val names = mapOf(
        1 to "T2",
        2 to "T3",
        3 to "T4",
        4 to "T5",
        5 to "T6",
        6 to "T7",
        7 to "CN"
    )
    return weekdays.mapNotNull { names[it] }.joinToString(", ")
}

private fun mapDoseStatus(statusString: String): ScheduleDoseStatus {
    return when (statusString.uppercase()) {
        "TAKEN" -> ScheduleDoseStatus.TAKEN
        "PENDING" -> ScheduleDoseStatus.PENDING
        "OVERDUE" -> ScheduleDoseStatus.OVERDUE
        "UNCONFIRMED" -> ScheduleDoseStatus.UNCONFIRMED
        "SNOOZED" -> ScheduleDoseStatus.SNOOZED
        "SKIPPED" -> ScheduleDoseStatus.SKIPPED
        "MISSED" -> ScheduleDoseStatus.MISSED
        "CANCELLED" -> ScheduleDoseStatus.CANCELLED
        else -> ScheduleDoseStatus.PENDING
    }
}
