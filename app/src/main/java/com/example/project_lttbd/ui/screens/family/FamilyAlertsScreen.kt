package com.example.project_lttbd.ui.screens.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.data.mock.CaregiverAlert
import com.example.project_lttbd.data.mock.MockAlertData

@Composable
fun FamilyAlertsScreen(
    onBackClick: () -> Unit,
    onAlertClick: (CaregiverAlert) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var alertsList by remember { mutableStateOf(MockAlertData.mockAlerts) }

    val filteredAlerts = when (selectedFilter) {
        "Chưa xử lý" -> alertsList.filter { !it.isRead }
        "Đã đọc" -> alertsList.filter { it.isRead }
        else -> alertsList
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF9))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F2F0))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cảnh báo người thân",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "Cùng theo dõi, thêm an tâm",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp
                    )
                )
            }

            // Avatar MA
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDDF2EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MA",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tài khoản",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = Color(0xFF0F6053),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Nội dung cuộn
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card chọn người thân
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2ECEA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDF2EE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TM",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trần Văn Minh",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Bố • Hồ sơ minh họa",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                            )
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_down),
                            contentDescription = "Chọn người thân",
                            tint = Color(0xFF0F6053),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F7F2)
                        ) {
                            Text(
                                text = "Đã liên kết",
                                color = Color(0xFF0F6053),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDDF2EE)
                        ) {
                            Text(
                                text = "Quyền xem đang hiệu lực",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Banner thông báo cần kiểm tra
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF8F6)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1 cảnh báo cần kiểm tra",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F6053)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Liều của bố quá giờ nhưng chưa có xác nhận. Hãy liên hệ để biết tình hình.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF1E8272),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            // Thanh lọc Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTab(
                    title = "Tất cả • 3",
                    isSelected = selectedFilter == "Tất cả",
                    onClick = { selectedFilter = "Tất cả" },
                    modifier = Modifier.weight(1f)
                )
                FilterTab(
                    title = "Chưa xử lý • 1",
                    isSelected = selectedFilter == "Chưa xử lý",
                    onClick = { selectedFilter = "Chưa xử lý" },
                    modifier = Modifier.weight(1.1f)
                )
                FilterTab(
                    title = "Đã đọc • 2",
                    isSelected = selectedFilter == "Đã đọc",
                    onClick = { selectedFilter = "Đã đọc" },
                    modifier = Modifier.weight(1f)
                )
            }

            // Subheader
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Thông báo của bố",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "02/10 • 1 cần xử lý, 2 thông tin đã đọc",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                )
            }

            // Card 1: Liều 08:00 chưa được xác nhận (Chưa xử lý)
            if (selectedFilter == "Tất cả" || selectedFilter == "Chưa xử lý") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val alert = alertsList.firstOrNull() ?: MockAlertData.mockAlerts.first()
                            onAlertClick(alert)
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDC2626))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_clock),
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Chưa xử lý",
                                        color = Color(0xFFDC2626),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "Chưa đọc",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF0F6053),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(
                            text = "Liều 08:00 chưa được xác nhận",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = "Trần Văn Minh (Bố) • Thứ Sáu, 02/10",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                        )

                        Text(
                            text = "Amlodipine 5 mg • 1 viên • Sau ăn sáng",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151)
                            )
                        )

                        Text(
                            text = "Quá giờ • Chưa có xác nhận từ bố.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            )
                        )

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = Color(0xFFF3F4F6)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Xem chi tiết cảnh báo",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Card 2: Liều 20:30 đang chờ xác nhận (Đã đọc • Sắp tới)
            if (selectedFilter == "Tất cả" || selectedFilter == "Đã đọc") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2ECEA))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_clock),
                                        contentDescription = null,
                                        tint = Color(0xFF92400E),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sắp tới • Thông tin",
                                        color = Color(0xFF92400E),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "Đã đọc",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(
                            text = "Liều 20:30 đang chờ xác nhận",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = "Trần Văn Minh (Bố) • Thứ Sáu, 02/10",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                        )

                        Text(
                            text = "Atorvastatin 20 mg • 1 viên • Sau ăn tối",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151)
                            )
                        )

                        Text(
                            text = "Chưa đến giờ • Không phải liều quá giờ.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Card 3: Đã liên kết hồ sơ của bố
            if (selectedFilter == "Tất cả" || selectedFilter == "Đã đọc") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2ECEA))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F7F2)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_users),
                                        contentDescription = null,
                                        tint = Color(0xFF0F6053),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cập nhật kết nối",
                                        color = Color(0xFF0F6053),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "Đã đọc",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(
                            text = "Đã liên kết hồ sơ của bố",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = "Cập nhật gia đình • Hôm qua, 01/10",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                        )

                        Text(
                            text = "Kết nối với Trần Văn Minh đã được thiết lập.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF374151))
                        )

                        Text(
                            text = "Quyền dữ liệu được cấp riêng cho kết nối.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Gợi ý về trạng thái đọc
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF3F4F6)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Đọc thông báo ≠ xác nhận liều",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đánh dấu đã đọc chỉ thay đổi trạng thái thông báo, không xác nhận uống thuốc hay xử lý trạng thái liều.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Text(
                text = "Dữ liệu sức khỏe minh họa • Không phải dữ liệu thật hay hướng dẫn điều trị.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FilterTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                color = if (isSelected) Color.White else Color(0xFF4B5563),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp
                )
            )
        }
    }
}
