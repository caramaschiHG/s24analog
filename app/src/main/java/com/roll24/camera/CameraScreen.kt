@file:Suppress("UnsafeOptInUsageError")
@file:OptIn(ExperimentalCamera2Interop::class)
package com.roll24.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roll24.Roll24ViewModel
import com.roll24.gallery.LocalGalleryScreen
import com.roll24.haptics.rememberRoll24Haptics
import com.roll24.ui.theme.Roll24Colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraScreen(
    viewModel: Roll24ViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val haptics = rememberRoll24Haptics()

    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val cameraSettings by viewModel.cameraSettings.collectAsState()
    val labSettings by viewModel.filmLabSettings.collectAsState()
    val cameraUiState by viewModel.cameraUiState.collectAsState()
    val galleryCaptures by viewModel.galleryCaptures.collectAsState()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var rawCaptureEnabled by remember { mutableStateOf(false) }
    var cameraCharacteristics by remember { mutableStateOf<CameraCharacteristics?>(null) }
    val captureResultStore = remember { CaptureResultStore() }
    var activePanel by rememberSaveable { mutableStateOf(CameraPanel.NONE) }
    var showGallery by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showGallery) {
        showGallery = false
    }

    LaunchedEffect(Unit) {
        viewModel.startGallery(context)
        viewModel.scanSensors(context)
    }

    LaunchedEffect(cameraUiState.shutterFlash) {
        if (cameraUiState.shutterFlash) {
            delay(120)
            viewModel.clearShutterFlash()
        }
    }

    LaunchedEffect(previewView, cameraSettings.aspect, cameraSettings.cleanCaptureMode, cameraUiState.activeLens) {
        val targetPreview = previewView ?: return@LaunchedEffect
        val provider = context.cameraProvider()
        val rotation = targetPreview.display?.rotation
            ?: view.display?.rotation
            ?: android.view.Surface.ROTATION_0
        // Always bind the default back camera. Lens switching is done via zoom
        // ratio (the system auto-picks the physical camera that matches the
        // requested zoom: 0.6x → ultra-wide, 1x → main, 3x → tele1, 5x → tele2).
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val aspectRatio = cameraSettings.aspect.toCameraXAspectRatio()

        val preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also { it.setSurfaceProvider(targetPreview.surfaceProvider) }

        val selectedInfo = cameraSelector.filter(provider.availableCameraInfos).firstOrNull()
        val supportedFormats = selectedInfo?.let {
            ImageCapture.getImageCaptureCapabilities(it).supportedOutputFormats
        }.orEmpty()
        val useRaw = cameraUiState.activeLens?.supportsRaw == true &&
            ImageCapture.OUTPUT_FORMAT_RAW_JPEG in supportedFormats
        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .setFlashMode(cameraSettings.flashMode.toCameraXFlashMode())
        if (useRaw) captureBuilder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)
        captureResultStore.clear()
        Camera2Interop.Extender(captureBuilder)
            .setSessionCaptureCallback(captureResultStore.callback)
            .setCaptureRequestOption(
                CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE,
                CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_ON
            )
        val capture = captureBuilder.build()

        provider.unbindAll()
        val boundCamera = provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            capture
        )

        applyCleanCaptureMode(boundCamera, cameraSettings.cleanCaptureMode)

        cameraUiState.activeLens?.let { lens ->
            val hint = SensorCaptureHints.forProfile(lens)
            Log.d("SensorCaptureHints", "SensorCaptureHints: $hint for ${lens.lensLabel}")
        }

        camera = boundCamera
        imageCapture = capture
        rawCaptureEnabled = useRaw
        cameraCharacteristics = runCatching {
            val cameraId = Camera2CameraInfo.from(boundCamera.cameraInfo).cameraId
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.getCameraCharacteristics(cameraId)
        }.onFailure {
            Log.w("CameraScreen", "Could not resolve Camera2 characteristics", it)
        }.getOrNull()

        // Apply zoom ratio matching the active lens label (0.6x / 1x / 3x / 5x).
        // The system uses the closest physical sub-camera for the requested zoom.
        cameraUiState.activeLens?.lensLabel?.let { label ->
            val zoom = when (label) {
                "0.6x" -> 0.6f
                "1x" -> 1f
                "3x" -> 3f
                "5x" -> 5f
                else -> 1f
            }
            runCatching {
                boundCamera.cameraControl.setZoomRatio(zoom)
            }.onFailure {
                Log.w("CameraScreen", "setZoomRatio($zoom) failed for $label", it)
            }
        }

        viewModel.setCameraReady(true)
    }

    LaunchedEffect(cameraSettings.flashMode, imageCapture) {
        imageCapture?.flashMode = cameraSettings.flashMode.toCameraXFlashMode()
    }

    LaunchedEffect(cameraSettings.exposureCompensation, camera) {
        val cameraInfo = camera?.cameraInfo ?: return@LaunchedEffect
        val cameraControl = camera?.cameraControl ?: return@LaunchedEffect
        val range = cameraInfo.exposureState.exposureCompensationRange
        if (range.lower <= range.upper) {
            val stepValue = cameraInfo.exposureState.exposureCompensationStep
            val step = stepValue.numerator.toFloat() / stepValue.denominator.toFloat()
            val index = if (step > 0f) {
                (cameraSettings.exposureCompensation / step).roundToInt()
            } else {
                0
            }.coerceIn(range.lower, range.upper)
            cameraControl.setExposureCompensationIndex(index)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Roll24Colors.InkBlack)
    ) {
        AnalogViewfinder(
            settings = cameraSettings,
            camera = camera,
            onPreviewViewReady = { previewView = it },
            onFocusTap = {
                if (!cameraSettings.focusLocked) haptics.shutterHalfPress()
            }
        )

        if (cameraUiState.shutterFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.42f))
            )
        }

        ResponsiveCameraChrome(
            selectedProfile = selectedProfile,
            cameraSettings = cameraSettings,
            labSettings = labSettings,
            uiState = cameraUiState,
            galleryCount = galleryCaptures.size,
            activePanel = activePanel,
            onPanelChange = { activePanel = it },
            onLensSelected = { profile -> viewModel.selectLens(context, profile) },
            onProfileSelected = { profile -> viewModel.selectProfile(context, profile) },
            onAspectChange = viewModel::updateAspect,
            onGridChange = viewModel::updateGridMode,
            onFlashChange = viewModel::updateFlashMode,
            onTimerChange = viewModel::updateTimerMode,
            onEvChange = viewModel::updateExposureCompensation,
            onLabChange = viewModel::updateFilmLabSettings,
            onCapture = {
                val capture = imageCapture
                if (capture != null && !cameraUiState.isCapturing) {
                    scope.launch {
                        if (cameraSettings.timerMode.delayMillis > 0) {
                            delay(cameraSettings.timerMode.delayMillis)
                        }

                        haptics.shutterRelease()
                        val jobId = viewModel.beginCaptureFeedback(context)
                        runCatching {
                            RawCaptureSession.capture(
                                imageCapture = capture,
                                expectsRaw = rawCaptureEnabled,
                                characteristics = cameraCharacteristics,
                                resultStore = captureResultStore
                            )
                        }.onSuccess { result ->
                            viewModel.developAndSaveCapture(context, result, jobId)
                        }.onFailure { error ->
                            viewModel.captureFailed(
                                context,
                                jobId,
                                error.message ?: "Falha ao capturar a foto"
                            )
                        }
                    }
                }
            },
            captureEnabled = cameraUiState.isCameraReady && !cameraUiState.isCapturing,
            onGalleryOpen = {
                activePanel = CameraPanel.NONE
                showGallery = true
            }
        )

        if (showGallery) {
            LocalGalleryScreen(
                captures = galleryCaptures,
                onClose = { showGallery = false },
                onRemoveLocal = { record -> viewModel.removeCaptureFromLocalGallery(context, record) }
            )
        }
    }
}
@Composable
private fun AnalogViewfinder(
    settings: CameraSettings,
    camera: Camera?,
    onPreviewViewReady: (PreviewView) -> Unit,
    onFocusTap: () -> Unit
) {
    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var lastTap by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(lastTap) {
        if (lastTap != null) {
            delay(1200)
            lastTap = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val screenRatio = maxWidth.value / maxHeight.value
        val matchHeight = screenRatio > settings.aspect.ratio
        val frameModifier = if (matchHeight) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(settings.aspect.ratio)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(settings.aspect.ratio)
        }

        Box(
            modifier = frameModifier
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, Roll24Colors.WarmGold.copy(alpha = 0.38f))
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        activePreviewView = this
                        onPreviewViewReady(this)
                    }
                },
                update = { targetPreview ->
                    targetPreview.setOnTouchListener { _, event ->
                        if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener true
                        val activeCamera = camera ?: return@setOnTouchListener true
                        if (settings.focusLocked) return@setOnTouchListener true

                        val point = targetPreview.meteringPointFactory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        activeCamera.cameraControl.startFocusAndMetering(action)
                        lastTap = Offset(event.x, event.y)
                        onFocusTap()
                        true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            GridOverlay(mode = settings.gridMode)

            lastTap?.let { tap ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Roll24Colors.WarmGold,
                        radius = 34.dp.toPx(),
                        center = tap,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun GridOverlay(mode: GridMode) {
    if (mode == GridMode.OFF) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 1.dp.toPx()
        val color = Color.White.copy(alpha = 0.38f)

        when (mode) {
            GridMode.THIRDS -> {
                drawLine(color, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), stroke)
                drawLine(color, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), stroke)
                drawLine(color, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), stroke)
                drawLine(color, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), stroke)
            }
            GridMode.CROSS -> {
                drawLine(color, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), stroke)
                drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), stroke)
            }
            GridMode.DIAGONALS -> {
                drawLine(color, Offset.Zero, Offset(size.width, size.height), stroke)
                drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), stroke)
            }
            GridMode.OFF -> Unit
        }
    }
}

private fun Roll24FlashMode.toCameraXFlashMode(): Int {
    return when (this) {
        Roll24FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        Roll24FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
        Roll24FlashMode.ON -> ImageCapture.FLASH_MODE_ON
    }
}

private fun ViewfinderAspect.toCameraXAspectRatio(): Int {
    return when (this) {
        ViewfinderAspect.SENSOR_4_3 -> AspectRatio.RATIO_4_3
        ViewfinderAspect.WIDE_16_9 -> AspectRatio.RATIO_16_9
        else -> AspectRatio.RATIO_4_3
    }
}

private suspend fun Context.cameraProvider(): ProcessCameraProvider {
    val future = ProcessCameraProvider.getInstance(this)
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(this)
        )
    }
}

@OptIn(ExperimentalCamera2Interop::class)
private fun applyCleanCaptureMode(camera: Camera?, mode: CleanCaptureMode) {
    if (mode != CleanCaptureMode.MINIMAL_PROCESSING || camera == null) return

    try {
        val cameraControl = camera.cameraControl
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE)
            .setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF)
            .setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF)
            .setCaptureRequestOption(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_FAST)
            .build()
        Camera2CameraControl.from(cameraControl).setCaptureRequestOptions(options)
    } catch (e: Exception) {
        Log.w("CameraScreen", "Failed to apply minimal-processing capture keys; falling back to automatic mode", e)
    }
}
