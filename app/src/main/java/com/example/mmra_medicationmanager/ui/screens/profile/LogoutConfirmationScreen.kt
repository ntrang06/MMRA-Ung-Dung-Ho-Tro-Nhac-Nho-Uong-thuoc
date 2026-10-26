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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R
import com.example.mmra_medicationmanager.model.UserProfileManager

@Composable
fun LogoutConfirmationScreen(
    modifier: Modifier = Modifier,
    avatarBitmap: Bitmap? = UserProfileManager.currentAvatarBitmap,
    onBackClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onConfirmLogout: () -> Unit = {},
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val logoutRedColor = Color(0xFFD32F2F)
    val warningBgColor = Color(0xFFFFF3E0)
    val warningBorderColor = Color(0xFFFFE0B2)
    val warningIconColor = Color(0xFFE65100)

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
                        text = stringResource(id = R.string.account_logout),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = "Xác nhận trước khi rời tài khoản",
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
                        painter = painterResource(id = R.drawable.ic_logout),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Cô Lan muốn đăng xuất?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = darkTextColor,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Account Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = lightBgColor,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text(
                                    text = "NL",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = UserProfileManager.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = UserProfileManager.phone,
                            fontSize = 13.sp,
                            color = subTextColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Bạn sẽ cần đăng nhập lại để tiếp tục dùng tài khoản này. Thông tin và các lựa chọn đã lưu vẫn được giữ lại.",
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Warning Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = warningBgColor,
                border = BorderStroke(1.dp, warningBorderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = null,
                        tint = warningIconColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hãy đăng nhập lại để tiếp tục nhận nhắc nhở của tài khoản cô Lan.",
                        fontSize = 13.sp,
                        color = Color(0xFF6D4C41),
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Neutral Button: Hủy — ở lại tài khoản
            Button(
                onClick = onCancelClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = "Hủy — ở lại tài khoản",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destructive Outlined Button: Đăng xuất
            OutlinedButton(
                onClick = onConfirmLogout,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, logoutRedColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logout),
                        contentDescription = null,
                        tint = logoutRedColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.account_logout),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = logoutRedColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LogoutConfirmationScreenPreview() {
    LogoutConfirmationScreen()
}