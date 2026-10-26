package com.example.mmra_medicationmanager.ui.screens.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R
import com.example.mmra_medicationmanager.model.TargetRole
import com.example.mmra_medicationmanager.model.UserProfileManager

enum class UsageModeType {
    PATIENT_ONLY,
    CAREGIVER_ONLY,
    COMBINED,
}

private fun Set<TargetRole>.toUsageModeType(): UsageModeType {
    return when {
        contains(TargetRole.PATIENT) && contains(TargetRole.CAREGIVER) -> UsageModeType.COMBINED
        contains(TargetRole.CAREGIVER) -> UsageModeType.CAREGIVER_ONLY
        else -> UsageModeType.PATIENT_ONLY
    }
}

private fun UsageModeType.toTargetRoleSet(): Set<TargetRole> {
    return when (this) {
        UsageModeType.PATIENT_ONLY -> setOf(TargetRole.PATIENT)
        UsageModeType.CAREGIVER_ONLY -> setOf(TargetRole.CAREGIVER)
        UsageModeType.COMBINED -> setOf(TargetRole.PATIENT, TargetRole.CAREGIVER)
    }
}

@Composable
fun ManageUsageModeScreen(
    modifier: Modifier = Modifier,
    initialSelectedRoles: Set<TargetRole> = setOf(TargetRole.PATIENT, TargetRole.CAREGIVER),
    onBackClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onConfirmSave: (Set<TargetRole>) -> Unit = {},
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val infoBgColor = Color(0xFFEFF6F4)
    val unselectedRadioBorder = Color(0xFF5A6E6A)

    val savedMode = initialSelectedRoles.toUsageModeType()
    var tempSelectedMode by remember { mutableStateOf(savedMode) }
    var pendingDialogMode by remember { mutableStateOf<UsageModeType?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = lightBgColor,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onBackClick() },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = stringResource(id = R.string.cd_back_button),
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.account_usage_mode),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Chọn một chế độ phù hợp với cách bạn sử dụng MMRA.",
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Option 1: Chỉ Người uống thuốc
            val isPatientOnlySelected = tempSelectedMode == UsageModeType.PATIENT_ONLY
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isPatientOnlySelected) lightBgColor else Color.White,
                border = BorderStroke(
                    if (isPatientOnlySelected) 1.5.dp else 1.dp,
                    if (isPatientOnlySelected) primaryColor else borderColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { tempSelectedMode = UsageModeType.PATIENT_ONLY },
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pill),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(26.dp)
                            .padding(top = 2.dp),
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Chỉ Người uống thuốc",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        if (savedMode == UsageModeType.PATIENT_ONLY) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chế độ hiện tại",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Quản lý thuốc của mình và nhận nhắc nhở uống thuốc đúng giờ.",
                            fontSize = 13.sp,
                            color = subTextColor,
                            lineHeight = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Radio indicator
                    RadioButtonIndicator(isSelected = isPatientOnlySelected, primaryColor = primaryColor, unselectedBorder = unselectedRadioBorder)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: Chỉ Người chăm sóc
            val isCaregiverOnlySelected = tempSelectedMode == UsageModeType.CAREGIVER_ONLY
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isCaregiverOnlySelected) lightBgColor else Color.White,
                border = BorderStroke(
                    if (isCaregiverOnlySelected) 1.5.dp else 1.dp,
                    if (isCaregiverOnlySelected) primaryColor else borderColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { tempSelectedMode = UsageModeType.CAREGIVER_ONLY },
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_caregiver),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(26.dp)
                            .padding(top = 2.dp),
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Chỉ Người chăm sóc",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        if (savedMode == UsageModeType.CAREGIVER_ONLY) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chế độ hiện tại",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Theo dõi các thành viên gia đình đã liên kết trong phạm vi quyền được cấp.",
                            fontSize = 13.sp,
                            color = subTextColor,
                            lineHeight = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    RadioButtonIndicator(isSelected = isCaregiverOnlySelected, primaryColor = primaryColor, unselectedBorder = unselectedRadioBorder)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 3: Kết hợp cả hai
            val isCombinedSelected = tempSelectedMode == UsageModeType.COMBINED
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isCombinedSelected) lightBgColor else Color.White,
                border = BorderStroke(
                    if (isCombinedSelected) 1.5.dp else 1.dp,
                    if (isCombinedSelected) primaryColor else borderColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { tempSelectedMode = UsageModeType.COMBINED },
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_caregiver),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(26.dp)
                            .padding(top = 2.dp),
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Kết hợp cả hai",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        if (savedMode == UsageModeType.COMBINED) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chế độ hiện tại",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Quản lý thuốc của mình và chăm sóc người thân trong cùng một tài khoản.",
                            fontSize = 13.sp,
                            color = subTextColor,
                            lineHeight = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    RadioButtonIndicator(isSelected = isCombinedSelected, primaryColor = primaryColor, unselectedBorder = unselectedRadioBorder)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = infoBgColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info_circle),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Thay đổi chế độ không tự động xóa dữ liệu thuốc, hủy liên kết gia đình hoặc thu hồi quyền chia sẻ.",
                        fontSize = 13.sp,
                        color = subTextColor,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Button: Lưu thay đổi
            Button(
                onClick = {
                    if (tempSelectedMode != savedMode) {
                        pendingDialogMode = tempSelectedMode
                    } else {
                        onConfirmSave(tempSelectedMode.toTargetRoleSet())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = "Lưu thay đổi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Outlined Button: Hủy
            OutlinedButton(
                onClick = { onCancelClick() },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = "Hủy",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dialogs A, B, C
        if (pendingDialogMode != null) {
            when (pendingDialogMode) {
                UsageModeType.PATIENT_ONLY -> {
                    // Dialog A: Chuyển sang chỉ Người uống thuốc?
                    AlertDialog(
                        onDismissRequest = { pendingDialogMode = null },
                        title = {
                            Text(
                                text = "Chuyển sang chỉ Người uống thuốc?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkTextColor,
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Các tính năng chăm sóc người thân sẽ được ẩn. Bạn vẫn có thể quản lý thuốc của mình và nhận nhắc nhở uống thuốc.",
                                    fontSize = 14.sp,
                                    color = subTextColor,
                                    lineHeight = 20.sp,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Dữ liệu cá nhân, liên kết gia đình và các quyền hiện có đều được giữ nguyên.",
                                    fontSize = 14.sp,
                                    color = subTextColor,
                                    lineHeight = 20.sp,
                                )
                            }
                        },
                        confirmButton = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val newRoles = UsageModeType.PATIENT_ONLY.toTargetRoleSet()
                                        UserProfileManager.role = "Người uống thuốc"
                                        pendingDialogMode = null
                                        onConfirmSave(newRoles)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(
                                        text = "Chuyển sang Người uống thuốc",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { pendingDialogMode = null },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(
                                        text = "Quay lại",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = darkTextColor,
                                    )
                                }
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp),
                    )
                }

                UsageModeType.CAREGIVER_ONLY -> {
                    // Dialog B: Chuyển sang chỉ Người chăm sóc?
                    AlertDialog(
                        onDismissRequest = { pendingDialogMode = null },
                        title = {
                            Text(
                                text = "Chuyển sang chỉ Người chăm sóc?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkTextColor,
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Các tính năng quản lý thuốc cá nhân sẽ được ẩn. Bạn vẫn có thể chăm sóc người thân trong phạm vi quyền đã được cấp.",
                                    fontSize = 14.sp,
                                    color = subTextColor,
                                    lineHeight = 20.sp,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Dữ liệu cá nhân và liên kết gia đình vẫn được lưu. Các quyền hiện có không thay đổi.",
                                    fontSize = 14.sp,
                                    color = subTextColor,
                                    lineHeight = 20.sp,
                                )
                            }
                        },
                        confirmButton = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val newRoles = UsageModeType.CAREGIVER_ONLY.toTargetRoleSet()
                                        UserProfileManager.role = "Người chăm sóc"
                                        pendingDialogMode = null
                                        onConfirmSave(newRoles)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(
                                        text = "Chuyển sang Người chăm sóc",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { pendingDialogMode = null },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(
                                        text = "Quay lại",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = darkTextColor,
                                    )
                                }
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp),
                    )
                }

                UsageModeType.COMBINED -> {
                    // Dialog C: Bật chế độ kết hợp cả hai?
                    AlertDialog(
                        onDismissRequest = { pendingDialogMode = null },
                        title = {
                            Text(
                                text = "Bật chế độ kết hợp cả hai?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkTextColor,
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Bạn sẽ có thể sử dụng cả tính năng quản lý thuốc cá nhân và tính năng chăm sóc người thân.",
                                    fontSize = 14.sp,
                                    color = subTextColor,
                                    lineHeight = 20.sp,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Chế độ này không tự động tạo liên kết gia đình hoặc cấp quyền chia sẻ. Bạn chỉ có thể xem thông tin người thân trong phạm vi quyền đã được cấp.",
                                    fontSize = 14.sp,
                                    color = subTextColor,
                                    lineHeight = 20.sp,
                                )
                            }
                        },
                        confirmButton = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val newRoles = UsageModeType.COMBINED.toTargetRoleSet()
                                        UserProfileManager.role = "Cả bản thân và người thân"
                                        pendingDialogMode = null
                                        onConfirmSave(newRoles)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(
                                        text = "Bật chế độ kết hợp",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { pendingDialogMode = null },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(
                                        text = "Quay lại",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = darkTextColor,
                                    )
                                }
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp),
                    )
                }

                null -> {}
            }
        }
    }
}

@Composable
private fun RadioButtonIndicator(
    isSelected: Boolean,
    primaryColor: Color,
    unselectedBorder: Color,
) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(2.dp, if (isSelected) primaryColor else unselectedBorder),
        modifier = Modifier.size(24.dp),
    ) {
        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = primaryColor,
                    modifier = Modifier.size(12.dp),
                ) {}
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ManageUsageModeScreenPreview() {
    ManageUsageModeScreen()
}