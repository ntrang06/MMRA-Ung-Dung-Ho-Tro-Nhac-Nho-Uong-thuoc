package com.example.mmra_medicationmanager.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mmra_medicationmanager.R
import com.example.mmra_medicationmanager.model.UserProfileManager

@Composable
fun CropAvatarScreen(
    modifier: Modifier = Modifier,
    inputBitmap: Bitmap? = UserProfileManager.tempCapturedBitmap,
    onBackClick: () -> Unit = {},
    onConfirmCrop: (Bitmap) -> Unit = {},
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF0F5B51)
    val lightBgColor = Color(0xFFE0F0EC)
    val darkTextColor = Color(0xFF0A3C35)
    val subTextColor = Color(0xFF5A6E6A)
    val borderColor = Color(0xFFE2E8E6)

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

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
                        text = stringResource(id = R.string.avatar_crop_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkTextColor,
                    )
                    Text(
                        text = stringResource(id = R.string.avatar_crop_step),
                        fontSize = 13.sp,
                        color = subTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.avatar_crop_instruction),
                fontSize = 14.sp,
                color = subTextColor,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Crop Box Area
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF38524B),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Image with transform gestures
                    if (inputBitmap != null) {
                        Image(
                            bitmap = inputBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    rotationZ = rotationAngle,
                                )
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        zoomScale = (zoomScale * zoom).coerceIn(0.8f, 3.0f)
                                    }
                                },
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFB5D4CC),
                            modifier = Modifier.size(200.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_user),
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(100.dp),
                                )
                            }
                        }
                    }

                    // Circle Crop Guide Overlay
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, Color.White),
                        modifier = Modifier.size(240.dp),
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Zoom & Rotate Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "—",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkTextColor,
                    modifier = Modifier
                        .clickable { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.8f) }
                        .padding(horizontal = 8.dp),
                )

                Slider(
                    value = zoomScale,
                    onValueChange = { zoomScale = it },
                    valueRange = 0.8f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = Color(0xFFE2E8E6),
                    ),
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = "+",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkTextColor,
                    modifier = Modifier
                        .clickable { zoomScale = (zoomScale + 0.2f).coerceAtMost(3.0f) }
                        .padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Rotate Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF7F4),
                    modifier = Modifier
                        .clickable { rotationAngle = (rotationAngle + 90f) % 360f },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_rotate),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.avatar_crop_rotate),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Avatar Preview Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    if (inputBitmap != null) {
                        Image(
                            bitmap = inputBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = lightBgColor,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text(
                                    text = "NL",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = stringResource(id = R.string.avatar_crop_preview_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkTextColor,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = UserProfileManager.name,
                            fontSize = 13.sp,
                            color = subTextColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Button: Sử dụng ảnh
            Button(
                onClick = {
                    val finalBitmap = inputBitmap ?: createCircleSampleBitmap()
                    val croppedCircularBitmap = cropToCircle(finalBitmap, zoomScale, rotationAngle)
                    UserProfileManager.saveAvatarBitmap(context, croppedCircularBitmap)
                    onConfirmCrop(croppedCircularBitmap)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.avatar_crop_use_photo),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.avatar_illustrative_note),
                fontSize = 12.sp,
                color = subTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun createCircleSampleBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#0F5B51")
        isAntiAlias = true
    }
    canvas.drawCircle(150f, 150f, 150f, paint)
    return bitmap
}

private fun cropToCircle(source: Bitmap, scale: Float, rotation: Float): Bitmap {
    val size = Math.min(source.width, source.height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint().apply {
        isAntiAlias = true
    }
    val path = Path()
    path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
    canvas.clipPath(path)

    val matrix = Matrix()
    matrix.postRotate(rotation, source.width / 2f, source.height / 2f)
    matrix.postScale(scale, scale, source.width / 2f, source.height / 2f)
    canvas.drawBitmap(source, matrix, paint)

    return output
}

@Preview(showBackground = true)
@Composable
fun CropAvatarScreenPreview() {
    CropAvatarScreen()
}