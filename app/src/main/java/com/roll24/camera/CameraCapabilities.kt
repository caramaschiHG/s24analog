package com.roll24.camera

import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import android.util.Size

data class CameraCapabilities(
    val cameraId: String,
    val hardwareLevel: Int,
    val hardwareLevelName: String,
    
    // Format support
    val supportsRaw: Boolean,
    val supportsYuv: Boolean,
    val availableFormats: List<String>,
    val formatSizes: Map<String, List<Size>>,
    
    // Manual control support
    val supportsManualSensor: Boolean,
    val supportsManualFocus: Boolean,
    val supportsManualWhiteBalance: Boolean,
    
    // Sensor capabilities
    val isoRange: Range<Int>?,
    val exposureTimeRange: Range<Long>?,
    val focalLengths: List<Float>,
    val sensorSize: Size?,
    val activeArraySize: android.graphics.Rect?,
    
    // Processing modes
    val noiseReductionModes: List<Int>,
    val edgeModes: List<Int>,
    val tonemapModes: List<Int>,
    
    // Additional features
    val maxDigitalZoom: Float,
    val supportsAeLock: Boolean,
    val supportsAwbLock: Boolean
) {
    companion object {
        fun getHardwareLevelName(level: Int): String = when (level) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN"
        }
        
        fun getFormatName(format: Int): String = when (format) {
            android.graphics.ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
            android.graphics.ImageFormat.RAW10 -> "RAW10"
            android.graphics.ImageFormat.RAW12 -> "RAW12"
            android.graphics.ImageFormat.YUV_420_888 -> "YUV_420_888"
            android.graphics.ImageFormat.JPEG -> "JPEG"
            android.graphics.ImageFormat.NV21 -> "NV21"
            else -> "FORMAT_$format"
        }
    }
}
