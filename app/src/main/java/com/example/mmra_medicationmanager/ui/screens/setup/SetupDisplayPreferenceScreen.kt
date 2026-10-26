package com.example.mmra_medicationmanager.ui.screens.setup

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.mmra_medicationmanager.R

@Composable
fun SetupDisplayPreferenceScreen(
    modifier: Modifier = Modifier,
    initialLargeText: Boolean = true,
    initialVoiceGuidance: Boolean = true,
    onBackClick: () -> Unit = {},
    onCompleteClick: (Boolean, Boolean) -> Unit = { _, _ -> },
) {
    var isLargeText by remember { mutableStateOf(initialLargeText) }
    var isVoiceGuidance by remember { mutableStateOf(initialVoiceGuidance) }

    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val infoBgColor = Color(0xFFEFF6F4)
    val iconBoxBgColor = Color(0xFFEFF7F4)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = lightBgColor,
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
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.setup_display_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = stringResource(id = R.string.setup_display_subtitle),
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.setup_display_headline),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = darkTextColor,
                lineHeight = 32.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.setup_display_desc),
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Setting 1: Chữ lớn, dễ đọc
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = iconBoxBgColor,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_text_size),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(id = R.string.setup_display_large_text_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.setup_display_large_text_desc),
                            fontSize = 13.sp,
                            color = subTextColor,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isLargeText) stringResource(id = R.string.setup_display_status_on) else stringResource(id = R.string.setup_display_status_off),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isLargeText,
                        onCheckedChange = { isLargeText = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD0DDD8),
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Setting 2: Hướng dẫn bằng giọng nói
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = iconBoxBgColor,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_speaker),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(id = R.string.setup_display_voice_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.setup_display_voice_desc),
                            fontSize = 13.sp,
                            color = subTextColor,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVoiceGuidance) stringResource(id = R.string.setup_display_status_on) else stringResource(id = R.string.setup_display_status_off),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isVoiceGuidance,
                        onCheckedChange = { isVoiceGuidance = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD0DDD8),
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview Card (Xem thử cỡ chữ)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.setup_display_preview_label),
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(id = R.string.setup_display_preview_text),
                        fontSize = if (isLargeText) 20.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                        lineHeight = if (isLargeText) 28.sp else 22.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            val isAnyOptionSelected = isLargeText || isVoiceGuidance
            Button(
                onClick = { onCompleteClick(isLargeText, isVoiceGuidance) },
                enabled = isAnyOptionSelected,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFFA2C0BB),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.setup_btn_save_continue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = infoBgColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_save),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.setup_display_info_note),
                        fontSize = 13.sp,
                        color = subTextColor,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SetupDisplayPreferenceScreenPreview() {
    SetupDisplayPreferenceScreen()
}