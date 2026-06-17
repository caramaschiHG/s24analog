package com.roll24.camera

import android.content.Context
import android.content.res.Configuration
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roll24.Roll24ViewModel
import com.roll24.film.FilmLabSettings
import com.roll24.film.FilmProfileRepository
import com.roll24.gallery.LocalGalleryScreen
import com.roll24.haptics.rememberRoll24Haptics
import com.roll24.ui.components.CaptureButton
import com.roll24.ui.components.FilmSelector
import com.roll24.ui.theme.Roll24Colors
import com.roll24.ui.theme.Roll24Radius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraScreen(
    viewModel: Roll24ViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
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
    var showLab by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }

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
        val rotation = targetPreview.display?.rotation ?: view.display.rotation
        val activeCameraId = cameraUiState.activeLens?.cameraId
        val cameraSelector = if (activeCameraId != null) {
            CameraSelector.Builder()
                .addCameraFilter { cameraInfos ->
                    cameraInfos.filter { cameraInfo ->
                        runCatching { Camera2CameraInfo.from(cameraInfo).cameraId == activeCameraId }
                            .getOrDefault(false)
                    }.ifEmpty { cameraInfos }
                }
                .build()
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val aspectRatio = cameraSettings.aspect.toCameraXAspectRatio()

        val preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also { it.setSurfaceProvider(targetPreview.surfaceProvider) }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .setFlashMode(cameraSettings.flashMode.toCameraXFlashMode())
            .build()

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

        CameraHud(
            profileName = selectedProfile.name,
            iso = selectedProfile.baseIso,
            cameraSettings = cameraSettings,
            labSettings = labSettings,
            uiState = cameraUiState,
            showControls = showControls,
            showLab = showLab,
            onToggleControls = { showControls = !showControls },
            onToggleLab = { showLab = !showLab },
            onAspectChange = viewModel::updateAspect,
            onGridChange = viewModel::updateGridMode,
            onFlashChange = viewModel::updateFlashMode,
            onTimerChange = viewModel::updateTimerMode,
            onEvChange = viewModel::updateExposureCompensation,
            onLabChange = viewModel::updateFilmLabSettings,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        SensorRail(
            sensors = cameraUiState.sensorProfiles,
            active = cameraUiState.activeLens,
            onSelected = { profile -> viewModel.selectLens(context, profile) },
            modifier = Modifier
                .align(if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 10.dp)
                .navigationBarsPadding()
        )

        CaptureJobStrip(
            jobs = cameraUiState.captureJobs,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 92.dp, end = 12.dp)
        )

        FilmSelector(
            profiles = FilmProfileRepository.profiles,
            selectedProfile = selectedProfile,
            onProfileSelected = { profile -> viewModel.selectProfile(context, profile) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 132.dp)
                .navigationBarsPadding()
        )

        CaptureButton(
            onClick = {
                val capture = imageCapture
                if (capture == null || cameraUiState.isCapturing) {
                    return@CaptureButton
                }

                scope.launch {
                    if (cameraSettings.timerMode.delayMillis > 0) {
                        delay(cameraSettings.timerMode.delayMillis)
                    }

                    haptics.shutterRelease()
                    val jobId = viewModel.beginCaptureFeedback(context)
                    capture.takePictureToTempFile(
                        context = context,
                        onSaved = { file -> viewModel.developAndSaveCapture(context, file, jobId) },
                        onError = { message -> viewModel.captureFailed(context, jobId, message) }
                    )
                }
            },
            enabled = cameraUiState.isCameraReady && !cameraUiState.isCapturing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
        )

        GalleryLauncher(
            count = galleryCaptures.size,
            lastLabel = cameraUiState.lastSavedCapture?.label,
            onClick = { showGallery = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 28.dp)
                .navigationBarsPadding()
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

@Composable
private fun SensorRail(
    sensors: List<SensorProfile>,
    active: SensorProfile?,
    onSelected: (SensorProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sensors.isEmpty()) return

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Roll24Colors.InkBlack.copy(alpha = 0.62f))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        sensors.take(6).forEach { sensor ->
            val selected = sensor == active
            Text(
                text = sensor.lensLabel,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) Roll24Colors.WarmGold else Roll24Colors.Raised)
                    .clickable { onSelected(sensor) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                color = if (selected) Roll24Colors.InkBlack else Roll24Colors.Paper,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CaptureJobStrip(
    jobs: List<CaptureJob>,
    modifier: Modifier = Modifier
) {
    val visibleJobs = jobs.take(4)
    if (visibleJobs.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        visibleJobs.forEach { job ->
            val color = when (job.stage) {
                CaptureJobStage.SAVED -> Roll24Colors.WarmGold
                CaptureJobStage.FAILED -> Color(0xFFFF8C8C)
                else -> Roll24Colors.Paper
            }
            Text(
                text = "${job.stage.name.lowercase()} ${(job.progress * 100).roundToInt()}%",
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Roll24Colors.InkBlack.copy(alpha = 0.66f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = color
            )
        }
    }
}

@Composable
private fun GalleryLauncher(
    count: Int,
    lastLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Roll24Radius.Md))
            .background(Roll24Colors.InkBlack.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Galeria", color = Roll24Colors.WarmGold, fontWeight = FontWeight.SemiBold)
        Text("$count locais", color = Roll24Colors.Paper)
        if (lastLabel != null) {
            Text("ultima salva", color = Roll24Colors.MutedText)
        }
    }
}

@Composable
private fun CameraHud(
    profileName: String,
    iso: Int,
    cameraSettings: CameraSettings,
    labSettings: FilmLabSettings,
    uiState: CameraUiState,
    showControls: Boolean,
    showLab: Boolean,
    onToggleControls: () -> Unit,
    onToggleLab: () -> Unit,
    onAspectChange: (ViewfinderAspect) -> Unit,
    onGridChange: (GridMode) -> Unit,
    onFlashChange: (Roll24FlashMode) -> Unit,
    onTimerChange: (TimerMode) -> Unit,
    onEvChange: (Float) -> Unit,
    onLabChange: (FilmLabSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Roll24Radius.Md))
                .background(Roll24Colors.InkBlack.copy(alpha = 0.70f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profileName, color = Roll24Colors.WarmGold, fontWeight = FontWeight.SemiBold)
                Text("ISO $iso  EV ${"%.1f".format(cameraSettings.exposureCompensation)}", color = Roll24Colors.MutedText)
            }
            StatusPill(text = if (uiState.isDeveloping) "Revelando" else "Pronta")
            Spacer(Modifier.width(8.dp))
            TextButtonPill("Cam", showControls, onToggleControls)
            Spacer(Modifier.width(8.dp))
            TextButtonPill("Lab", showLab, onToggleLab)
        }

        uiState.lastSavedCapture?.let {
            Text(
                text = "Salvo: ${it.label}",
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(Roll24Radius.Md))
                    .background(Roll24Colors.InkBlack.copy(alpha = 0.72f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = Roll24Colors.Paper
            )
        }

        uiState.errorMessage?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Roll24Radius.Md))
                    .background(Color(0xFF642020).copy(alpha = 0.88f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                color = Color.White
            )
        }

        AnimatedVisibility(visible = showControls) {
            CameraControlsPanel(
                settings = cameraSettings,
                onAspectChange = onAspectChange,
                onGridChange = onGridChange,
                onFlashChange = onFlashChange,
                onTimerChange = onTimerChange,
                onEvChange = onEvChange
            )
        }

        AnimatedVisibility(visible = showLab) {
            LabPanel(settings = labSettings, onChange = onLabChange)
        }
    }
}

@Composable
private fun CameraControlsPanel(
    settings: CameraSettings,
    onAspectChange: (ViewfinderAspect) -> Unit,
    onGridChange: (GridMode) -> Unit,
    onFlashChange: (Roll24FlashMode) -> Unit,
    onTimerChange: (TimerMode) -> Unit,
    onEvChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Roll24Radius.Md))
            .background(Roll24Colors.Panel.copy(alpha = 0.92f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OptionRow("Moldura", ViewfinderAspect.values().toList(), settings.aspect, { it.label }, onAspectChange)
        OptionRow("Grade", GridMode.values().toList(), settings.gridMode, { it.label }, onGridChange)
        OptionRow("Flash", Roll24FlashMode.values().toList(), settings.flashMode, { it.label }, onFlashChange)
        OptionRow("Timer", TimerMode.values().toList(), settings.timerMode, { it.label }, onTimerChange)
        SliderRow("EV", settings.exposureCompensation, -2f..2f, onEvChange)
    }
}

@Composable
private fun LabPanel(
    settings: FilmLabSettings,
    onChange: (FilmLabSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Roll24Radius.Md))
            .background(Roll24Colors.Panel.copy(alpha = 0.94f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SliderRow("Filme", settings.filmIntensity, 0f..1.25f) { onChange(settings.copy(filmIntensity = it)) }
        SliderRow("Push/Pull", settings.pushPull, -2f..2f) { onChange(settings.copy(pushPull = it)) }
        SliderRow("Grao", settings.grainAmount, 0f..2f) { onChange(settings.copy(grainAmount = it)) }
        SliderRow("Halation", settings.halationAmount, 0f..2f) { onChange(settings.copy(halationAmount = it)) }
        SliderRow("Bloom", settings.bloomAmount, 0f..2f) { onChange(settings.copy(bloomAmount = it)) }
        SliderRow("Vinheta", settings.vignetteAmount, 0f..2f) { onChange(settings.copy(vignetteAmount = it)) }
        SliderRow("Calor", settings.warmth, -1f..1f) { onChange(settings.copy(warmth = it)) }
        SliderRow("Contraste", settings.contrast, -0.5f..0.5f) { onChange(settings.copy(contrast = it)) }
    }
}

@Composable
private fun <T> OptionRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Roll24Colors.Paper, modifier = Modifier.width(74.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                TextButtonPill(
                    text = optionLabel(option),
                    selected = option == selected,
                    onClick = { onSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Roll24Colors.Paper, modifier = Modifier.width(82.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text("%.1f".format(value), color = Roll24Colors.MutedText, modifier = Modifier.width(42.dp))
    }
}

@Composable
private fun TextButtonPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Roll24Colors.WarmGold else Roll24Colors.Raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = if (selected) Roll24Colors.InkBlack else Roll24Colors.Paper,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
    )
}

@Composable
private fun StatusPill(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Roll24Colors.Raised)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val alpha by animateFloatAsState(targetValue = 1f, label = "status")
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Roll24Colors.WarmGold.copy(alpha = alpha))
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = Roll24Colors.Paper)
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
        Camera2CameraControl.from(cameraControl).captureRequestOptions = options
    } catch (e: Exception) {
        Log.w("CameraScreen", "Failed to apply minimal-processing capture keys; falling back to automatic mode", e)
    }
}

private fun ImageCapture.takePictureToTempFile(
    context: Context,
    onSaved: (File) -> Unit,
    onError: (String) -> Unit
) {
    val file = try {
        File.createTempFile("roll24_capture_", ".jpg", context.cacheDir)
    } catch (e: Exception) {
        onError(e.message ?: "Falha ao criar arquivo temporario")
        return
    }

    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(file)
            }

            override fun onError(exception: ImageCaptureException) {
                file.delete()
                onError(exception.message ?: "Falha ao capturar foto")
            }
        }
    )
}
