package com.example.project_lttbd.ui.screens.reminder

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.theme.MMRATheme

@Composable
fun DoseReminderCard(
    dose: ReminderDoseUiModel,
    onPlayInstructions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val imageModifier = Modifier
                .widthIn(max = 180.dp)
                .fillMaxWidth()
                .aspectRatio(180f / 138f)
                .clip(RoundedCornerShape(18.dp))

            val imageRes = dose.imageRes

            if (imageRes != null) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = "Ảnh ${dose.medicineName}",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = imageModifier,
                    color = Color(0xFFEEF5F3)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.thuoc),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFF126B62)
                        )

                        Text(
                            text = "Ảnh thuốc",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF526565)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dose.medicineName,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        lineHeight = 29.9.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF102A2A),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = dose.quantityText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 38.sp,
                        lineHeight = 46.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF126B62),
                    textAlign = TextAlign.Center
                )

                if (dose.mealInstruction.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFEDF5FF)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.an),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF2E6FAD)
                            )

                            Text(
                                text = dose.mealInstruction,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    letterSpacing = 0.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF2E6FAD)
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = onPlayInstructions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEEF5F3)
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = Color(0xFF126B62)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.loa),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Text(
                        text = "Nghe hướng dẫn bằng giọng nói",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF102A2A)
                    )

                    dose.audioDurationLabel?.let { duration ->
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color(0xFF526565)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun DoseReminderCardPreview() {
    MMRATheme {
        Surface(
            color = Color(0xFF0A4D47)
        ) {
            DoseReminderCard(
                dose = ReminderDoseUiModel(
                    doseId = "dose_02",
                    medicineId = "medicine_02",
                    timeLabel = "13:00 • Hôm nay",
                    medicineName = "Metformin 500 mg",
                    quantityText = "1 viên",
                    mealInstruction = "CÙNG BỮA TRƯA",
                    audioDurationLabel = "0:08"
                ),
                onPlayInstructions = {},
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}