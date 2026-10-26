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
fun SwitchAccountScreen(
    modifier: Modifier = Modifier,
    avatarBitmap: Bitmap? = UserProfileManager.currentAvatarBitmap,
    onBackClick: () -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val infoBgColor = Color(0xFFEFF6F4)

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
                        text = stringResource(id = R.string.account_switch_account),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = "Chọn tài khoản bạn muốn dùng",
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Bạn đang sử dụng tài khoản của cô Lan.",
                fontSize = 14.sp,
                color = subTextColor,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Current Account Card (Selected)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = lightBgColor,
                border = BorderStroke(1.5.dp, primaryColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFC0DDD6),
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text(
                                    text = "NL",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = UserProfileManager.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkTextColor,
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_circle),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = UserProfileManager.role,
                            fontSize = 13.sp,
                            color = subTextColor,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = UserProfileManager.email,
                            fontSize = 13.sp,
                            color = subTextColor,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Đang sử dụng",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Outlined Button: Thêm tài khoản / Đăng nhập
            OutlinedButton(
                onClick = onAddAccountClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_user),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thêm tài khoản / Đăng nhập",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                    )
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
                        painter = painterResource(id = R.drawable.ic_shield_check),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đăng nhập tài khoản khác bằng Google, số điện thoại hoặc email. Thông tin và lựa chọn của mỗi tài khoản được giữ riêng.",
                        fontSize = 13.sp,
                        color = subTextColor,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Hủy — giữ tài khoản hiện tại",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onCancelClick() },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SwitchAccountScreenPreview() {
    SwitchAccountScreen()
}