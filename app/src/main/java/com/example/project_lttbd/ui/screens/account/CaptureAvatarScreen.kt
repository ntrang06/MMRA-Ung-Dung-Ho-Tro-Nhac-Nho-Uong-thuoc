package com.example.project_lttbd.ui.screens.account

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.example.project_lttbd.R

@Composable
fun CaptureAvatarScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPhotoCaptured: (Bitmap) -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    val darkTealBg = Color(0xFF07211C)
    val primaryColor = Color(0xFF0F5B51)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    var isFlashOn by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = darkTealBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Dark Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x33FFFFFF),
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
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.avatar_capture_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(id = R.string.avatar_capture_step),
                        fontSize = 13.sp,
                        color = Color(0xFFC0D5D1),
                    )
                }
            }

            // Camera View Box
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFD3E4E0),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Face Positioning Banner
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xCC2C4541),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.avatar_capture_banner),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }

                    // Face Oval Dashed Overlay + Illustration / Preview
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFB5D4CC),
                        border = BorderStroke(2.dp, Color.White),
                        modifier = Modifier.size(220.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_user),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(100.dp),
                            )
                        }
                    }

                    // Illustrative Caption Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.avatar_illustrative_note),
                            fontSize = 12.sp,
                            color = subTextColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // White Control Panel at Bottom
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color(0xFFFAFDFB),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.avatar_capture_guide),
                        fontSize = 14.sp,
                        color = subTextColor,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Flash Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF7F4),
                            modifier = Modifier
                                .size(52.dp)
                                .clickable { isFlashOn = !isFlashOn },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_flash_off),
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        // Large Shutter Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(4.dp, primaryColor),
                            modifier = Modifier
                                .size(76.dp)
                                .clickable {
                                    // Generate sample photo bitmap for captured photo
                                    val capturedBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(capturedBitmap)
                                    canvas.drawColor(android.graphics.Color.parseColor("#E0F0EC"))
                                    val paint = Paint().apply {
                                        color = android.graphics.Color.parseColor("#0F5B51")
                                        isAntiAlias = true
                                    }
                                    canvas.drawCircle(200f, 200f, 150f, paint)
                                    onPhotoCaptured(capturedBitmap)
                                },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = primaryColor,
                                    modifier = Modifier.size(56.dp),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_camera),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Camera Switch Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF7F4),
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_switch_camera),
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.avatar_action_take_photo),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = R.string.avatar_action_cancel),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = subTextColor,
                        modifier = Modifier.clickable { onCancelClick() },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureAvatarScreenPreview() {
    CaptureAvatarScreen()
}