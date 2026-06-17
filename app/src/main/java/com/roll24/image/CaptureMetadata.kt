package com.roll24.image

import androidx.exifinterface.media.ExifInterface
import org.json.JSONObject

/**
 * Film-relevant EXIF metadata extracted from a captured image.
 *
 * Only the fields needed for film emulation are stored (ISO, exposure time,
 * focal length, aperture, white balance mode and capture date/time).
 */
data class CaptureMetadata(
    val iso: Int?,
    val exposureTime: String?,
    val focalLengthMm: Float?,
    val aperture: Float?,
    val whiteBalanceMode: String?,
    val dateTimeOriginal: String?,
    val sourceWidth: Int? = null,
    val sourceHeight: Int? = null
) {
    fun toJson(): String = JSONObject().apply {
        put("iso", iso ?: JSONObject.NULL)
        put("exposureTime", exposureTime ?: JSONObject.NULL)
        put("focalLengthMm", focalLengthMm?.toDouble() ?: JSONObject.NULL)
        put("aperture", aperture?.toDouble() ?: JSONObject.NULL)
        put("whiteBalanceMode", whiteBalanceMode ?: JSONObject.NULL)
        put("dateTimeOriginal", dateTimeOriginal ?: JSONObject.NULL)
        put("sourceWidth", sourceWidth ?: JSONObject.NULL)
        put("sourceHeight", sourceHeight ?: JSONObject.NULL)
    }.toString()

    companion object {
        fun fromJson(json: String): CaptureMetadata? = try {
            val obj = JSONObject(json)
            CaptureMetadata(
                iso = if (obj.isNull("iso")) null else obj.getInt("iso"),
                exposureTime = if (obj.isNull("exposureTime")) null else obj.getString("exposureTime"),
                focalLengthMm = if (obj.isNull("focalLengthMm")) null else obj.getDouble("focalLengthMm").toFloat(),
                aperture = if (obj.isNull("aperture")) null else obj.getDouble("aperture").toFloat(),
                whiteBalanceMode = if (obj.isNull("whiteBalanceMode")) null else obj.getString("whiteBalanceMode"),
                dateTimeOriginal = if (obj.isNull("dateTimeOriginal")) null else obj.getString("dateTimeOriginal"),
                sourceWidth = if (obj.isNull("sourceWidth")) null else obj.getInt("sourceWidth"),
                sourceHeight = if (obj.isNull("sourceHeight")) null else obj.getInt("sourceHeight")
            )
        } catch (e: Exception) {
            null
        }

        fun fromExifInterface(
            exif: ExifInterface,
            width: Int? = null,
            height: Int? = null
        ): CaptureMetadata = CaptureMetadata(
            iso = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED, 0).takeIf { it > 0 },
            exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
            focalLengthMm = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                .takeIf { it > 0.0 }?.toFloat(),
            aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                .takeIf { it > 0.0 }?.toFloat(),
            whiteBalanceMode = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE),
            dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
            sourceWidth = width,
            sourceHeight = height
        )
    }
}
