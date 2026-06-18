package com.roll24.camera.probe

import android.util.Size

/**
 * Complete hardware report for a single physical camera on the S24 Ultra.
 * Collected by [S24UltraHardwareProbe] at runtime.
 */
data class CameraLensReport(
    val cameraId: String,
    val physicalCameraIds: List<String>,
    val lensFacing: Int,
    val focalLengths: List<Float>,
    val apertures: List<Float>,
    val capabilities: List<Int>,
    val hardwareLevel: Int,
    val rawSensorSizes: List<Size>,
    val yuvSizes: List<Size>,
    val jpegSizes: List<Size>,
    val supportedOutputFormats: List<Int>,
    val sensitivityRange: IntRange?,
    val exposureTimeRangeNs: LongRange?,
    val afModes: List<Int>,
    val aeModes: List<Int>,
    val awbModes: List<Int>,
    val noiseReductionModes: List<Int>,
    val edgeModes: List<Int>,
    val tonemapModes: List<Int>,
    val activeArraySize: Size?,
    val preCorrectionActiveArraySize: Size?,
    val sensorOrientation: Int,
    val whiteLevel: Int?,
    val blackLevelPattern: IntArray?,
    val cfaArrangement: Int?,
    val lensShadingMapAvailable: Boolean,
    val maxDigitalZoom: Float?,
    val opticalStabilization: Boolean,
    val videoStabilization: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CameraLensReport) return false
        return cameraId == other.cameraId
    }

    override fun hashCode(): Int = cameraId.hashCode()
}

/**
 * Aggregated hardware report for all rear-facing cameras on the device.
 */
data class S24UltraHardwareReport(
    val deviceModel: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val totalRearCameras: Int,
    val lensReports: List<CameraLensReport>,
    val timestampMs: Long = System.currentTimeMillis()
)
