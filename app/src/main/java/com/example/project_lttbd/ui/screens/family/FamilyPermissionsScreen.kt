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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_lttbd.R
import com.example.project_lttbd.data.remote.dto.family.FamilyPermissionsDto
import com.example.project_lttbd.ui.viewmodel.FamilyViewModel

@Composable
fun FamilyPermissionsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    linkId: String = "",
    memberName: String = "Người thân",
    memberRole: String = "Người chăm sóc",
    familyViewModel: FamilyViewModel = viewModel()
) {
    val context = LocalContext.current

    val isLoadingMembers = familyViewModel.isLoadingMembers
    val membersError = familyViewModel.membersError
    val currentMember = familyViewModel.members.firstOrNull { it.linkId == linkId }
    val initialPermissions = currentMember?.permissions ?: FamilyPermissionsDto()

    // Xác định vai trò chuẩn xác theo nguyên tắc Fail-Safe (mặc định false khi chưa xác thực xong):
    // role == "owner": Người dùng hiện tại là Chủ sở hữu lịch (được sửa quyền).
    // role == "caregiver": Người dùng hiện tại là Người chăm sóc (chỉ được xem).
    val isOwnerConfirmed = currentMember != null && currentMember.role == "owner"
    val isCaregiverConfirmed = currentMember != null && currentMember.role == "caregiver"

    var viewSchedule by remember(currentMember) { mutableStateOf(initialPermissions.viewSchedule) }
    var viewDoseStatus by remember(currentMember) { mutableStateOf(initialPermissions.viewDoseStatus) }
    var receiveMissedDoseAlerts by remember(currentMember) { mutableStateOf(initialPermissions.receiveMissedDoseAlerts) }

    var showRevokeConfirmDialog by remember { mutableStateOf(false) }

    val activeCount = listOf(viewSchedule, viewDoseStatus, receiveMissedDoseAlerts).count { it }
    val isUpdating = familyViewModel.isUpdatingPermissions
    val isRevoking = familyViewModel.isRevokingLink
    val updateError = familyViewModel.updatePermissionsError
    val revokeError = familyViewModel.revokeLinkError

    val displayName = if (currentMember != null) {
        val shortUid = if (currentMember.memberUid.length > 8) {
            currentMember.memberUid.take(8) + "..."
        } else {
            currentMember.memberUid
        }
        "UID: $shortUid"
    } else {
        memberName
    }

    val displayRole = if (currentMember != null) {
        if (currentMember.role == "owner") "Chủ sở hữu lịch" else if (currentMember.role == "caregiver") "Người chăm sóc" else "Không xác định"
    } else {
        memberRole
    }

    // AlertDialog xác nhận thu hồi liên kết
    if (showRevokeConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRevoking) showRevokeConfirmDialog = false
            },
            title = {
                Text(
                    text = "Hủy liên kết gia đình?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn hủy liên kết với thành viên này không? Sau khi xác nhận, liên kết sẽ bị thu hồi và người này không thể tiếp tục truy cập dữ liệu được chia sẻ.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF4B5563),
                        lineHeight = 20.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (linkId.isNotBlank() && !isRevoking) {
                            familyViewModel.revokeLink(linkId) {
                                showRevokeConfirmDialog = false
                                Toast.makeText(context, "Đã thu hồi liên kết gia đình!", Toast.LENGTH_SHORT).show()
                                onBackClick()
                            }
                        }
                    },
                    enabled = !isRevoking,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    if (isRevoking) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Xác nhận hủy", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRevokeConfirmDialog = false },
                    enabled = !isRevoking
                ) {
                    Text("Quay lại")
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
            // Xử lý rõ ràng các trạng thái Tải dữ liệu / Lỗi / Không tìm thấy liên kết / Banner vai trò
            if (isLoadingMembers && currentMember == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEFF8F6)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Đang xác thực vai trò và phân quyền...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF4B5563))
                        )
                    }
                }
            } else if (membersError != null && currentMember == null) {
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
                            text = membersError,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { familyViewModel.loadMembers() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Tải lại danh sách")
                        }
                    }
                }
            } else if (currentMember == null) {
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
                            text = "Liên kết gia đình không tồn tại hoặc đã bị thu hồi.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                // Banner trạng thái vai trò đã xác thực
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOwnerConfirmed) Color(0xFFEFF8F6) else Color(0xFFFEF3C7)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isOwnerConfirmed) "Phân quyền truy cập" else "Chỉ xem phân quyền",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOwnerConfirmed) Color(0xFF0F6053) else Color(0xFF92400E)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isOwnerConfirmed) {
                                "Bạn quyết định cấp hoặc thu hồi từng quyền truy cập dữ liệu thuốc chính thức của mình."
                            } else if (isCaregiverConfirmed) {
                                "Bạn đang tham gia với vai trò Người chăm sóc. Chỉ chủ sở hữu lịch mới có quyền thay đổi phân quyền này."
                            } else {
                                "Không thể xác định vai trò của liên kết này."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isOwnerConfirmed) Color(0xFF1E8272) else Color(0xFF78350F),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                // Card thông tin người liên kết
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2ECEA))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "NGƯỜI LIÊN KẾT",
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
                                    text = "FM",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = displayRole,
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
                                    text = "$activeCount/3 quyền đang được cấp",
                                    color = Color(0xFF1D4ED8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Danh sách các quyền chia sẻ
            Text(
                text = "Các quyền hỗ trợ từ backend",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )

            if (updateError != null || revokeError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = updateError ?: revokeError ?: "",
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            PermissionSwitchCard(
                iconResId = R.drawable.ic_calendar,
                title = "Xem lịch uống thuốc (viewSchedule)",
                description = "Cho phép người thân xem danh sách giờ uống thuốc đã thiết lập.",
                checked = viewSchedule,
                enabled = isOwnerConfirmed && !isUpdating,
                onCheckedChange = { if (isOwnerConfirmed) viewSchedule = it }
            )

            PermissionSwitchCard(
                iconResId = R.drawable.ic_clock,
                title = "Xem trạng thái liều (viewDoseStatus)",
                description = "Cho phép người thân xem trạng thái liều đã uống, bỏ qua hoặc trễ liều.",
                checked = viewDoseStatus,
                enabled = isOwnerConfirmed && !isUpdating,
                onCheckedChange = { if (isOwnerConfirmed) viewDoseStatus = it }
            )

            PermissionSwitchCard(
                iconResId = R.drawable.ic_bell,
                title = "Nhận cảnh báo trễ liều (receiveMissedDoseAlerts)",
                description = "Gửi thông báo cảnh báo cho người thân khi bạn bị trễ liều uống thuốc.",
                checked = receiveMissedDoseAlerts,
                enabled = isOwnerConfirmed && !isUpdating,
                onCheckedChange = { if (isOwnerConfirmed) receiveMissedDoseAlerts = it }
            )

            // Ghi chú về quyền
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF3F4F6)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isOwnerConfirmed) "Không bao gồm quyền chỉnh sửa" else "Chỉ dành cho Chủ sở hữu",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isOwnerConfirmed) {
                            "Người thân chỉ có quyền xem thông tin được cấp, không có quyền thay đổi lịch uống hoặc xác nhận liều thay bạn."
                        } else {
                            "Bạn đang tham gia với vai trò Người chăm sóc. Chỉ chủ sở hữu lịch mới có quyền thay đổi phân quyền truy cập."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            // Nút hủy/thu hồi liên kết (Mở AlertDialog xác nhận)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isRevoking && linkId.isNotBlank()) {
                        if (linkId.isNotBlank()) {
                            showRevokeConfirmDialog = true
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEE2E2)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRevoking) {
                        CircularProgressIndicator(
                            color = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Hủy liên kết tài khoản này",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Save Button (chỉ hiển thị khi đã xác nhận chính xác là Owner)
        if (isOwnerConfirmed) {
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
                            if (linkId.isNotBlank()) {
                                val newPermissions = FamilyPermissionsDto(
                                    viewSchedule = viewSchedule,
                                    viewDoseStatus = viewDoseStatus,
                                    receiveMissedDoseAlerts = receiveMissedDoseAlerts
                                )
                                familyViewModel.updatePermissions(linkId, newPermissions) {
                                    Toast.makeText(context, "Đã lưu cài đặt phân quyền!", Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                }
                            } else {
                                onBackClick()
                            }
                        },
                        enabled = !isUpdating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Lưu cài đặt phân quyền",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
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
    enabled: Boolean = true,
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
                    .background(if (enabled) Color(0xFFF3F4F6) else Color(0xFFFAFAFA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF4B5563) else Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color(0xFF1F2937) else Color(0xFF6B7280)
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
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
