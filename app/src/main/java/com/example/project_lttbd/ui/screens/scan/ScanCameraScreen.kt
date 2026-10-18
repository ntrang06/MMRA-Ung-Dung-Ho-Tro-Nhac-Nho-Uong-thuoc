package com.example.project_lttbd.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.project_lttbd.ui.components.MMRAButton

@Composable
fun ScanCameraScreen(
    onCaptureClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onManualInputClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onCaptureClick()
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F201C))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Nút đóng
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onCloseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tiêu đề & Bước
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quét vỏ hộp thuốc",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "Bước 1 / 3 • Nhận diện thuốc",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9FBDB6),
                            fontSize = 12.sp
                        )
                    )
                }

                // Nút bật/tắt Flash
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isFlashOn) Color(0xFF126B62) else Color(0x33FFFFFF))
                        .clickable { isFlashOn = !isFlashOn },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFlashOn) "⚡" else "⚡",
                        color = if (isFlashOn) Color.Yellow else Color.White,
                        fontSize = 18.sp
                    )
                }
            }

            // Viewfinder Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Badge nhắc nhở
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1D3B34)
                    ) {
                        Text(
                            text = "⛶ GIỮ MÁY ỔN ĐỊNH",
                            color = Color(0xFF85D5C7),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Đặt toàn bộ mặt trước vỏ hộp vào khung",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Khung căn chỉnh vỏ hộp thuốc
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(220.dp)
                            .border(
                                width = 2.dp,
                                color = Color(0x80FFFFFF),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(Color(0x1A000000), shape = RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Đường kẻ ngang dẫn hướng căn giữa
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(1.dp)
                                .background(Color(0x66FFFFFF))
                        )

                        // Các góc định vị
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .size(width = 24.dp, height = 4.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(width = 24.dp, height = 4.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .size(width = 24.dp, height = 4.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(width = 24.dp, height = 4.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Gợi ý ánh sáng
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1D3B34)
                    ) {
                        Text(
                            text = "☀ Đủ sáng • Không bị lóa",
                            color = Color(0xFFB1CCC4),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Action Sheet
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Nút chụp to ở giữa
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                if (hasCameraPermission) {
                                    onCaptureClick()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📷", fontSize = 28.sp)
                        }
                    }

                    // Nút nhập thông tin thủ công
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onManualInputClick() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⌨ ", fontSize = 16.sp)
                        Text(
                            text = "Nhập thông tin thủ công",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Nút chọn ảnh từ thư viện
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickImageClick() }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🖼 ", fontSize = 16.sp)
                        Text(
                            text = "Hoặc chọn ảnh từ thư viện",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }
    }
}
