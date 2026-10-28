package com.example.project_lttbd.ui.screens.account

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.project_lttbd.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

@Composable
fun CaptureAvatarScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPhotoCaptured: (Bitmap) -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPreview = LocalInspectionMode.current
    val mainExecutor = remember(context) {
        ContextCompat.getMainExecutor(context)
    }

    val primaryColor = Color(0xFF0F5B51)
    val subTextColor = Color(0xFF5A6E6A)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionRequested by rememberSaveable {
        mutableStateOf(false)
    }

    var lensFacing by rememberSaveable {
        mutableIntStateOf(CameraSelector.LENS_FACING_FRONT)
    }

    var cameraReady by remember { mutableStateOf(false) }
    var canSwitchCamera by remember { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val screenActive = remember { AtomicBoolean(true) }
    val latestOnPhotoCaptured by rememberUpdatedState(onPhotoCaptured)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val controller = remember(context, isPreview) {
        if (isPreview) {
            null
        } else {
            LifecycleCameraController(context.applicationContext).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
                imageCaptureResolutionSelector =
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 960),
                                ResolutionStrategy
                                    .FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                        .build()
            }
        }
    }

    val previewView = remember(context, isPreview) {
        if (isPreview) {
            null
        } else {
            PreviewView(context).apply {
                implementationMode =
                    PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }
    }

    DisposableEffect(Unit) {
        screenActive.set(true)
        onDispose {
            screenActive.set(false)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!isPreview && !hasPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(
        controller,
        previewView,
        lifecycleOwner,
        hasPermission,
        lensFacing
    ) {
        var disposed = false

        cameraReady = false
        hasFlash = false
        flashOn = false
        cameraError = null

        val streamObserver = Observer<PreviewView.StreamState> { state ->
            if (!disposed) {
                cameraReady = state == PreviewView.StreamState.STREAMING
                if (cameraReady) {
                    hasFlash = controller?.cameraInfo
                        ?.hasFlashUnit() == true
                }
            }
        }

        if (
            controller != null &&
            previewView != null &&
            hasPermission
        ) {
            val initialization = controller.initializationFuture

            initialization.addListener(
                {
                    if (!disposed) {
                        try {
                            initialization.get()

                            val selector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            val otherFacing =
                                if (
                                    lensFacing ==
                                    CameraSelector.LENS_FACING_FRONT
                                ) {
                                    CameraSelector.LENS_FACING_BACK
                                } else {
                                    CameraSelector.LENS_FACING_FRONT
                                }

                            val otherSelector = CameraSelector.Builder()
                                .requireLensFacing(otherFacing)
                                .build()

                            if (!controller.hasCamera(selector)) {
                                if (controller.hasCamera(otherSelector)) {
                                    lensFacing = otherFacing
                                } else {
                                    cameraError =
                                        "Không tìm thấy camera trên thiết bị."
                                }
                            } else {
                                canSwitchCamera =
                                    controller.hasCamera(otherSelector)

                                controller.cameraSelector = selector
                                controller.imageCaptureFlashMode =
                                    ImageCapture.FLASH_MODE_OFF

                                previewView.controller = controller

                                previewView.previewStreamState.observe(
                                    lifecycleOwner,
                                    streamObserver
                                )

                                controller.bindToLifecycle(lifecycleOwner)
                            }
                        } catch (_: Exception) {
                            cameraError =
                                "Không mở được camera. Hãy đóng ứng dụng " +
                                        "khác đang dùng camera rồi thử lại."
                        }
                    }
                },
                mainExecutor
            )
        }

        onDispose {
            disposed = true
            previewView?.previewStreamState
                ?.removeObserver(streamObserver)
            previewView?.controller = null
            controller?.unbind()
        }
    }

    LaunchedEffect(controller, flashOn) {
        controller?.imageCaptureFlashMode =
            if (flashOn) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF07211C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Quay lại",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Chụp ảnh đại diện",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Bước 1 / 3 • Chụp ảnh",
                        fontSize = 13.sp,
                        color = Color(0xFFC0D5D1)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF38524B),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (hasPermission && previewView != null) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (cameraReady) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color.White,
                                    radius = size.minDimension * 0.38f,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    when {
                        !hasPermission && !isPreview -> {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "Cho phép truy cập camera " +
                                            "để chụp ảnh đại diện.",
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        permissionRequested = true
                                        permissionLauncher.launch(
                                            Manifest.permission.CAMERA
                                        )
                                    }
                                ) {
                                    Text("Cho phép camera")
                                }

                                TextButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Settings
                                                    .ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts(
                                                    "package",
                                                    context.packageName,
                                                    null
                                                )
                                            ).addFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                            )
                                        )
                                    }
                                ) {
                                    Text(
                                        text = "Mở cài đặt ứng dụng",
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        cameraError != null -> {
                            Text(
                                text = cameraError.orEmpty(),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }

                        !cameraReady -> {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Đang mở camera…",
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (cameraReady) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xCC2C4541),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Đặt khuôn mặt vào giữa khung",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xCC2C4541),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = if (
                                    lensFacing ==
                                    CameraSelector.LENS_FACING_FRONT
                                ) {
                                    "Camera trước"
                                } else {
                                    "Camera sau"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 6.dp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFFAFDFB),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Nhìn thẳng • Chọn nơi đủ sáng",
                        color = subTextColor,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CaptureAvatarControl(
                            icon = if (flashOn) {
                                R.drawable.ic_flash_on
                            } else {
                                R.drawable.ic_flash_off
                            },
                            label = when {
                                !hasFlash -> "Không có flash"
                                flashOn -> "Flash bật"
                                else -> "Flash tắt"
                            },
                            enabled = cameraReady && hasFlash && !isCapturing,
                            onClick = {
                                flashOn = !flashOn
                            }
                        )

                        Surface(
                            onClick = {
                                val currentController = controller

                                if (
                                    currentController != null &&
                                    cameraReady &&
                                    !isCapturing
                                ) {
                                    isCapturing = true
                                    cameraError = null

                                    val mirror =
                                        lensFacing ==
                                                CameraSelector
                                                    .LENS_FACING_FRONT

                                    try {
                                        currentController.takePicture(
                                            mainExecutor,
                                            object :
                                                ImageCapture
                                                .OnImageCapturedCallback() {

                                                override fun onCaptureSuccess(
                                                    image: ImageProxy
                                                ) {
                                                    if (
                                                        !screenActive.get()
                                                    ) {
                                                        image.close()
                                                        return
                                                    }

                                                    try {
                                                        val bitmap =
                                                            capturedAvatarBitmap(
                                                                image,
                                                                mirror
                                                            )

                                                        isCapturing = false
                                                        latestOnPhotoCaptured(
                                                            bitmap
                                                        )
                                                    } catch (_: Exception) {
                                                        isCapturing = false
                                                        cameraError =
                                                            "Không đọc được " +
                                                                    "ảnh vừa chụp. " +
                                                                    "Hãy chụp lại."
                                                    } finally {
                                                        image.close()
                                                    }
                                                }

                                                override fun onError(
                                                    exception:
                                                    ImageCaptureException
                                                ) {
                                                    if (
                                                        screenActive.get()
                                                    ) {
                                                        isCapturing = false
                                                        cameraError =
                                                            "Chụp ảnh chưa " +
                                                                    "thành công. " +
                                                                    "Hãy thử lại."
                                                    }
                                                }
                                            }
                                        )
                                    } catch (_: Exception) {
                                        isCapturing = false
                                        cameraError =
                                            "Camera chưa sẵn sàng. " +
                                                    "Hãy chờ rồi chụp lại."
                                    }
                                }
                            },
                            enabled = cameraReady && !isCapturing,
                            shape = CircleShape,
                            color = primaryColor,
                            border = BorderStroke(
                                4.dp,
                                Color.White
                            ),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (isCapturing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(
                                            R.drawable.ic_camera
                                        ),
                                        contentDescription = "Chụp ảnh",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        CaptureAvatarControl(
                            icon = R.drawable.ic_switch_camera,
                            label = "Đổi camera",
                            enabled = cameraReady &&
                                    canSwitchCamera && !isCapturing,
                            onClick = {
                                lensFacing = if (
                                    lensFacing ==
                                    CameraSelector.LENS_FACING_FRONT
                                ) {
                                    CameraSelector.LENS_FACING_BACK
                                } else {
                                    CameraSelector.LENS_FACING_FRONT
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (isCapturing) {
                            "Đang chụp…"
                        } else {
                            "Chụp ảnh"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A3C35)
                    )

                    TextButton(onClick = onCancelClick) {
                        Text(
                            text = "Hủy",
                            color = subTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CaptureAvatarControl(
    icon: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(92.dp)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = Color(0xFFEFF7F4),
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = label,
                    tint = if (enabled) {
                        Color(0xFF0F5B51)
                    } else {
                        Color(0xFF9CAFAA)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF5A6E6A),
            textAlign = TextAlign.Center
        )
    }
}

private fun capturedAvatarBitmap(
    image: ImageProxy,
    mirror: Boolean
): Bitmap {
    val raw = image.toBitmap()
    val crop = image.cropRect

    val cropped = Bitmap.createBitmap(
        raw,
        crop.left,
        crop.top,
        crop.width(),
        crop.height()
    )

    val resize = min(
        1f,
        1600f / max(cropped.width, cropped.height).toFloat()
    )

    val matrix = Matrix().apply {
        postScale(resize, resize)
        postRotate(image.imageInfo.rotationDegrees.toFloat())

        if (mirror) {
            postScale(-1f, 1f)
        }
    }

    return Bitmap.createBitmap(
        cropped,
        0,
        0,
        cropped.width,
        cropped.height,
        matrix,
        true
    )
}

@Preview(showBackground = true)
@Composable
fun CaptureAvatarScreenPreview() {
    CaptureAvatarScreen()
}