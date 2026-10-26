package com.example.mmra_medicationmanager.ui.screens.profile

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R
import com.example.mmra_medicationmanager.model.UserProfileManager

@Composable
fun MyAccountScreen(
    modifier: Modifier = Modifier,
    avatarBitmap: Bitmap? = UserProfileManager.currentAvatarBitmap,
    onBackClick: () -> Unit = {},
    onAvatarOptionsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val logoutRedColor = Color(0xFFD32F2F)
    val logoutBgColor = Color(0xFFFDE8E8)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                        text = stringResource(id = R.string.account_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = stringResource(id = R.string.account_subtitle),
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                ) {
                    // Avatar
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = lightBgColor,
                            modifier = Modifier.size(80.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text(
                                    text = "NL",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = UserProfileManager.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = UserProfileManager.role,
                        fontSize = 14.sp,
                        color = subTextColor,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Verified Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = lightBgColor,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_circle),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.account_verified),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = UserProfileManager.email,
                        fontSize = 13.sp,
                        color = subTextColor,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = UserProfileManager.phone,
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Thông tin cá nhân
            SectionHeader(title = stringResource(id = R.string.account_section_personal))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AccountSettingRow(
                        iconRes = R.drawable.ic_camera,
                        title = stringResource(id = R.string.account_change_avatar),
                        onClick = onAvatarOptionsClick,
                    )
                    Divider(color = borderColor, thickness = 0.8.dp)
                    AccountSettingRow(
                        iconRes = R.drawable.ic_user,
                        title = stringResource(id = R.string.account_edit_name),
                        subtitle = UserProfileManager.name,
                    )
                    Divider(color = borderColor, thickness = 0.8.dp)
                    AccountSettingRow(
                        iconRes = R.drawable.ic_email,
                        title = stringResource(id = R.string.account_edit_email),
                        subtitle = UserProfileManager.email,
                    )
                    Divider(color = borderColor, thickness = 0.8.dp)
                    AccountSettingRow(
                        iconRes = R.drawable.ic_phone,
                        title = stringResource(id = R.string.account_edit_phone),
                        subtitle = UserProfileManager.phone,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Bảo mật
            SectionHeader(title = stringResource(id = R.string.account_section_security))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AccountSettingRow(
                    iconRes = R.drawable.ic_lock,
                    title = stringResource(id = R.string.account_change_password),
                    subtitle = stringResource(id = R.string.account_change_password_sub),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Nhắc nhở và trợ năng
            SectionHeader(title = stringResource(id = R.string.account_section_reminders))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AccountSettingRow(
                        iconRes = R.drawable.ic_bell,
                        title = stringResource(id = R.string.account_notifications),
                        subtitle = stringResource(id = R.string.account_notifications_sub),
                    )
                    Divider(color = borderColor, thickness = 0.8.dp)
                    AccountSettingRow(
                        iconRes = R.drawable.ic_text_size,
                        title = stringResource(id = R.string.account_accessibility),
                        subtitle = stringResource(id = R.string.account_accessibility_sub),
                    )
                    Divider(color = borderColor, thickness = 0.8.dp)
                    AccountSettingRow(
                        iconRes = R.drawable.ic_speaker,
                        title = stringResource(id = R.string.account_voice_guidance),
                        subtitle = stringResource(id = R.string.account_voice_guidance_sub),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: Quản lý tài khoản
            SectionHeader(title = stringResource(id = R.string.account_section_manage))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AccountSettingRow(
                        iconRes = R.drawable.ic_text_size,
                        title = stringResource(id = R.string.account_usage_mode),
                        subtitle = stringResource(id = R.string.account_usage_mode_sub),
                    )
                    Divider(color = borderColor, thickness = 0.8.dp)
                    AccountSettingRow(
                        iconRes = R.drawable.ic_caregiver,
                        title = stringResource(id = R.string.account_switch_account),
                    )
                    Divider(color = borderColor, thickness = 0.8.dp)
                    AccountSettingRow(
                        iconRes = R.drawable.ic_logout,
                        title = stringResource(id = R.string.account_logout),
                        titleColor = logoutRedColor,
                        iconBgColor = logoutBgColor,
                        iconTint = logoutRedColor,
                        onClick = onLogoutClick,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(id = R.string.account_footer_note),
                fontSize = 12.sp,
                color = subTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0A3C35),
    )
}

@Composable
private fun AccountSettingRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color(0xFF0A3C35),
    iconBgColor: Color = Color(0xFFEFF7F4),
    iconTint: Color = Color(0xFF0F5B51),
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconBgColor,
            modifier = Modifier.size(40.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF5A6E6A),
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Color(0xFF8FA39E),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyAccountScreenPreview() {
    MyAccountScreen()
}