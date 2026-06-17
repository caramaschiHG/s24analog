package com.roll24.camera

import android.net.Uri

enum class CaptureJobStage {
    QUEUED,
    CAPTURING,
    DEVELOPING,
    SAVED,
    FAILED
}

data class CaptureOutputUris(
    val rawUri: Uri? = null,
    val negativeUri: Uri? = null,
    val developedUri: Uri? = null,
    val thumbnailUri: Uri? = null,
    val galleryUri: Uri? = null
)

data class CaptureJob(
    val id: String,
    val label: String,
    val stage: CaptureJobStage,
    val progress: Float = 0f,
    val outputUris: CaptureOutputUris = CaptureOutputUris(),
    val error: String? = null
)

data class SavedCapture(
    val negativeUri: Uri?,
    val developedUri: Uri?,
    val thumbnailUri: Uri?,
    val galleryUri: Uri?,
    val label: String
)

data class SensorProfile(
    val cameraId: String,
    val physicalId: String?,
    val lensLabel: String,
    val supportsRaw: Boolean,
    val supportsManual: Boolean,
    val focalLengthMm: Float?,
    val aperture: Float?,
    val isoRange: String?,
    val exposureRange: String?,
    val rawSizes: List<String>,
    val yuvSizes: List<String>,
    val manufacturer: String = "Unknown",
    val modelName: String = "Unknown",
    val resolutionMp: Float = 0f,
    val pixelSizeUm: Float = 0f,
    val opticalFormat: String = "Unknown",
    val cfaPattern: String = "RGGB",
    val binningModes: List<String> = emptyList(),
    val hasOis: Boolean = false,
    val technologies: List<String> = emptyList(),
    val nativeFocalLengthMm: Float = focalLengthMm ?: 0f,
    val equivalentFocalLengthMm: Float = focalLengthMm ?: 0f,
    val matchedDossierSpec: com.roll24.sensor.SensorSpec? = null
) {
    val isS24UltraSensor: Boolean get() = matchedDossierSpec != null
}

data class CameraUiState(
    val isCameraReady: Boolean = false,
    val isCapturing: Boolean = false,
    val isDeveloping: Boolean = false,
    val activeLens: SensorProfile? = null,
    val sensorProfiles: List<SensorProfile> = emptyList(),
    val queueDepth: Int = 0,
    val captureJobs: List<CaptureJob> = emptyList(),
    val lastSavedId: String? = null,
    val shutterFlash: Boolean = false,
    val lastSavedCapture: SavedCapture? = null,
    val errorMessage: String? = null,
    val visibleError: String? = null
)
