package com.example.project_lttbd.ui.screens.account

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_lttbd.R
import com.example.project_lttbd.data.model.UserProfileManager
import java.util.Locale

@Composable
fun VoiceGuidanceSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val infoBgColor = Color(0xFFEFF6F4)
    val iconBoxBgColor = Color(0xFFEFF7F4)

    var isVoiceEnabled by remember { mutableStateOf(UserProfileManager.isVoiceGuidanceEnabled) }
    var volume by remember { mutableFloatStateOf(UserProfileManager.voiceVolume) }

    val sampleText = "Chào cô Lan. Chạm vào Lịch uống để xem các liều thuốc trong ngày."

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
            // Header
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
                        text = "Hướng dẫn giọng nói",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = "Nghe rõ, thao tác dễ hơn",
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: Toggle
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
                            text = "Đọc to lời nhắc và tên nút khi bạn cần.",
                            fontSize = 13.sp,
                            color = subTextColor,
                            lineHeight = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVoiceEnabled) stringResource(id = R.string.setup_display_status_on) else stringResource(id = R.string.setup_display_status_off),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isVoiceEnabled,
                        onCheckedChange = { isVoiceEnabled = it },
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

            // Card 2: Nghe thử hướng dẫn
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    Text(
                        text = "Nghe thử hướng dẫn",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "“$sampleText”",
                        fontSize = 14.sp,
                        color = subTextColor,
                        lineHeight = 20.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            speakSampleViaTts(context, sampleText)
                        },
                        enabled = isVoiceEnabled,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, if (isVoiceEnabled) primaryColor else Color(0xFFA2C0BB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_speaker),
                                contentDescription = null,
                                tint = if (isVoiceEnabled) primaryColor else Color(0xFFA2C0BB),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Phát hướng dẫn mẫu",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVoiceEnabled) primaryColor else Color(0xFFA2C0BB),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 3: Âm lượng giọng nói
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Âm lượng giọng nói",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_speaker),
                            contentDescription = null,
                            tint = subTextColor,
                            modifier = Modifier.size(18.dp),
                        )

                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 0f..1f,
                            enabled = isVoiceEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryColor,
                                activeTrackColor = primaryColor,
                                inactiveTrackColor = Color(0xFFE2E8E6),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_speaker),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Chọn mức âm lượng nghe thoải mái với bạn.",
                        fontSize = 12.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Button: Lưu cài đặt
            Button(
                onClick = {
                    UserProfileManager.isVoiceGuidanceEnabled = isVoiceEnabled
                    UserProfileManager.voiceVolume = volume
                    UserProfileManager.saveProfileInfo(context)
                    onSaveSuccess()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = "Lưu cài đặt",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info Box
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
                        painter = painterResource(id = R.drawable.ic_speaker),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Bạn cũng có thể dùng nút âm lượng trên điện thoại để điều chỉnh khi nghe.",
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

private fun speakSampleViaTts(context: Context, text: String) {
    try {
        val ttsClass = Class.forName("android.speech.tts.TextToSpeech")
        var ttsObj: Any? = null
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                try {
                    val setLangMethod = ttsClass.getMethod("setLanguage", Locale::class.java)
                    setLangMethod.invoke(ttsObj, Locale("vi", "VN"))
                    val speakMethod = ttsClass.getMethod(
                        "speak",
                        String::class.java,
                        Int::class.javaPrimitiveType,
                        Bundle::class.java,
                        String::class.java,
                    )
                    speakMethod.invoke(
                        ttsObj,
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "sample_utterance",
                    )
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        val constructor = ttsClass.getConstructor(Context::class.java, TextToSpeech.OnInitListener::class.java)
        ttsObj = constructor.newInstance(context, listener)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceGuidanceSettingsScreenPreview() {
    VoiceGuidanceSettingsScreen()
}