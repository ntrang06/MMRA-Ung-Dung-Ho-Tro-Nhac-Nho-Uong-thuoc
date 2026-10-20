package com.example.project_lttbd.ui.screens.reminder

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.theme.MMRATheme

data class ReminderNoticeUiModel(
    val title: String,
    val message: String
)

@Composable
fun DoseReminderScreen(
    dose: ReminderDoseUiModel,
    onClose: () -> Unit,
    onTaken: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onSkip: (String) -> Unit,
    onPlayInstructions: () -> Unit,
    modifier: Modifier = Modifier,
    notice: ReminderNoticeUiModel? = null,
    actionsEnabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A4D47))
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                top = 10.dp,
                end = 20.dp,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ReminderHeader(
            timeLabel = dose.timeLabel,
            onClose = onClose
        )

        DoseReminderCard(
            dose = dose,
            onPlayInstructions = onPlayInstructions
        )

        if (notice != null) {
            ReminderNoticeCard(notice = notice)
        }

        ReminderActionButton(
            label = "Tôi đã uống",
            iconRes = R.drawable.dau_tich,
            onClick = { onTaken(dose.doseId) },
            enabled = actionsEnabled,
            primary = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReminderActionButton(
                label = "Nhắc lại 10 phút",
                iconRes = R.drawable.dongho,
                onClick = { onSnooze(dose.doseId) },
                enabled = actionsEnabled,
                modifier = Modifier.weight(1.6f)
            )

            ReminderActionButton(
                label = "Bỏ qua",
                iconRes = R.drawable.bo_qua,
                onClick = { onSkip(dose.doseId) },
                enabled = actionsEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReminderHeader(
    timeLabel: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.14f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.chuong),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Đến giờ uống thuốc",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )

            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp
                ),
                color = Color(0xFFD9EEEA)
            )
        }

        Surface(
            onClick = onClose,
            modifier = Modifier
                .size(44.dp)
                .semantics {
                    contentDescription = "Đóng màn nhắc liều"
                },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.14f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ReminderNoticeCard(
    notice: ReminderNoticeUiModel
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF1D6),
        border = BorderStroke(1.dp, Color(0xFFAA5B00))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.canhbao),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFAA5B00)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A)
                )

                Text(
                    text = notice.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        letterSpacing = 0.sp
                    ),
                    color = Color(0xFF526565)
                )
            }
        }
    }
}

@Composable
private fun ReminderActionButton(
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when {
            !enabled -> Color(0xFFD7E2DF)
            primary -> Color(0xFF126B62)
            else -> Color.White
        },
        contentColor = when {
            !enabled -> Color(0xFF526565)
            primary -> Color.White
            else -> Color(0xFF126B62)
        },
        border = if (primary) {
            null
        } else {
            BorderStroke(1.dp, Color(0xFF126B62))
        }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 54.dp)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(
                8.dp,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun DoseReminderScreenPreview() {
    MMRATheme {
        DoseReminderScreen(
            dose = ReminderDoseUiModel(
                doseId = "dose_02",
                medicineId = "medicine_02",
                timeLabel = "13:00 • Hôm nay",
                medicineName = "Metformin 500 mg",
                quantityText = "1 viên",
                mealInstruction = "CÙNG BỮA TRƯA",
                audioDurationLabel = "0:08"
            ),
            notice = ReminderNoticeUiModel(
                title = "Chưa ghi nhận liều trùng",
                message = "Liều gần nhất đã uống lúc 08:03. " +
                        "Nếu bạn đã uống liều này, không uống thêm."
            ),
            onClose = {},
            onTaken = {},
            onSnooze = {},
            onSkip = {},
            onPlayInstructions = {}
        )
    }
}