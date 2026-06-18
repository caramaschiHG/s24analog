package com.roll24.image

import android.util.Log

/**
 * Sanitizes EXIF/TIFF orientation values and converts rotation degrees.
 *
 * EXIF orientation values 1-8 are valid. Value 9+ is invalid.
 * The S24 Ultra DngCreator has been observed to produce orientation=9.
 */
object OrientationUtils {
    private const val TAG = "OrientationUtils"

    /** Valid EXIF orientations are 1..8. Anything else → NORMAL (1). */
    fun sanitizeExifOrientation(value: Int?): Int {
        if (value == null) return 1
        return if (value in 1..8) {
            value
        } else {
            Log.w(TAG, "Invalid EXIF orientation=$value, sanitizing to NORMAL (1)")
            1
        }
    }

    /**
     * Convert CameraX rotationDegrees to an EXIF orientation value.
     * CameraX reports rotation needed to display the image upright.
     */
    fun rotationDegreesToExifOrientation(rotationDegrees: Int): Int = when (rotationDegrees) {
        90 -> 6   // Rotate CW 90
        180 -> 3  // Rotate 180
        270 -> 8  // Rotate CW 270 (= CCW 90)
        else -> 1 // Normal
    }

    /**
     * Sanitize rotation degrees for Bitmap rotation.
     * Only 0, 90, 180, 270 are valid.
     */
    fun sanitizeRotationDegrees(degrees: Int): Int {
        val normalized = ((degrees % 360) + 360) % 360
        return when (normalized) {
            0, 90, 180, 270 -> normalized
            else -> {
                Log.w(TAG, "Non-standard rotationDegrees=$degrees, rounding to nearest 90")
                when {
                    normalized < 45 -> 0
                    normalized < 135 -> 90
                    normalized < 225 -> 180
                    normalized < 315 -> 270
                    else -> 0
                }
            }
        }
    }
}
