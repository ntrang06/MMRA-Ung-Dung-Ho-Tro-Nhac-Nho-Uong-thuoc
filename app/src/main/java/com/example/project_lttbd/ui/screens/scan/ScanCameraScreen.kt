
package com.example.project_lttbd.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.project_lttbd.R
import java.io.File

@Composable
fun ScanCameraScreen(
    onCaptureClick: (Uri) -> Unit,
    onPickImageClick: () -> Unit,
    onManualInputClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraReady by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            Toast.makeText(
                context,
                "Cần cấp quyền camera để chụp ảnh thuốc.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (!hasPermission) return@LaunchedEffect

        cameraReady = false

        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            try {
                val provider = providerFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF

                provider.unbindAll()

                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

                cameraReady = true
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Không thể mở camera: ${e.localizedMessage ?: "Lỗi thiết bị"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhoto() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        if (!cameraReady || isCapturing) return

        if (flashEnabled && camera?.cameraInfo?.hasFlashUnit() != true) {
            Toast.makeText(
                context,
                "Thiết bị không hỗ trợ đèn flash.",
                Toast.LENGTH_SHORT
            ).show()
            flashEnabled = false
        }

        imageCapture.flashMode =
            if (flashEnabled) ImageCapture.FLASH_MODE_ON
            else ImageCapture.FLASH_MODE_OFF

        val photoFile = File(
            context.cacheDir,
            "medicine_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            photoFile
        ).build()

        isCapturing = true

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    isCapturing = false
                    onCaptureClick(Uri.fromFile(photoFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    Toast.makeText(
                        context,
                        "Chụp ảnh thất bại: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F201C))
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onCloseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontSize = 22.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Quét vỏ hộp thuốc",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Bước 1 / 3 • Nhận diện thuốc",
                        color = Color(0xFF9FBDB6),
                        fontSize = 12.sp
                    )
                }

                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (flashEnabled) Color(0xFF126B62)
                            else Color(0x33FFFFFF)
                        )
                        .clickable {
                            if (camera?.cameraInfo?.hasFlashUnit() == false) {
                                Toast.makeText(
                                    context,
                                    "Camera không có đèn flash.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                flashEnabled = !flashEnabled
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚡",
                        color = if (flashEnabled) Color(0xFFFACC15)
                        else Color.White,
                        fontSize = 22.sp
                    )
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )

                    // Khung hướng dẫn căn chỉnh nằm trên camera thật
                    Box(
                        Modifier
                            .fillMaxWidth(0.88f)
                            .height(220.dp)
                            .border(
                                2.dp,
                                Color.White.copy(alpha = 0.85f),
                                RoundedCornerShape(20.dp)
                            )
                    )

                    if (!cameraReady) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Cần quyền camera để quét thuốc",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    Manifest.permission.CAMERA
                                )
                            }
                        ) {
                            Text("Cấp quyền camera")
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp
                ),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Đặt mặt trước vỏ hộp vào khung",
                        color = Color(0xFF31534A),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(
                                4.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .clickable(
                                enabled = cameraReady && !isCapturing
                            ) {
                                capturePhoto()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = androidx.compose.ui.res
                                        .painterResource(R.drawable.ic_camera),
                                    contentDescription = "Chụp ảnh",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    TextButton(onClick = onManualInputClick) {
                        Text("Nhập thông tin thủ công")
                    }

                    TextButton(onClick = onPickImageClick) {
                        Text("Hoặc chọn ảnh từ thư viện")
                    }
                }
            }
        }
    }
}
