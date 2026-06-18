package com.roll24.camera

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roll24.R
import com.roll24.film.FilmLabSettings
import com.roll24.film.FilmProfile
import com.roll24.film.FilmProfileRepository
import com.roll24.ui.components.CaptureButton
import com.roll24.ui.components.FilmSelector
import com.roll24.ui.theme.Roll24Colors
import com.roll24.ui.theme.Roll24Radius
import com.roll24.ui.theme.Roll24Spacing

internal const val TAG_CAMERA_CHROME = "camera_chrome"
internal const val TAG_PORTRAIT_CONTROLS = "portrait_controls"
internal const val TAG_LANDSCAPE_CONTROLS = "landscape_controls"
internal const val TAG_ADAPTIVE_PANEL = "adaptive_panel"

@Composable
internal fun ResponsiveCameraChrome(
    selectedProfile: FilmProfile,
    cameraSettings: CameraSettings,
    labSettings: FilmLabSettings,
    uiState: CameraUiState,
    galleryCount: Int,
    activePanel: CameraPanel,
    onPanelChange: (CameraPanel) -> Unit,
    onLensSelected: (SensorProfile) -> Unit,
    onProfileSelected: (FilmProfile) -> Unit,
    onAspectChange: (ViewfinderAspect) -> Unit,
    onGridChange: (GridMode) -> Unit,
    onFlashChange: (Roll24FlashMode) -> Unit,
    onTimerChange: (TimerMode) -> Unit,
    onEvChange: (Float) -> Unit,
    onLabChange: (FilmLabSettings) -> Unit,
    onCapture: () -> Unit,
    captureEnabled: Boolean,
    onGalleryOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = activePanel != CameraPanel.NONE) {
        onPanelChange(CameraPanel.NONE)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag(TAG_CAMERA_CHROME)) {
        val layout = resolveWindowLayout(maxWidth.value, maxHeight.value)

        CameraStatusBar(
            profile = selectedProfile,
            settings = cameraSettings,
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(horizontal = Roll24Spacing.Md, vertical = Roll24Spacing.Sm)
                .widthIn(max = if (layout.mode == CameraLayoutMode.LANDSCAPE) 420.dp else 620.dp)
        )

        CameraFeedback(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(top = 76.dp, start = Roll24Spacing.Md, end = Roll24Spacing.Md)
                .widthIn(max = 520.dp)
        )

        if (layout.mode == CameraLayoutMode.PORTRAIT) {
            PortraitControls(
                selectedProfile = selectedProfile,
                sensors = uiState.sensorProfiles,
                activeLens = uiState.activeLens,
                galleryCount = galleryCount,
                activePanel = activePanel,
                onPanelChange = onPanelChange,
                onLensSelected = onLensSelected,
                onCapture = onCapture,
                captureEnabled = captureEnabled,
                onGalleryOpen = onGalleryOpen,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            LandscapeControls(
                selectedProfile = selectedProfile,
                sensors = uiState.sensorProfiles,
                activeLens = uiState.activeLens,
                galleryCount = galleryCount,
                activePanel = activePanel,
                compact = layout.compactHeight,
                onPanelChange = onPanelChange,
                onLensSelected = onLensSelected,
                onCapture = onCapture,
                captureEnabled = captureEnabled,
                onGalleryOpen = onGalleryOpen
            )
        }

        AdaptivePanel(
            panel = activePanel,
            layout = layout,
            maxHeight = maxHeight,
            onClose = { onPanelChange(CameraPanel.NONE) }
        ) {
            when (activePanel) {
                CameraPanel.CAMERA -> CameraSettingsContent(
                    settings = cameraSettings,
                    onAspectChange = onAspectChange,
                    onGridChange = onGridChange,
                    onFlashChange = onFlashChange,
                    onTimerChange = onTimerChange,
                    onEvChange = onEvChange
                )
                CameraPanel.LAB -> LabSettingsContent(settings = labSettings, onChange = onLabChange)
                CameraPanel.FILMS -> FilmSelector(
                    profiles = FilmProfileRepository.profiles,
                    selectedProfile = selectedProfile,
                    onProfileSelected = {
                        onProfileSelected(it)
                        onPanelChange(CameraPanel.NONE)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                CameraPanel.NONE -> Unit
            }
        }
    }
}

@Composable
private fun CameraStatusBar(
    profile: FilmProfile,
    settings: CameraSettings,
    uiState: CameraUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Roll24Radius.Md))
            .background(Roll24Colors.InkBlack.copy(alpha = 0.82f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = Roll24Colors.Paper,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.camera_exposure_summary, profile.baseIso, settings.exposureCompensation),
                color = Roll24Colors.MutedText,
                style = MaterialTheme.typography.labelSmall
            )
        }
        StatusIndicator(isBusy = uiState.isCapturing || uiState.isDeveloping)
    }
}

@Composable
private fun StatusIndicator(isBusy: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isBusy) Roll24Colors.WarmGold else Roll24Colors.Success)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = stringResource(if (isBusy) R.string.status_developing else R.string.status_ready),
            color = Roll24Colors.Paper,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun CameraFeedback(uiState: CameraUiState, modifier: Modifier = Modifier) {
    val error = uiState.visibleError ?: uiState.errorMessage
    val notice = uiState.visibleNotice
    val activeJob = uiState.captureJobs.firstOrNull {
        it.stage == CaptureJobStage.QUEUED || it.stage == CaptureJobStage.CAPTURING || it.stage == CaptureJobStage.DEVELOPING
    }
    val message = when {
        error != null -> error
        notice != null -> notice
        activeJob != null -> stringResource(R.string.capture_progress, (activeJob.progress * 100).toInt())
        uiState.lastSavedCapture != null -> stringResource(R.string.capture_saved, uiState.lastSavedCapture.label)
        else -> null
    }
    AnimatedVisibility(visible = message != null, modifier = modifier) {
        if (message != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Roll24Radius.Sm))
                    .background(if (error != null) Roll24Colors.Danger else Roll24Colors.Panel.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        error != null -> Icons.Rounded.Warning
                        notice != null -> Icons.Rounded.Warning
                        activeJob != null -> Icons.Rounded.CameraAlt
                        else -> Icons.Rounded.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        error != null -> Color.White
                        notice != null -> Roll24Colors.WarmGold
                        else -> Roll24Colors.Success
                    },
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(message, color = Roll24Colors.Paper, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PortraitControls(
    selectedProfile: FilmProfile,
    sensors: List<SensorProfile>,
    activeLens: SensorProfile?,
    galleryCount: Int,
    activePanel: CameraPanel,
    onPanelChange: (CameraPanel) -> Unit,
    onLensSelected: (SensorProfile) -> Unit,
    onCapture: () -> Unit,
    captureEnabled: Boolean,
    onGalleryOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TAG_PORTRAIT_CONTROLS)
            .background(Roll24Colors.InkBlack.copy(alpha = 0.84f))
            .navigationBarsPadding()
            .padding(horizontal = Roll24Spacing.Md, vertical = Roll24Spacing.Sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LensSelector(sensors, activeLens, onLensSelected)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ChromeAction(
                icon = Icons.Rounded.PhotoLibrary,
                label = if (galleryCount > 0) galleryCount.toString() else stringResource(R.string.gallery),
                selected = false,
                onClick = onGalleryOpen
            )
            ChromeAction(
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.camera_controls),
                selected = activePanel == CameraPanel.CAMERA,
                onClick = { onPanelChange(togglePanel(activePanel, CameraPanel.CAMERA)) }
            )
            CaptureButton(onClick = onCapture, enabled = captureEnabled)
            ChromeAction(
                icon = Icons.Rounded.Science,
                label = stringResource(R.string.lab),
                selected = activePanel == CameraPanel.LAB,
                onClick = { onPanelChange(togglePanel(activePanel, CameraPanel.LAB)) }
            )
            FilmChip(
                profile = selectedProfile,
                selected = activePanel == CameraPanel.FILMS,
                onClick = { onPanelChange(togglePanel(activePanel, CameraPanel.FILMS)) }
            )
        }
    }
}

@Composable
private fun BoxScope.LandscapeControls(
    selectedProfile: FilmProfile,
    sensors: List<SensorProfile>,
    activeLens: SensorProfile?,
    galleryCount: Int,
    activePanel: CameraPanel,
    compact: Boolean,
    onPanelChange: (CameraPanel) -> Unit,
    onLensSelected: (SensorProfile) -> Unit,
    onCapture: () -> Unit,
    captureEnabled: Boolean,
    onGalleryOpen: () -> Unit
) {
    LensSelector(
        sensors = sensors,
        active = activeLens,
        onSelected = onLensSelected,
        vertical = true,
        modifier = Modifier
            .align(Alignment.CenterStart)
            .systemBarsPadding()
            .padding(start = 12.dp)
    )
    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .testTag(TAG_LANDSCAPE_CONTROLS)
            .systemBarsPadding()
            .padding(end = 10.dp)
            .clip(RoundedCornerShape(Roll24Radius.Lg))
            .background(Roll24Colors.InkBlack.copy(alpha = 0.86f))
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
    ) {
        ChromeAction(Icons.Rounded.PhotoLibrary, galleryCount.toString(), false, onGalleryOpen, compact)
        ChromeAction(Icons.Rounded.Tune, stringResource(R.string.camera_controls), activePanel == CameraPanel.CAMERA, {
            onPanelChange(togglePanel(activePanel, CameraPanel.CAMERA))
        }, compact)
        CaptureButton(onClick = onCapture, enabled = captureEnabled)
        ChromeAction(Icons.Rounded.Science, stringResource(R.string.lab), activePanel == CameraPanel.LAB, {
            onPanelChange(togglePanel(activePanel, CameraPanel.LAB))
        }, compact)
        FilmChip(selectedProfile, activePanel == CameraPanel.FILMS, {
            onPanelChange(togglePanel(activePanel, CameraPanel.FILMS))
        }, compact)
    }
}

@Composable
private fun LensSelector(
    sensors: List<SensorProfile>,
    active: SensorProfile?,
    onSelected: (SensorProfile) -> Unit,
    vertical: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (sensors.isEmpty()) return
    val content: @Composable () -> Unit = {
        sensors.take(6).forEach { sensor ->
            val selected = sensor == active
            Text(
                text = sensor.lensLabel,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(CircleShape)
                    .background(if (selected) Roll24Colors.WarmGold else Roll24Colors.Raised)
                    .clickable(role = Role.RadioButton) { onSelected(sensor) }
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                color = if (selected) Roll24Colors.InkBlack else Roll24Colors.Paper,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
    if (vertical) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(Roll24Radius.Lg))
                .background(Roll24Colors.InkBlack.copy(alpha = 0.78f))
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    } else {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
}

@Composable
private fun ChromeAction(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(if (compact) 52.dp else 62.dp)
            .clip(RoundedCornerShape(Roll24Radius.Sm))
            .background(if (selected) Roll24Colors.WarmGold else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Roll24Colors.InkBlack else Roll24Colors.Paper,
            modifier = Modifier.size(22.dp)
        )
        if (!compact) {
            Text(
                text = label,
                color = if (selected) Roll24Colors.InkBlack else Roll24Colors.MutedText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilmChip(
    profile: FilmProfile,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(if (compact) 52.dp else 68.dp)
            .clip(RoundedCornerShape(Roll24Radius.Sm))
            .background(if (selected) Roll24Colors.WarmGold else Roll24Colors.Raised)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.film),
            color = if (selected) Roll24Colors.InkBlack else Roll24Colors.WarmGold,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        if (!compact) {
            Text(
                text = profile.name,
                color = if (selected) Roll24Colors.InkBlack else Roll24Colors.Paper,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdaptivePanel(
    panel: CameraPanel,
    layout: WindowLayout,
    maxHeight: androidx.compose.ui.unit.Dp,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val visible = panel != CameraPanel.NONE
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onClose)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (layout.mode == CameraLayoutMode.PORTRAIT) slideInVertically { it } else slideInHorizontally { it },
        exit = if (layout.mode == CameraLayoutMode.PORTRAIT) slideOutVertically { it } else slideOutHorizontally { it },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = if (layout.mode == CameraLayoutMode.PORTRAIT) {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .testTag(TAG_ADAPTIVE_PANEL)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.68f)
                        .navigationBarsPadding()
                        .clip(RoundedCornerShape(topStart = Roll24Radius.Lg, topEnd = Roll24Radius.Lg))
                        .background(Roll24Colors.Panel)
                } else {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .testTag(TAG_ADAPTIVE_PANEL)
                        .fillMaxHeight()
                        .widthIn(min = 300.dp, max = 390.dp)
                        .padding(end = 86.dp)
                        .systemBarsPadding()
                        .clip(RoundedCornerShape(Roll24Radius.Lg))
                        .background(Roll24Colors.Panel)
                }
            ) {
                PanelHeader(panel = panel, onClose = onClose)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(panel: CameraPanel, onClose: () -> Unit) {
    val title = when (panel) {
        CameraPanel.CAMERA -> stringResource(R.string.camera_controls)
        CameraPanel.LAB -> stringResource(R.string.film_lab)
        CameraPanel.FILMS -> stringResource(R.string.choose_film)
        CameraPanel.NONE -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Roll24Colors.Paper, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.panel_hint), color = Roll24Colors.MutedText, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = Roll24Colors.Paper)
        }
    }
}

@Composable
private fun CameraSettingsContent(
    settings: CameraSettings,
    onAspectChange: (ViewfinderAspect) -> Unit,
    onGridChange: (GridMode) -> Unit,
    onFlashChange: (Roll24FlashMode) -> Unit,
    onTimerChange: (TimerMode) -> Unit,
    onEvChange: (Float) -> Unit
) {
    SettingSection(stringResource(R.string.frame)) {
        ChoiceRow(ViewfinderAspect.values().toList(), settings.aspect, { it.localizedLabel() }, onAspectChange)
    }
    SettingSection(stringResource(R.string.grid)) {
        ChoiceRow(GridMode.values().toList(), settings.gridMode, { it.localizedLabel() }, onGridChange)
    }
    SettingSection(stringResource(R.string.flash)) {
        ChoiceRow(Roll24FlashMode.values().toList(), settings.flashMode, { it.localizedLabel() }, onFlashChange)
    }
    SettingSection(stringResource(R.string.timer)) {
        ChoiceRow(TimerMode.values().toList(), settings.timerMode, { it.localizedLabel() }, onTimerChange)
    }
    EditorialSlider(stringResource(R.string.exposure), settings.exposureCompensation, -2f..2f, onEvChange)
}

@Composable
private fun LabSettingsContent(settings: FilmLabSettings, onChange: (FilmLabSettings) -> Unit) {
    EditorialSlider(stringResource(R.string.film_intensity), settings.filmIntensity, 0f..1.25f) { onChange(settings.copy(filmIntensity = it)) }
    EditorialSlider(stringResource(R.string.push_pull), settings.pushPull, -2f..2f) { onChange(settings.copy(pushPull = it)) }
    EditorialSlider(stringResource(R.string.grain), settings.grainAmount, 0f..2f) { onChange(settings.copy(grainAmount = it)) }
    EditorialSlider(stringResource(R.string.halation), settings.halationAmount, 0f..2f) { onChange(settings.copy(halationAmount = it)) }
    EditorialSlider(stringResource(R.string.bloom), settings.bloomAmount, 0f..2f) { onChange(settings.copy(bloomAmount = it)) }
    EditorialSlider(stringResource(R.string.vignette), settings.vignetteAmount, 0f..2f) { onChange(settings.copy(vignetteAmount = it)) }
    EditorialSlider(stringResource(R.string.warmth), settings.warmth, -1f..1f) { onChange(settings.copy(warmth = it)) }
    EditorialSlider(stringResource(R.string.contrast), settings.contrast, -0.5f..0.5f) { onChange(settings.copy(contrast = it)) }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), color = Roll24Colors.WarmGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun <T> ChoiceRow(options: List<T>, selected: T, label: @Composable (T) -> String, onSelected: (T) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                text = label(option),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(Roll24Radius.Sm))
                    .background(if (isSelected) Roll24Colors.WarmGold else Roll24Colors.Raised)
                    .clickable(role = Role.RadioButton) { onSelected(option) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                color = if (isSelected) Roll24Colors.InkBlack else Roll24Colors.Paper,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EditorialSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Roll24Radius.Md))
            .background(Roll24Colors.Raised.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Roll24Colors.Paper, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(String.format("%.1f", value), color = Roll24Colors.WarmGold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Text(
            text = stringResource(R.string.slider_range, range.start, range.endInclusive),
            color = Roll24Colors.MutedText,
            style = MaterialTheme.typography.labelSmall
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label }
        )
    }
}

@Composable
private fun ViewfinderAspect.localizedLabel(): String = label

@Composable
private fun GridMode.localizedLabel(): String = when (this) {
    GridMode.OFF -> stringResource(R.string.off)
    GridMode.THIRDS -> stringResource(R.string.thirds)
    GridMode.CROSS -> stringResource(R.string.cross)
    GridMode.DIAGONALS -> stringResource(R.string.diagonals)
}

@Composable
private fun Roll24FlashMode.localizedLabel(): String = when (this) {
    Roll24FlashMode.OFF -> stringResource(R.string.off)
    Roll24FlashMode.AUTO -> stringResource(R.string.auto)
    Roll24FlashMode.ON -> stringResource(R.string.on)
}

@Composable
private fun TimerMode.localizedLabel(): String = when (this) {
    TimerMode.OFF -> stringResource(R.string.off)
    TimerMode.THREE_SECONDS -> stringResource(R.string.three_seconds)
    TimerMode.TEN_SECONDS -> stringResource(R.string.ten_seconds)
}
