package com.example.project_lttbd.ui.screens.account

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import com.example.project_lttbd.R
import com.example.project_lttbd.data.model.UserProfileManager

@Composable
fun AvatarOptionsScreen(
    modifier: Modifier = Modifier,
    avatarBitmap: Bitmap? = UserProfileManager.currentAvatarBitmap,
    onTakePhotoClick: () -> Unit = {},
    onPickGalleryClick: () -> Unit = {},
    onDeletePhotoClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val redTextColor = Color(0xFFD32F2F)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Current Avatar
            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape),
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = lightBgColor,
                    modifier = Modifier.size(110.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = "NL",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = UserProfileManager.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = darkTextColor,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (avatarBitmap != null) stringResource(id = R.string.avatar_option_current_sub) else stringResource(id = R.string.avatar_option_default_sub),
                fontSize = 13.sp,
                color = subTextColor,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Panel with 4 Action Buttons
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    // Action 1: Chụp ảnh
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onTakePhotoClick() },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_camera),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(id = R.string.avatar_action_take_photo),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkTextColor,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action 2: Chọn từ thư viện
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onPickGalleryClick() },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_gallery),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(id = R.string.avatar_action_pick_gallery),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkTextColor,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action 3: Xóa ảnh
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onDeletePhotoClick() },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_trash),
                                contentDescription = null,
                                tint = redTextColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(id = R.string.avatar_action_delete_photo),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = redTextColor,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action 4: Hủy
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onCancelClick() },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = stringResource(id = R.string.avatar_action_cancel),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkTextColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AvatarOptionsScreenPreview() {
    AvatarOptionsScreen()
}