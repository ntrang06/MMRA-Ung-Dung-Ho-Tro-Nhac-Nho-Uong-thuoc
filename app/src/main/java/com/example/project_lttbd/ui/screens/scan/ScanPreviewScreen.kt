package com.example.project_lttbd.ui.screens.scan

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.data.scan.ScanCropRegion
import com.example.project_lttbd.ui.components.MedicineScanCropper

@Composable
fun ScanPreviewScreen(
    imageUri: Uri?,
    isFromGallery: Boolean,
    onRetakeClick: () -> Unit,
    onUsePhotoClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    cropRegion: ScanCropRegion = ScanCropRegion.Full,
    onCropRegionChange: (ScanCropRegion) -> Unit = {}
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBackClick,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFFE8F2F0)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Quay lại",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = "Khoanh vùng hộp/vỉ thuốc",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isFromGallery) {
                    "Ảnh đã chọn từ thư viện"
                } else {
                    "Ảnh vừa chụp"
                },
                color = Color(0xFF526565),
                style = MaterialTheme.typography.bodySmall
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF142523)
            ) {
                MedicineScanCropper(
                    source = imageUri?.toString(),
                    region = cropRegion,
                    onRegionChange = onCropRegionChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }

            TextButton(
                onClick = {
                    onCropRegionChange(ScanCropRegion.Full)
                }
            ) {
                Text("Chọn cả ảnh")
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFE8F5EF)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Giữ tên thuốc và hàm lượng trong khung",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF126B62)
                    )

                    Text(
                        text = "Kéo 4 góc để chỉnh khung, kéo bên trong " +
                                "để di chuyển. Loại logo, nhãn ảnh và chữ " +
                                "ở nền. Chỉ vùng sáng trong khung được đọc.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF526565)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        Surface(
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRetakeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFF126B62)
                    )
                ) {
                    Text(
                        if (isFromGallery) {
                            "Chọn ảnh khác"
                        } else {
                            "Chụp lại ảnh"
                        }
                    )
                }

                Button(
                    onClick = onUsePhotoClick,
                    enabled = imageUri != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF126B62)
                    )
                ) {
                    Text("Đọc phần hộp/vỉ đã chọn")
                }
            }
        }
    }
}