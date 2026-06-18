package com.roll24.camera.probe

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes [S24UltraHardwareReport] to human-readable JSON.
 * Uses Android's built-in org.json (no external dependency needed).
 */
object S24UltraHardwareReportSerializer {

    fun S24UltraHardwareReport.toPrettyJson(): String {
        val root = JSONObject().apply {
            put("device_model", deviceModel)
            put("android_version", androidVersion)
            put("sdk_version", sdkVersion)
            put("total_rear_cameras", totalRearCameras)
            put("timestamp_ms", timestampMs)
            put("cameras", JSONArray().apply {
                lensReports.forEach { lens ->
                    put(lensToJson(lens))
                }
            })
        }
        return root.toString(2)
    }

    private fun lensToJson(lens: CameraLensReport): JSONObject = JSONObject().apply {
        put("camera_id", lens.cameraId)
        put("physical_camera_ids", JSONArray(lens.physicalCameraIds))
        put("lens_facing", lens.lensFacing)
        put("focal_lengths", JSONArray(lens.focalLengths))
        put("apertures", JSONArray(lens.apertures))
        put("capabilities", JSONArray(lens.capabilities))
        put("hardware_level", hardwareLevelName(lens.hardwareLevel))
        put("raw_sensor_sizes", sizesToJson(lens.rawSensorSizes))
        put("yuv_sizes", sizesToJson(lens.yuvSizes))
        put("jpeg_sizes", sizesToJson(lens.jpegSizes))
        put("supported_output_formats", JSONArray(lens.supportedOutputFormats))
        put("sensitivity_range", lens.sensitivityRange?.let {
            JSONObject().put("min", it.first).put("max", it.last)
        })
        put("exposure_time_range_ns", lens.exposureTimeRangeNs?.let {
            JSONObject().put("min", it.first).put("max", it.last)
        })
        put("af_modes", JSONArray(lens.afModes))
        put("ae_modes", JSONArray(lens.aeModes))
        put("awb_modes", JSONArray(lens.awbModes))
        put("noise_reduction_modes", JSONArray(lens.noiseReductionModes))
        put("edge_modes", JSONArray(lens.edgeModes))
        put("tonemap_modes", JSONArray(lens.tonemapModes))
        put("active_array_size", lens.activeArraySize?.let { "${it.width}x${it.height}" })
        put("pre_correction_active_array_size", lens.preCorrectionActiveArraySize?.let { "${it.width}x${it.height}" })
        put("sensor_orientation", lens.sensorOrientation)
        put("white_level", lens.whiteLevel)
        put("black_level_pattern", lens.blackLevelPattern?.let { JSONArray(it.toList()) })
        put("cfa_arrangement", lens.cfaArrangement)
        put("lens_shading_map_available", lens.lensShadingMapAvailable)
        put("max_digital_zoom", lens.maxDigitalZoom)
        put("optical_stabilization", lens.opticalStabilization)
        put("video_stabilization", lens.videoStabilization)
    }

    private fun sizesToJson(sizes: List<android.util.Size>): JSONArray {
        return JSONArray().apply {
            sizes.forEach { put("${it.width}x${it.height}") }
        }
    }

    private fun hardwareLevelName(level: Int): String = when (level) {
        0 -> "LIMITED"
        1 -> "FULL"
        2 -> "LEGACY"
        3 -> "LEVEL_3"
        4 -> "EXTERNAL"
        else -> "UNKNOWN($level)"
    }
}
