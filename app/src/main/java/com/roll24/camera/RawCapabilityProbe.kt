package com.roll24.camera

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size

/**
 * Result of a RAW capability probe for a single camera.
 *
 * @property cameraId The logical or physical camera id that was probed.
 * @property isRawSensorAvailable True when both the RAW capability is advertised
 *           and at least one [ImageFormat.RAW_SENSOR] output size is reported.
 * @property rawSizes List of reported RAW_SENSOR output sizes, e.g. "4080x3072".
 * @property timestamp Time at which the probe completed (System.currentTimeMillis).
 * @property lastError Human-readable error message when the probe failed, or null.
 */
data class RawProbeResult(
    val cameraId: String,
    val isRawSensorAvailable: Boolean,
    val rawSizes: List<String>,
    val timestamp: Long,
    val lastError: String?
)

/**
 * Investigates Camera2 RAW_SENSOR capabilities for a given camera id.
 *
 * This probe is intentionally read-only: it queries [CameraCharacteristics] and
 * never opens a capture session. Actual RAW capture pipelines are complex and
 * can crash on devices that advertise RAW support but do not expose a usable
 * Bayer stream (notably many Samsung devices). Keeping this probe read-only
 * satisfies the investigation requirement while avoiding crashes/ANRs.
 */
object RawCapabilityProbe {
    private const val TAG = "RawCapabilityProbe"

    fun probe(cameraId: String, cameraManager: CameraManager): RawProbeResult {
        val start = System.currentTimeMillis()
        return try {
            val chars = cameraManager.getCameraCharacteristics(cameraId)
            val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?: intArrayOf()
            val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val hasRawCapability = capabilities.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
            )
            val rawSizeList = streamMap
                ?.getOutputSizes(ImageFormat.RAW_SENSOR)
                ?.toList()
                .orEmpty()
            val rawSizes = rawSizeList.toCompactStrings()
            val isAvailable = hasRawCapability && rawSizes.isNotEmpty()

            Log.d(TAG, "cameraId=$cameraId rawAvailable=$isAvailable " +
                "hasCapability=$hasRawCapability rawSizes=$rawSizes")

            RawProbeResult(
                cameraId = cameraId,
                isRawSensorAvailable = isAvailable,
                rawSizes = rawSizes,
                timestamp = start,
                lastError = null
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to probe RAW for cameraId=$cameraId", e)
            RawProbeResult(
                cameraId = cameraId,
                isRawSensorAvailable = false,
                rawSizes = emptyList(),
                timestamp = start,
                lastError = e.message ?: e.javaClass.simpleName
            )
        }
    }

    private fun List<Size>.toCompactStrings(): List<String> {
        return sortedByDescending { it.width * it.height }
            .take(6)
            .map { "${it.width}x${it.height}" }
    }
}
