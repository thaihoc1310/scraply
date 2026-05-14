package com.example.scraply.ui.stamp

import android.Manifest
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.scraply.R
import com.example.scraply.ui.common.CircleIconButton
import com.example.scraply.util.CutterGeometry
import com.example.scraply.util.PostageStampShape
import com.example.scraply.util.StampBitmapProcessor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StampCameraScreen(
    vm: StampCaptureViewModel,
    onCaptured: (String) -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (cameraPermission.status.isGranted) {
            CameraContent(
                vm = vm,
                onCaptured = onCaptured,
            )
        } else {
            PermissionRequest(
                onRequestPermission = { cameraPermission.launchPermissionRequest() }
            )
        }
    }
}

@Composable
private fun PermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Scraply needs camera access to capture stamps.",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant permission")
        }
    }
}

@Composable
private fun CameraContent(
    vm: StampCaptureViewModel,
    onCaptured: (String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Camera state
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(8f) }
    var hasFlash by remember { mutableStateOf(false) }
    var hasFlashHardware by remember { mutableStateOf(false) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Preview view dimensions - get actual size after layout
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    // Animation state
    var isCapturing by remember { mutableStateOf(false) }
    var isCutting by remember { mutableStateOf(false) }
    var showBlackHole by remember { mutableStateOf(false) }
    var showFallingCard by remember { mutableStateOf(false) }
    var fallingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var frozenPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    // Animation values
    val fallProgress = remember { Animatable(0f) }
    val overlayScale by animateFloatAsState(
        targetValue = if (isCutting) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1100f),
        label = "cutter_press_scale",
    )
    val overlayTranslationY by animateFloatAsState(
        targetValue = if (isCutting) with(density) { 18.dp.toPx() } else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 950f),
        label = "cutter_press_translation_y",
    )

    // Preview view
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val captureSound = remember(context) {
        MediaPlayer.create(context, R.raw.stamp_sound)
    }

    DisposableEffect(captureSound) {
        onDispose {
            captureSound?.release()
        }
    }

    // Setup camera
    DisposableEffect(lensFacing, previewSize) {
        if (previewSize == IntSize.Zero) {
            onDispose {
                camera = null
            }
        } else {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                try {
                    cameraProvider.unbindAll()
                    val viewPort = previewView.viewPort
                    val cam = if (viewPort != null) {
                        val useCaseGroup = UseCaseGroup.Builder()
                            .addUseCase(preview)
                            .addUseCase(imageCapture)
                            .setViewPort(viewPort)
                            .build()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, useCaseGroup)
                    } else {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageCapture,
                        )
                    }
                    camera = cam
                    val zoomState = cam.cameraInfo.zoomState.value
                    minZoom = zoomState?.minZoomRatio ?: 1f
                    maxZoom = zoomState?.maxZoomRatio ?: 8f
                    zoom = zoomState?.zoomRatio ?: 1f
                    hasFlashHardware = cam.cameraInfo.hasFlashUnit()
                } catch (e: Exception) {
                    // Handle error
                }
            }, executor)

            onDispose {
                camera = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    if (isCapturing) return@detectTransformGestures
                    val newZoom = (zoom * gestureZoom).coerceIn(minZoom, maxZoom)
                    zoom = newZoom
                    camera?.cameraControl?.setZoomRatio(newZoom)
                }
            },
    ) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    previewSize = coordinates.size
                },
        )

        frozenPreviewBitmap?.let { frozen ->
            Image(
                bitmap = frozen.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
            )
        }

        val cutterWidthFraction = CutterGeometry.cutterWidth / CutterGeometry.refScreenWidth
        val stampHoleWidthFraction = cutterWidthFraction * CutterGeometry.innerW
        val stampAspectRatio = CutterGeometry.stampWidth / CutterGeometry.stampHeight

        // Cutter overlay frame - positioned same as before
        AnimatedVisibility(
            visible = !showEditor,
            enter = fadeIn(),
            exit = fadeOut(tween(100)),
            modifier = Modifier.zIndex(3f),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val aspectRatio = CutterGeometry.cutterWidth / CutterGeometry.cutterHeight
                Box(
                    modifier = Modifier
                        .fillMaxWidth(cutterWidthFraction)
                        .aspectRatio(aspectRatio)
                        .graphicsLayer {
                            scaleX = overlayScale
                            scaleY = overlayScale
                            translationY = overlayTranslationY
                        },
                ) {
                    StampFrameOverlay(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showBlackHole,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(8f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(stampHoleWidthFraction)
                    .aspectRatio(stampAspectRatio)
                    .graphicsLayer {
                        val progress = fallProgress.value
                        scaleX = 0.94f + 0.06f * progress
                        scaleY = 0.94f + 0.06f * progress
                        alpha = 1f - 0.35f * progress
                    }
                    .clip(PostageStampShape)
                    .background(Color.Black),
            )
        }

        // Falling card
        AnimatedVisibility(
            visible = showFallingCard,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(100)),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(10f),
        ) {
            fallingBitmap?.let { bmp ->
                val currentStampWidth = (previewSize.width * stampHoleWidthFraction).coerceAtLeast(1f)
                val adjustStampWidth = with(density) {
                    (previewSize.width - 40.dp.toPx()).coerceAtLeast(1f) * 0.58f
                }
                val targetScale = (adjustStampWidth / currentStampWidth).coerceIn(1f, 1.55f)
                val fallTargetY = with(density) { (-64).dp.toPx() }
                val progress = fallProgress.value
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(stampHoleWidthFraction)
                        .aspectRatio(stampAspectRatio)
                        .graphicsLayer {
                            translationY = fallTargetY * progress
                            rotationZ = -2.5f * progress
                            val cardScale = 1f + (targetScale - 1f) * progress
                            scaleX = cardScale
                            scaleY = cardScale
                            shadowElevation = 20f
                        },
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Zoom indicator
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${"%.1f".format(zoom)}x",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            // Camera controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (hasFlashHardware) {
                    CircleIconButton(
                        icon = if (hasFlash) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Toggle flash",
                        onClick = {
                            if (isCapturing) return@CircleIconButton
                            hasFlash = !hasFlash
                            imageCapture.flashMode = if (hasFlash) {
                                ImageCapture.FLASH_MODE_ON
                            } else {
                                ImageCapture.FLASH_MODE_OFF
                            }
                        },
                        background = Color.White,
                        tint = Color.Black,
                    )
                }

                CircleIconButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = "Flip camera",
                    onClick = {
                        if (isCapturing) return@CircleIconButton
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    background = Color.White,
                    tint = Color.Black,
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${"%.1f".format(zoom)}x",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Pinch to zoom",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))

            CaptureButton(
                enabled = !isCapturing,
                onClick = {
                    if (isCapturing) return@CaptureButton

                    isCapturing = true
                    isCutting = true
                    showBlackHole = false
                    showFallingCard = false
                    frozenPreviewBitmap?.recycle()
                    val previewSnapshot = previewView.bitmap
                    frozenPreviewBitmap = previewSnapshot
                    try {
                        captureSound?.seekTo(0)
                        captureSound?.start()
                    } catch (_: IllegalStateException) {
                    }

                    scope.launch {
                        fallProgress.snapTo(0f)
                        val animationJob = launch {
                            val previewStamp = withContext(Dispatchers.Default) {
                                previewSnapshot?.let { snapshot ->
                                    StampBitmapProcessor.cut(
                                        context = context,
                                        source = snapshot,
                                        previewSize = IntSize(snapshot.width, snapshot.height),
                                        cutterScale = 1f,
                                    )
                                }
                            }
                            val soundDurationMs = (captureSound?.duration ?: 520).coerceIn(320, 900)
                            val pressDelayMs = (soundDurationMs * 0.12f).toLong()
                            val fallDurationMs = (soundDurationMs - pressDelayMs).coerceAtLeast(220L).toInt()

                            delay(pressDelayMs)
                            isCutting = false

                            if (previewStamp != null) {
                                fallingBitmap = previewStamp
                                showBlackHole = true
                                showFallingCard = true

                                fallProgress.snapTo(0f)
                                fallProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = fallDurationMs,
                                        easing = FastOutSlowInEasing,
                                    ),
                                )
                            }
                        }

                        // Capture image
                        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                        val output = ImageCapture.OutputFileOptions.Builder(file).build()

                        imageCapture.takePicture(
                            output,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                    scope.launch {
                                        // Load and process bitmap
                                        val bitmap = withContext(Dispatchers.IO) {
                                            val src = StampBitmapProcessor.loadBitmapWithExifRotation(file.absolutePath)
                                            if (src != null && previewSize.width > 0) {
                                                StampBitmapProcessor.cut(
                                                    context = context,
                                                    source = src,
                                                    previewSize = previewSize,
                                                    cutterScale = 1f,
                                                )
                                            } else null
                                        }

                                        if (bitmap != null) {
                                            val savedUri = withContext(Dispatchers.IO) {
                                                val outFile = File(context.filesDir, "stamps/${System.currentTimeMillis()}.png")
                                                outFile.parentFile?.mkdirs()
                                                outFile.outputStream().use { out ->
                                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                                }
                                                Uri.fromFile(outFile).toString()
                                            }

                                            animationJob.join()
                                            onCaptured(savedUri)
                                            frozenPreviewBitmap = null
                                            isCapturing = false
                                        } else {
                                            animationJob.cancel()
                                            isCutting = false
                                            showBlackHole = false
                                            showFallingCard = false
                                            fallingBitmap = null
                                            frozenPreviewBitmap = null
                                            isCapturing = false
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    scope.launch {
                                        animationJob.cancel()
                                        isCutting = false
                                        showBlackHole = false
                                        showFallingCard = false
                                        fallingBitmap = null
                                        frozenPreviewBitmap = null
                                        isCapturing = false
                                    }
                                }
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.85f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "capture_scale",
    )

    Box(
        modifier = Modifier
            .size(78.dp)
            .scale(scale)
            .background(Color.White.copy(alpha = 0.25f), CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .background(Color.White, CircleShape),
    )
}
