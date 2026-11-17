package com.example.project_lttbd.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.project_lttbd.R
import com.example.project_lttbd.data.image.MedicinePhotoCodec
import kotlinx.coroutines.CancellationException

@Composable
fun MedicinePhoto(
    source: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Ảnh thuốc",
    contentScale: ContentScale = ContentScale.Crop,
    maxSide: Int = 256
) {
    val context = LocalContext.current.applicationContext

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        source,
        maxSide
    ) {
        value = null

        if (!source.isNullOrBlank()) {
            value = try {
                MedicinePhotoCodec.load(
                    context = context,
                    source = source,
                    maxSide = maxSide
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier.background(Color(0xFFEEF5F3)),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap

        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.thuoc),
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF126B62)
            )
        }
    }
}

@Composable
fun MedicinePhotoViewer(
    source: String,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Xem ảnh thuốc",
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = onClose) {
                        Text("Xong")
                    }
                }

                MedicinePhoto(
                    source = source,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentScale = ContentScale.Fit,
                    maxSide = 1024
                )
            }
        }
    }
}