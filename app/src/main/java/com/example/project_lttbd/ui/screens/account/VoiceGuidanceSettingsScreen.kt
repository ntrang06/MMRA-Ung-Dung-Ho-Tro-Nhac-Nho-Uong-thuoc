package com.example.project_lttbd.ui.screens.account

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
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
    onSaveSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val isPreview = LocalInspectionMode.current

    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)
    val infoBgColor = Color(0xFFEFF6F4)
    val disabledColor = Color(0xFFA2C0BB)

    var isVoiceEnabled by rememberSaveable {
        mutableStateOf(UserProfileManager.isVoiceGuidanceEnabled)
    }
    var volume by rememberSaveable {
        mutableStateOf(UserProfileManager.voiceVolume.coerceIn(0f, 1f))
    }

    var speechEngine by remember {
        mutableStateOf<TextToSpeech?>(null)
    }
    var isTtsReady by remember {
        mutableStateOf(false)
    }
    var ttsError by remember {
        mutableStateOf<String?>(null)
    }

    val sampleText =
        "Chào cô Lan. Chạm vào Lịch uống để xem các liều thuốc trong ngày."

    DisposableEffect(applicationContext, isPreview) {
        val handler = Handler(Looper.getMainLooper())
        var active = true
        var engine: TextToSpeech? = null

        fun reportError(message: String) {
            handler.post {
                if (active) {
                    ttsError = message
                }
            }
        }

        if (!isPreview) {
            try {
                engine = TextToSpeech(applicationContext) { status ->
                    // Chờ constructor hoàn tất và cập nhật giao diện
                    // trên luồng chính.
                    handler.post {
                        if (active) {
                            val currentEngine = engine

                            if (
                                status != TextToSpeech.SUCCESS ||
                                currentEngine == null
                            ) {
                                isTtsReady = false
                                ttsError =
                                    "Không khởi tạo được giọng nói. " +
                                            "Hãy kiểm tra dịch vụ chuyển văn bản " +
                                            "thành giọng nói trên điện thoại."
                            } else {
                                try {
                                    val languageStatus =
                                        currentEngine.setLanguage(
                                            Locale.forLanguageTag("vi-VN")
                                        )

                                    when {
                                        languageStatus ==
                                                TextToSpeech.LANG_MISSING_DATA -> {
                                            isTtsReady = false
                                            ttsError =
                                                "Máy chưa có dữ liệu giọng " +
                                                        "tiếng Việt. Hãy tải giọng " +
                                                        "tiếng Việt trong cài đặt " +
                                                        "chuyển văn bản thành giọng nói, " +
                                                        "rồi mở lại màn này."
                                        }

                                        languageStatus <
                                                TextToSpeech.LANG_AVAILABLE -> {
                                            isTtsReady = false
                                            ttsError =
                                                "Dịch vụ giọng nói hiện tại " +
                                                        "chưa hỗ trợ tiếng Việt. " +
                                                        "Hãy chọn dịch vụ có hỗ trợ " +
                                                        "tiếng Việt rồi mở lại màn này."
                                        }

                                        else -> {
                                            currentEngine.setAudioAttributes(
                                                AudioAttributes.Builder()
                                                    .setUsage(
                                                        AudioAttributes.USAGE_MEDIA
                                                    )
                                                    .setContentType(
                                                        AudioAttributes
                                                            .CONTENT_TYPE_SPEECH
                                                    )
                                                    .build()
                                            )

                                            isTtsReady = true
                                            ttsError = null
                                        }
                                    }
                                } catch (_: Exception) {
                                    isTtsReady = false
                                    ttsError =
                                        "Không chuẩn bị được giọng tiếng Việt. " +
                                                "Hãy kiểm tra cài đặt giọng nói " +
                                                "trên điện thoại."
                                }
                            }
                        }
                    }
                }

                speechEngine = engine

                engine?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) = Unit

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onError(utteranceId: String?) {
                            reportError(
                                "Không phát được hướng dẫn. " +
                                        "Hãy kiểm tra dữ liệu giọng tiếng Việt " +
                                        "trên điện thoại."
                            )
                        }

                        override fun onError(
                            utteranceId: String?,
                            errorCode: Int
                        ) {
                            reportError(
                                "Không phát được hướng dẫn. " +
                                        "Hãy kiểm tra dữ liệu giọng tiếng Việt " +
                                        "và kết nối mạng nếu giọng đọc cần mạng."
                            )
                        }
                    }
                )
            } catch (_: Exception) {
                isTtsReady = false
                ttsError =
                    "Không mở được dịch vụ giọng nói trên điện thoại."
            }
        }

        onDispose {
            active = false
            handler.removeCallbacksAndMessages(null)
            engine?.stop()
            engine?.shutdown()
            speechEngine = null
            isTtsReady = false
        }
    }

    val canPlaySample = isVoiceEnabled && isTtsReady
    val sampleColor =
        if (canPlaySample) primaryColor else disabledColor

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFFAFDFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = lightBgColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(
                                R.string.cd_back_button
                            ),
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Hướng dẫn giọng nói",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor
                    )
                    Text(
                        text = "Nghe rõ, thao tác dễ hơn",
                        fontSize = 13.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF7F4),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = painterResource(
                                    R.drawable.ic_speaker
                                ),
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                R.string.setup_display_voice_title
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Đọc to lời nhắc và tên nút khi bạn cần.",
                            fontSize = 13.sp,
                            color = subTextColor,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                if (isVoiceEnabled) {
                                    R.string.setup_display_status_on
                                } else {
                                    R.string.setup_display_status_off
                                }
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Switch(
                        checked = isVoiceEnabled,
                        onCheckedChange = {
                            isVoiceEnabled = it
                            if (!it) {
                                speechEngine?.stop()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD0DDD8),
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Nghe thử hướng dẫn",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "“$sampleText”",
                        fontSize = 14.sp,
                        color = subTextColor,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val currentEngine = speechEngine
                            val audioManager = context.getSystemService(
                                Context.AUDIO_SERVICE
                            ) as? AudioManager

                            when {
                                volume <= 0f -> {
                                    Toast.makeText(
                                        context,
                                        "Hãy tăng âm lượng giọng nói.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                audioManager?.getStreamVolume(
                                    AudioManager.STREAM_MUSIC
                                ) == 0 -> {
                                    Toast.makeText(
                                        context,
                                        "Âm lượng đa phương tiện đang bằng 0. " +
                                                "Hãy tăng âm lượng điện thoại.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                currentEngine != null && isTtsReady -> {
                                    ttsError = null

                                    val params = Bundle().apply {
                                        putFloat(
                                            TextToSpeech.Engine.KEY_PARAM_VOLUME,
                                            volume.coerceIn(0f, 1f)
                                        )
                                    }

                                    val result = currentEngine.speak(
                                        sampleText,
                                        TextToSpeech.QUEUE_FLUSH,
                                        params,
                                        "voice_guidance_sample"
                                    )

                                    if (result == TextToSpeech.ERROR) {
                                        ttsError =
                                            "Không gửi được yêu cầu phát " +
                                                    "giọng nói. Hãy mở lại màn này."
                                    }
                                }
                            }
                        },
                        enabled = canPlaySample,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, sampleColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_speaker
                            ),
                            contentDescription = null,
                            tint = sampleColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Phát hướng dẫn mẫu",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = sampleColor
                        )
                    }

                    if (!isTtsReady && ttsError == null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Đang chuẩn bị giọng nói…",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }

                    ttsError?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Âm lượng giọng nói",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor
                        )
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_speaker),
                            contentDescription = null,
                            tint = subTextColor,
                            modifier = Modifier.size(18.dp)
                        )

                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 0f..1f,
                            enabled = isVoiceEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryColor,
                                activeTrackColor = primaryColor,
                                inactiveTrackColor = Color(0xFFE2E8E6)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Icon(
                            painter = painterResource(R.drawable.ic_speaker),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Chọn mức âm lượng nghe thoải mái với bạn.",
                        fontSize = 12.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    UserProfileManager.isVoiceGuidanceEnabled =
                        isVoiceEnabled
                    UserProfileManager.voiceVolume = volume
                    UserProfileManager.saveProfileInfo(context)
                    onSaveSuccess()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Lưu cài đặt",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = infoBgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_speaker),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Bạn cũng có thể dùng nút âm lượng " +
                                "trên điện thoại để điều chỉnh khi nghe.",
                        fontSize = 13.sp,
                        color = subTextColor,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceGuidanceSettingsScreenPreview() {
    VoiceGuidanceSettingsScreen()
}