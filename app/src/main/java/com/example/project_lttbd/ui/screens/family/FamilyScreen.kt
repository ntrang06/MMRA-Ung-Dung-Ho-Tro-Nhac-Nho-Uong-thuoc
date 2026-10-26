package com.example.project_lttbd.ui.screens.family

import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R
import com.example.mmra_medicationmanager.model.UserProfileManager
import com.example.project_lttbd.data.mock.FamilyMember
import com.example.project_lttbd.data.mock.MockFamilyData

@Composable
fun FamilyScreen(
    modifier: Modifier = Modifier,
    onMemberClick: (String) -> Unit = {},
    onInviteClick: () -> Unit = {},
    onInviteDetailClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var membersList by remember { mutableStateOf(MockFamilyData.mockMembers) }
    var hasPendingInvite by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6FAF9))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Người thân",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Cùng chăm sóc người thân tốt hơn mỗi ngày.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                // Nút chuông cảnh báo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F2F0))
                            .clickable { onAlertsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bell),
                            contentDescription = "Thông báo",
                            tint = Color(0xFF0F6053),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Thông báo",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Color(0xFF0F6053),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Avatar nhỏ tài khoản
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDDF2EE))
                            .clickable { onAccountClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = UserProfileManager.getInitials(),
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
        }

        // Nội dung cuộn
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Người thân của bạn
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Người thân của bạn",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.alignByBaseline()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F7F2),
                    modifier = Modifier.alignByBaseline()
                ) {
                    Text(
                        text = "Đã liên kết • ${membersList.size}",
                        color = Color(0xFF0F6053),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Danh sách các thẻ thành viên
            membersList.forEach { member ->
                FamilyMemberCard(
                    member = member,
                    onClick = { onMemberClick(member.id) }
                )
            }

            // Section 2: Lời mời người thân
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "XEM TRƯỚC TRẠNG THÁI LỜI MỜI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF0F6053),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Lời mời người thân",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Các trạng thái minh họa, không phải người thân đã liên kết. Chấp nhận kết nối không cấp quyền dữ liệu sức khỏe; cần cấp quyền riêng biệt.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    )
                )
            }

            // Nhận lời mời subsection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "NHẬN LỜI MỜI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF0F6053),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Bạn nhận được lời mời và có thể chọn chấp nhận hoặc từ chối.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                )
            }

            // Lời mời nhận được card
            if (hasPendingInvite) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2ECEA))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
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
                                        text = "Đang chờ phản hồi",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onInviteDetailClick() }
                            ) {
                                Text(
                                    text = "Chi tiết",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_right),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = "Lời mời nhận • Từ Nguyễn Văn A • Gửi hôm nay",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = "Chưa liên kết • Chưa chấp nhận • Chưa chia sẻ dữ liệu",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    hasPendingInvite = false
                                    Toast.makeText(
                                        context,
                                        "Đã chấp nhận kết nối!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chấp nhận", fontSize = 13.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    hasPendingInvite = false
                                    Toast.makeText(
                                        context,
                                        "Đã từ chối lời mời",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = null,
                                        tint = Color(0xFF4B5563),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Từ chối", color = Color(0xFF4B5563), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Subsection: LỜI MỜI ĐÃ GỬI
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "LỜI MỜI ĐÃ GỬI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF0F6053),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Bạn đã gửi lời mời và chỉ có thể xem trạng thái hoặc thực hiện hành động phù hợp.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                )
            }

            // Card 1: Gửi cho Nguyễn Thị B (Đang chờ)
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
                                text = "Đang chờ chấp nhận",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Text(
                        text = "Lời mời mẫu • Gửi cho Nguyễn Thị B • Gửi hôm nay",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = "Chưa chấp nhận • Chưa liên kết • Chưa chia sẻ dữ liệu",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                    )

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Đã hủy lời mời gửi cho Nguyễn Thị B",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = null,
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hủy lời mời", color = Color(0xFF4B5563), fontSize = 13.sp)
                        }
                    }
                }
            }

            // Card 2: Gửi cho Nguyễn Văn C (Đã từ chối)
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_users),
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Đã từ chối",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Text(
                        text = "Lời mời mẫu • Gửi cho Nguyễn Văn C • Người nhận đã từ chối",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = "Không liên kết • Tôn trọng lựa chọn của người nhận",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                    )
                }
            }

            // Card 3: Gửi cho Nguyễn Thị D (Đã hết hạn)
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF3F4F6)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clock),
                                contentDescription = null,
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Đã hết hạn",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF4B5563),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Text(
                        text = "Lời mời mẫu • Gửi cho Nguyễn Thị D • Quá thời hạn chấp nhận",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = "Chưa liên kết • Có thể gửi lời mời mới",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                    )

                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Đã gửi lại lời mời cho Nguyễn Thị D!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_refresh),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gửi lại", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Section 3: XEM TRƯỚC TRẠNG THÁI THAY THẾ
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "XEM TRƯỚC TRẠNG THÁI THAY THẾ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF0F6053),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Khi chưa liên kết người thân — không phải trạng thái hiện tại của dữ liệu minh họa ở trên.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    )
                )
            }

            // Thẻ Mời người thân mới (Empty state preview)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2ECEA))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F7F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_users),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Cùng chăm sóc, thêm an tâm",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Bạn chưa liên kết người thân. Mời người thân để cùng theo dõi lịch uống thuốc nhé.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onInviteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_user_plus),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mời người thân",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FamilyMemberCard(
    member: FamilyMember,
    onClick: () -> Unit
) {
    val initials = member.fullName.split(" ")
        .takeLast(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")

    val progress = if (member.todayTotalCount > 0) {
        member.todayTakenCount.toFloat() / member.todayTotalCount.toFloat()
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2ECEA))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDDF2EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${member.relationship} • Hồ sơ theo dõi",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tiến độ hôm nay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đã xác nhận ${member.todayTakenCount}/${member.todayTotalCount} liều hôm nay",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151)
                    )
                )

                val statusPillText = if (member.hasLateDose) "Cần chú ý" else "Chờ xác nhận"
                val statusPillBg = if (member.hasLateDose) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                val statusPillColor =
                    if (member.hasLateDose) Color(0xFFDC2626) else Color(0xFF92400E)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusPillBg
                ) {
                    Text(
                        text = statusPillText,
                        color = statusPillColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (member.hasLateDose) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE5E7EB)
            )

            Text(
                text = "${member.todayTakenCount} đã xác nhận • 1 đang chờ • ${if (member.hasLateDose) 1 else 0} quá giờ chưa xác nhận",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp
                )
            )

            // Cảnh báo nếu có liều trễ
            if (member.hasLateDose) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF1F2),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            tint = Color(0xFFBE123C),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Liều 08:00 quá giờ chưa xác nhận",
                            color = Color(0xFFBE123C),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}