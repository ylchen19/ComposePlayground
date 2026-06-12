package com.example.composeplayground.ui.screen.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.composeplayground.ui.screen.camera.components.CameraDashboardOverlay
import com.example.composeplayground.ui.screen.camera.components.CameraPreviewCanvas
import com.example.composeplayground.ui.screen.camera.model.CameraFilter
import com.example.composeplayground.ui.screen.camera.model.FlashMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // pointerInput keyed on bounds only; rememberUpdatedState keeps the
    // in-flight pinch coroutine reading the latest zoomRatio without restarting.
    val latestZoomRatio by rememberUpdatedState(uiState.zoomRatio)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(uiState.hasCameraPermission, uiState.minZoomRatio, uiState.maxZoomRatio) {
                if (!uiState.hasCameraPermission) return@pointerInput
                detectTransformGestures { _, _, zoom, _ ->
                    val newRatio = (latestZoomRatio * zoom)
                        .coerceIn(uiState.minZoomRatio, uiState.maxZoomRatio)
                    viewModel.onZoomChanged(newRatio)
                }
            },
    ) {
        if (uiState.hasCameraPermission) {
            CameraPreviewCanvas(
                frame = currentBitmap,
                filter = uiState.selectedFilter,
                modifier = Modifier.fillMaxSize(),
            )
            CameraControlEffect(
                uiState = uiState,
                viewModel = viewModel,
                onFrame = { currentBitmap = it },
            )
            CameraDashboardOverlay(
                uiState = uiState,
                onBack = onBack,
                onModeChange = viewModel::setMode,
                onToggleGrid = viewModel::toggleGrid,
                onToggleDashboard = viewModel::toggleDashboard,
                onControlGroupChange = viewModel::setActiveControlGroup,
                onFilterSelected = viewModel::selectFilter,
                onFlashToggle = { viewModel.setFlashMode(uiState.flashMode.next()) },
                onShutter = viewModel::requestCapture,
                onFlip = viewModel::toggleCamera,
                onZoomClick = { viewModel.onZoomChanged(uiState.nextZoomTarget()) },
                onEvChanged = viewModel::setEvIndex,
                onWhiteBalanceSelected = viewModel::setWhiteBalance,
                onManualFocusToggle = viewModel::setManualFocus,
                onFocusDistanceChanged = viewModel::onFocusDistanceChanged,
                onManualExposureToggle = viewModel::setManualExposure,
                onIsoChanged = viewModel::setIso,
                onShutterSpeedChanged = viewModel::setShutterSpeed,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PermissionRationaleContent(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.fillMaxSize(),
            )
            PermissionBackButton(
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun PermissionBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onBack,
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 12.dp, top = 8.dp)
            .size(48.dp)
            .background(Color.Black.copy(alpha = 0.35f), CircleShape),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = Color.White,
        )
    }
}

private fun FlashMode.next(): FlashMode = when (this) {
    FlashMode.Off -> FlashMode.Auto
    FlashMode.Auto -> FlashMode.On
    FlashMode.On -> FlashMode.Off
}

private fun CameraUiState.nextZoomTarget(): Float {
    val targets = listOf(1f, 2f, 5f)
        .map { it.coerceIn(minZoomRatio, maxZoomRatio) }
        .distinct()
    return targets.firstOrNull { it > zoomRatio } ?: targets.first()
}

// ── Camera lifecycle + control effect ────────────────────────────────────────

@AndroidXOptIn(ExperimentalCamera2Interop::class)
@Composable
private fun CameraControlEffect(
    uiState: CameraUiState,
    viewModel: CameraViewModel,
    onFrame: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame by rememberUpdatedState(onFrame)

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    // AtomicReference so the capture callback can read the executor without triggering recomposition.
    val captureExecutorRef = remember { AtomicReference<ExecutorService?>(null) }

    DisposableEffect(lifecycleOwner, uiState.isFrontCamera) {
        val executor = Executors.newSingleThreadExecutor()
        captureExecutorRef.set(executor)

        val cameraSelector = if (uiState.isFrontCamera)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA

        val imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            processFrame(imageProxy, currentOnFrame)
        }

        val capture = ImageCapture.Builder().build()

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, imageAnalysis, capture
                )
                imageCapture = capture

                camera?.let { cam ->
                    cam.cameraInfo.zoomState.observe(lifecycleOwner) { z ->
                        viewModel.onZoomBoundsAvailable(z.minZoomRatio, z.maxZoomRatio)
                    }
                    cam.cameraInfo.exposureState.let { es ->
                        viewModel.onEvRangeAvailable(
                            es.exposureCompensationRange.lower,
                            es.exposureCompensationRange.upper,
                        )
                    }
                    val info = Camera2CameraInfo.from(cam.cameraInfo)
                    info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                        ?.let { viewModel.onIsoRangeAvailable(it.lower, it.upper) }
                    info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                        ?.let { viewModel.onShutterRangeAvailable(it.lower, it.upper) }
                    info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                        ?.let { viewModel.onMaxFocusDistanceAvailable(it) }
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try { future.get().unbindAll() } catch (_: Exception) {}
            captureExecutorRef.set(null)
            executor.shutdown()
            camera = null
            imageCapture = null
        }
    }

    LaunchedEffect(uiState.flashMode, imageCapture) {
        imageCapture?.flashMode = uiState.flashMode.imageCaptureMode
    }

    LaunchedEffect(uiState.zoomRatio, camera) {
        camera?.cameraControl?.setZoomRatio(uiState.zoomRatio)
    }

    LaunchedEffect(uiState.evIndex, camera) {
        camera?.cameraControl?.setExposureCompensationIndex(uiState.evIndex)
    }

    LaunchedEffect(
        uiState.whiteBalance,
        uiState.isManualFocus, uiState.focusDistance,
        uiState.isManualExposure, uiState.isoValue, uiState.shutterSpeedNs,
        camera,
    ) {
        val cam = camera ?: return@LaunchedEffect
        val c2 = Camera2CameraControl.from(cam.cameraControl)
        c2.addCaptureRequestOptions(
            CaptureRequestOptions.Builder().apply {
                setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    uiState.whiteBalance.awbMode,
                )
                if (uiState.isManualFocus) {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF,
                    )
                    setCaptureRequestOption(
                        CaptureRequest.LENS_FOCUS_DISTANCE,
                        uiState.focusDistance,
                    )
                } else {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                    )
                }
                if (uiState.isManualExposure) {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF,
                    )
                    setCaptureRequestOption(
                        CaptureRequest.SENSOR_SENSITIVITY,
                        uiState.isoValue,
                    )
                    setCaptureRequestOption(
                        CaptureRequest.SENSOR_EXPOSURE_TIME,
                        uiState.shutterSpeedNs,
                    )
                } else {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON,
                    )
                }
            }.build()
        )
    }

    val currentUiState by rememberUpdatedState(uiState)
    LaunchedEffect(Unit) {
        viewModel.captureRequestFlow.collect {
            val capture = imageCapture ?: run {
                viewModel.onCaptureComplete()
                return@collect
            }
            val exec = captureExecutorRef.get() ?: run {
                viewModel.onCaptureComplete()
                return@collect
            }
            val filter = currentUiState.selectedFilter
            capture.takePicture(
                exec,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        var source: Bitmap? = null
                        var rotated: Bitmap? = null
                        var filtered: Bitmap? = null
                        try {
                            source = image.toBitmap()
                            rotated = source.rotatedBitmap(image.imageInfo.rotationDegrees)
                            filtered = applyFilter(rotated, filter)
                            saveToGallery(context, filtered)
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Capture processing failed", e)
                        } finally {
                            filtered?.recycle()
                            if (rotated !== source) rotated?.recycle()
                            source?.recycle()
                            image.close()
                            viewModel.onCaptureComplete()
                        }
                    }

                    override fun onError(e: ImageCaptureException) {
                        viewModel.onCaptureComplete()
                    }
                },
            )
        }
    }
}

// ── Frame processing (ImageAnalysis) ─────────────────────────────────────────

private fun processFrame(
    imageProxy: ImageProxy,
    onFrameReady: (Bitmap) -> Unit,
) {
    try {
        val source = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val rotated = source.rotatedBitmap(rotationDegrees)
        if (rotated !== source) source.recycle()
        onFrameReady(rotated)
    } finally {
        imageProxy.close()
    }
}

// ── Bitmap helpers ────────────────────────────────────────────────────────────

private fun Bitmap.rotatedBitmap(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, false)
}

private fun applyFilter(source: Bitmap, filter: CameraFilter): Bitmap {
    val output = createBitmap(source.width, source.height)
    val canvas = Canvas(output)
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(filter.buildMatrix()) }
    canvas.drawBitmap(source, 0f, 0f, paint)
    return output
}

private fun saveToGallery(context: Context, bitmap: Bitmap) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
    ) ?: return
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
    }
}

// ── Permission UI ─────────────────────────────────────────────────────────────

@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                "需要相機權限",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "此功能需要存取相機，才能顯示即時濾鏡效果。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) { Text("授予權限") }
        }
    }
}
