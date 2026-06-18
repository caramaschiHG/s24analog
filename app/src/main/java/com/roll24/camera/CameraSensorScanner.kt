package com.roll24.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import android.util.Size
import com.roll24.sensor.S24UltraSensorDossier
import kotlin.math.abs

object CameraSensorScanner {
    private const val TAG = "CameraSensorScanner"

    fun scan(context: Context): List<SensorProfile> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val candidates = mutableListOf<SensorProfile>()

        for (cameraId in manager.cameraIdList) {
            val chars = runCatching { manager.getCameraCharacteristics(cameraId) }.getOrNull() ?: continue
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            if (facing != CameraCharacteristics.LENS_FACING_BACK) continue

            if (FeatureFlags.enableRawInvestigation) {
                val probeResult = RawCapabilityProbe.probe(cameraId, manager)
                Log.d(TAG, "RAW probe: $probeResult")
            }

            // If the logical camera exposes physical sub-cameras (S24 Ultra:
            // 0.6x, 1x, 3x, 5x), enumerate ONLY the physicals to avoid showing
            // the logical "primary" camera as a duplicate of its main lens.
            val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                chars.physicalCameraIds
            } else {
                emptySet()
            }

            if (physicalIds.isEmpty()) {
                candidates += buildProfile(cameraId, null, chars)
            } else {
                for (physicalId in physicalIds) {
                    val physical = runCatching {
                        manager.getCameraCharacteristics(physicalId)
                    }.getOrNull()
                    if (physical != null) {
                        candidates += buildProfile(cameraId, physicalId, physical)
                    }
                }
            }
        }

        return candidates
            .distinctBy { it.lensLabel }
            .sortedWith(compareBy<SensorProfile> { it.focalLengthMm ?: Float.MAX_VALUE }.thenBy { it.cameraId })
    }

    private fun buildProfile(
        cameraId: String,
        physicalId: String?,
        chars: CameraCharacteristics
    ): SensorProfile {
        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val focal = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
        val aperture = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull()
        val supportsRawCapability = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        val supportsManual = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val rawSizes = streamMap?.getOutputSizes(ImageFormat.RAW_SENSOR)?.toList().orEmpty()
        val yuvSizes = streamMap?.getOutputSizes(ImageFormat.YUV_420_888)?.toList().orEmpty()

        val profile = SensorProfile(
            cameraId = cameraId,
            physicalId = physicalId,
            lensLabel = lensLabelFor(focal),
            supportsRaw = supportsRawCapability && rawSizes.isNotEmpty(),
            supportsManual = supportsManual,
            focalLengthMm = focal,
            aperture = aperture,
            isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.toString(),
            exposureRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.toString(),
            rawSizes = rawSizes.toCompactStrings(),
            yuvSizes = yuvSizes.toCompactStrings(),
            manufacturer = "Unknown",
            modelName = "Unknown",
            resolutionMp = 0f,
            pixelSizeUm = 0f,
            opticalFormat = "Unknown",
            cfaPattern = "RGGB",
            binningModes = emptyList(),
            hasOis = false,
            technologies = emptyList(),
            nativeFocalLengthMm = focal ?: 0f,
            equivalentFocalLengthMm = focal ?: 0f
        )

        val matched = S24UltraSensorDossier.byLensLabel(profile.lensLabel)
            ?: S24UltraSensorDossier.byFocalLengthMm(profile.focalLengthMm ?: 0f)

        return if (matched != null) {
            Log.d("SensorScanner", "matched ${profile.lensLabel} -> ${matched.modelName}")
            profile.copy(matchedDossierSpec = matched)
        } else {
            Log.d("SensorScanner", "no match for ${profile.lensLabel}")
            profile
        }
    }

    private fun lensLabelFor(focalLength: Float?): String {
        val focal = focalLength ?: return "1x"
        val known = listOf(
            2.2f to "0.6x",
            6.3f to "1x",
            7.9f to "3x",
            18.6f to "5x"
        )
        return known.minByOrNull { abs(it.first - focal) }?.second ?: "${"%.1f".format(focal)}mm"
    }

    private fun List<Size>.toCompactStrings(): List<String> {
        return sortedByDescending { it.width * it.height }
            .take(6)
            .map { "${it.width}x${it.height}" }
    }
}
