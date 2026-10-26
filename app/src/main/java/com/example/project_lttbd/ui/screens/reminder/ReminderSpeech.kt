package com.example.project_lttbd.ui.screens.reminder

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun rememberReminderSpeech(text: String): () -> Unit {
    val context = LocalContext.current

    var engine by remember {
        mutableStateOf<TextToSpeech?>(null)
    }
    var ready by remember {
        mutableStateOf(false)
    }

    DisposableEffect(context) {
        var disposed = false

        val tts = TextToSpeech(context) { status ->
            if (!disposed) {
                ready = status == TextToSpeech.SUCCESS
            }
        }

        engine = tts

        onDispose {
            disposed = true
            ready = false
            engine = null
            tts.stop()
            tts.shutdown()
        }
    }

    return {
        val tts = engine

        if (!ready || tts == null) {
            Toast.makeText(
                context,
                "Giọng đọc chưa sẵn sàng. Vui lòng thử lại.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val language = tts.setLanguage(
                Locale.forLanguageTag("vi-VN")
            )

            if (
                language == TextToSpeech.LANG_MISSING_DATA ||
                language == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Toast.makeText(
                    context,
                    "Thiết bị chưa có giọng đọc tiếng Việt.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "mmra-dose-instructions"
                )
            }
        }
    }
}