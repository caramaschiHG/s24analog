package com.roll24.camera.probe

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Log
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Probes all rear-facing cameras on the S24 Ultra and collects a comprehensive
 * hardware report. Designed for debug/diagnostics — the report can be exported
 * as JSON for analysis.
 */
object S24UltraHardwareProbe {

    private const val TAG = "HardwareProbe"

    /**
     * Generate a full hardware report for all rear-facing cameras.
     */
    suspend fun generateReport(context: Context): S24UltraHardwareReport =
        withContext(Dispatchers.IO) {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val rearCameraIds = manager.cameraIdList.filter { id ->
                val chars = manager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }

            Log.d(TAG, "Found ${rearCameraIds.size} rear cameras: $rearCameraIds")

            val lensReports = rearCameraIds.map { id ->
                probeSingleCamera(manager, id)
            }

            S24UltraHardwareReport(
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = Build.VERSION.RELEASE,
                sdkVersion = Build.VERSION.SDK_INT,
                totalRearCameras = rearCameraIds.size,
                lensReports = lensReports
            )
        }

    private fun probeSingleCamera(manager: CameraManager, cameraId: String): CameraLensReport {
        val chars = manager.getCameraCharacteristics(cameraId)
        val configMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            chars.physicalCameraIds.toList()
        } else {
            emptyList()
        }

        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.toList() ?: emptyList()
        val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
            ?.toList() ?: emptyList()
        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.toList() ?: emptyList()
        val hwLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            ?: CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

        val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)?.toList() ?: emptyList()
        val yuvSizes = configMap?.getOutputSizes(ImageFormat.YUV_420_888)?.toList() ?: emptyList()
        val jpegSizes = configMap?.getOutputSizes(ImageFormat.JPEG)?.toList() ?: emptyList()
        val outputFormats = configMap?.outputFormats?.toList() ?: emptyList()

        val sensitivityRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            ?.let { IntRange(it.lower, it.upper) }
        val exposureRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            ?.let { LongRange(it.lower, it.upper) }

        val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            ?.toList() ?: emptyList()
        val aeModes = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            ?.toList() ?: emptyList()
        val awbModes = chars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
            ?.toList() ?: emptyList()
        val nrModes = chars.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)
            ?.toList() ?: emptyList()
        val edgeModes = chars.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)
            ?.toList() ?: emptyList()
        val tonemapModes = chars.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES)
            ?.toList() ?: emptyList()

        val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)?.let {
            Size(it.width(), it.height())
        }
        val preCorrectionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            chars.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE)?.let {
                Size(it.width(), it.height())
            }
        } else null

        val orientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val whiteLevel = chars.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL)
        val blackLevelPattern = chars.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN)
            ?.let { pattern ->
                IntArray(4) { i -> pattern.getOffsetForIndex(i / 2, i % 2) }
            }
        val cfa = chars.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
        val lensShadingAvailable = chars.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES)
            ?.any { it != CameraMetadata.STATISTICS_LENS_SHADING_MAP_MODE_OFF } ?: false

        val maxDigitalZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)

        val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?.toList() ?: emptyList()
        val hasOis = oisModes.any { it != CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF }

        val vstabModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            ?.toList() ?: emptyList()
        val hasVstab = vstabModes.any { it != CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF }

        Log.d(TAG, "Camera $cameraId: focal=$focalLengths apertures=$apertures RAW=${rawSizes.size} YUV=${yuvSizes.size}")

        return CameraLensReport(
            cameraId = cameraId,
            physicalCameraIds = physicalIds,
            lensFacing = CameraCharacteristics.LENS_FACING_BACK,
            focalLengths = focalLengths,
            apertures = apertures,
            capabilities = capabilities,
            hardwareLevel = hwLevel,
            rawSensorSizes = rawSizes,
            yuvSizes = yuvSizes,
            jpegSizes = jpegSizes,
            supportedOutputFormats = outputFormats,
            sensitivityRange = sensitivityRange,
            exposureTimeRangeNs = exposureRange,
            afModes = afModes,
            aeModes = aeModes,
            awbModes = awbModes,
            noiseReductionModes = nrModes,
            edgeModes = edgeModes,
            tonemapModes = tonemapModes,
            activeArraySize = activeArray,
            preCorrectionActiveArraySize = preCorrectionArray,
            sensorOrientation = orientation,
            whiteLevel = whiteLevel,
            blackLevelPattern = blackLevelPattern,
            cfaArrangement = cfa,
            lensShadingMapAvailable = lensShadingAvailable,
            maxDigitalZoom = maxDigitalZoom,
            opticalStabilization = hasOis,
            videoStabilization = hasVstab
        )
    }
}
