package com.example.project_lttbd.ui.screens.medicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.ui.theme.MMRATheme

@Composable
fun MedicineHeader(
    ownerName: String,
    initials: String,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(
            horizontal = 20.dp,
            vertical = 10.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Thuốc của tôi",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF102A2A)
            )

            Text(
                text = "Danh sách thuốc của $ownerName",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF526565)
            )
        }

        Surface(
            onClick = onAccountClick,
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .heightIn(min = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    3.dp,
                    Alignment.CenterVertically
                )
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFD9EEEA)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF126B62)
                        )
                    }
                }

                Text(
                    text = "Tài khoản",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF126B62)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MedicineHeaderPreview() {
    MMRATheme {
        Surface(
            color = Color(0xFFF5FAF9)
        ) {
            MedicineHeader(
                ownerName = "cô Lan",
                initials = "NL",
                onAccountClick = {}
            )
        }
    }
}