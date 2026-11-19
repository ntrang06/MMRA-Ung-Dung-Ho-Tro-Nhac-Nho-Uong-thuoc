package com.example.project_lttbd.ui.screens.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.components.MedicinePhoto

@Composable
fun ScanProcessingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUri: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F2F0))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = R.drawable.ic_arrow_back
                    ),
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = "Đọc chữ trên ảnh thuốc",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )

                Text(
                    text = "Vui lòng chờ trong giây lát.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                MedicinePhoto(
                    source = imageUri,
                    contentDescription = "Ảnh thuốc đang đọc chữ",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    maxSide = 1024
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .border(
                            width = 1.5.dp,
                            color = Color(0xB3FFFFFF),
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                Surface(
                    modifier = Modifier
                        .padding(14.dp)
                        .align(Alignment.BottomStart),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xCC0F201C)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable.ic_scan
                            ),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = "ĐANG ĐỌC CHỮ",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFE2ECEA)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(72.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F7F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_scan
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Text(
                        text = "Đang đọc chữ hộp/vỉ thuốc…",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Vui lòng chờ trong giây lát. " +
                                "Bạn sẽ kiểm tra thông tin trước khi lưu.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = Color(0xFFF3F4F6)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDDF2EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_scan
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "Đọc chữ",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "Trích xuất thông tin",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEFF8F6)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            id = R.drawable.ic_scan
                        ),
                        contentDescription = null,
                        tint = Color(0xFF1E8272),
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = "Chữ đọc được sẽ được đưa vào form " +
                                "để bạn kiểm tra trước khi lưu.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF1E8272),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}