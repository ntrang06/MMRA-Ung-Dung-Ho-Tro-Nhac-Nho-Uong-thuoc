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
import androidx.compose.material3.TextButton
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

@Composable
fun ManageUsageModeScreen(
    modifier: Modifier = Modifier,
    initialSelectedRoles: Set<TargetRole> = setOf(TargetRole.PATIENT),
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
    val unselectedCheckboxBorder = Color(0xFFCDE0DC)

    var localSelectedRoles by remember { mutableStateOf(initialSelectedRoles) }
    var showConfirmDialog by remember { mutableStateOf(false) }

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
                    Text(
                        text = stringResource(id = R.string.account_section_manage),
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Icon Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = lightBgColor,
                modifier = Modifier.size(56.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_user),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Thay đổi chế độ sử dụng",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = darkTextColor,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Chọn vai trò phù hợp để MMRA tối ưu hóa giao diện và thông báo cho bạn.",
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: Người uống thuốc (Patient)
            val isPatientSelected = localSelectedRoles.contains(TargetRole.PATIENT)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isPatientSelected) lightBgColor else Color.White,
                border = BorderStroke(
                    if (isPatientSelected) 1.5.dp else 1.dp,
                    if (isPatientSelected) primaryColor else borderColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        localSelectedRoles = if (isPatientSelected) {
                            localSelectedRoles - TargetRole.PATIENT
                        } else {
                            localSelectedRoles + TargetRole.PATIENT
                        }
                    },
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
                            text = stringResource(id = R.string.setup_target_option_user_title),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.setup_target_option_user_desc),
                            fontSize = 13.sp,
                            color = subTextColor,
                            lineHeight = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isPatientSelected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_circle),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, unselectedCheckboxBorder),
                            modifier = Modifier.size(24.dp),
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Người chăm sóc (Caregiver)
            val isCaregiverSelected = localSelectedRoles.contains(TargetRole.CAREGIVER)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isCaregiverSelected) lightBgColor else Color.White,
                border = BorderStroke(
                    if (isCaregiverSelected) 1.5.dp else 1.dp,
                    if (isCaregiverSelected) primaryColor else borderColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        localSelectedRoles = if (isCaregiverSelected) {
                            localSelectedRoles - TargetRole.CAREGIVER
                        } else {
                            localSelectedRoles + TargetRole.CAREGIVER
                        }
                    },
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
                            text = stringResource(id = R.string.setup_target_option_caregiver_title),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.setup_target_option_caregiver_desc),
                            fontSize = 13.sp,
                            color = subTextColor,
                            lineHeight = 18.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isCaregiverSelected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_circle),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, unselectedCheckboxBorder),
                            modifier = Modifier.size(24.dp),
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Button: Lưu thay đổi
            Button(
                onClick = {
                    if (localSelectedRoles.isNotEmpty()) {
                        if (localSelectedRoles != initialSelectedRoles) {
                            showConfirmDialog = true
                        } else {
                            onConfirmSave(localSelectedRoles)
                        }
                    }
                },
                enabled = localSelectedRoles.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFFA2C0BB),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lưu thay đổi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Action: Hủy -> Quay về MyAccountScreen không lưu thay đổi
            Text(
                text = stringResource(id = R.string.avatar_action_cancel),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = subTextColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onCancelClick() },
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        text = "Thay đổi chế độ sử dụng sẽ cập nhật giao diện và các thông báo tương ứng trên ứng dụng.",
                        fontSize = 13.sp,
                        color = subTextColor,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Confirmation Dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = {
                    Text(
                        text = "Xác nhận thay đổi chế độ sử dụng?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                },
                text = {
                    Text(
                        text = "Giao diện và thông báo của ứng dụng sẽ thay đổi theo vai trò bạn đã chọn.",
                        fontSize = 14.sp,
                        color = subTextColor,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            onConfirmSave(localSelectedRoles)
                        },
                    ) {
                        Text(
                            text = "Đồng ý",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            // Dialog 'Quay lại' button closes dialog, remains on screen, keeps unsaved changes
                            showConfirmDialog = false
                        },
                    ) {
                        Text(
                            text = "Quay lại",
                            fontSize = 15.sp,
                            color = subTextColor,
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ManageUsageModeScreenPreview() {
    ManageUsageModeScreen()
}