package com.roll24.camera.zoom

import kotlin.math.abs

/**
 * Fixed optical anchor point for the S24 Ultra lens system.
 * Each anchor corresponds to a physical sub-camera or a known optical transition.
 */
data class ZoomAnchor(
    val zoomRatio: Float,
    val equivalentFocalLengthMm: Float,
    val nativeFocalLengthMm: Float,
    val label: String,
    val lensName: String
)

/**
 * S24 Ultra zoom model: known physical lens anchors and interpolation logic
 * for computing 35mm-equivalent focal lengths across the entire zoom range.
 */
object S24UltraZoomModel {

    val ANCHORS: List<ZoomAnchor> = listOf(
        ZoomAnchor(
            zoomRatio = 0.6f,
            equivalentFocalLengthMm = 13f,
            nativeFocalLengthMm = 2.2f,
            label = "0.6x",
            lensName = "Ultra Wide"
        ),
        ZoomAnchor(
            zoomRatio = 1f,
            equivalentFocalLengthMm = 23f,
            nativeFocalLengthMm = 6.3f,
            label = "1x",
            lensName = "Main"
        ),
        ZoomAnchor(
            zoomRatio = 3f,
            equivalentFocalLengthMm = 69f,
            nativeFocalLengthMm = 7.9f,
            label = "3x",
            lensName = "Tele"
        ),
        ZoomAnchor(
            zoomRatio = 5f,
            equivalentFocalLengthMm = 115f,
            nativeFocalLengthMm = 18.6f,
            label = "5x",
            lensName = "Periscope"
        )
    )

    /** Threshold to consider the current zoom "near" an optical anchor. */
    private const val ANCHOR_PROXIMITY_THRESHOLD = 0.08f

    /**
     * Computes the 35mm-equivalent focal length for any zoom ratio.
     * Between anchors: linear interpolation.
     * Above 5x: extrapolation based on the 5x anchor proportionally.
     */
    fun equivalentFocalLengthForZoom(zoomRatio: Float): Float {
        val clamped = zoomRatio.coerceAtLeast(ANCHORS.first().zoomRatio)

        // Below first anchor
        if (clamped <= ANCHORS.first().zoomRatio) {
            return ANCHORS.first().equivalentFocalLengthMm
        }

        // Between anchors: interpolate
        for (i in 0 until ANCHORS.size - 1) {
            val lower = ANCHORS[i]
            val upper = ANCHORS[i + 1]
            if (clamped in lower.zoomRatio..upper.zoomRatio) {
                val t = (clamped - lower.zoomRatio) / (upper.zoomRatio - lower.zoomRatio)
                return lower.equivalentFocalLengthMm +
                    t * (upper.equivalentFocalLengthMm - lower.equivalentFocalLengthMm)
            }
        }

        // Above last anchor: extrapolate from 5x proportionally
        val lastAnchor = ANCHORS.last()
        return lastAnchor.equivalentFocalLengthMm *
            (clamped / lastAnchor.zoomRatio)
    }

    /**
     * Returns the nearest optical anchor for the given zoom ratio.
     */
    fun nearestAnchorForZoom(zoomRatio: Float): ZoomAnchor {
        return ANCHORS.minByOrNull { abs(it.zoomRatio - zoomRatio) } ?: ANCHORS[1]
    }

    /**
     * Returns true if the zoom ratio is close enough to a physical optical anchor
     * that the system is likely using that sub-camera natively (no digital crop).
     */
    fun isNearOpticalAnchor(zoomRatio: Float): Boolean {
        return ANCHORS.any { abs(it.zoomRatio - zoomRatio) <= ANCHOR_PROXIMITY_THRESHOLD }
    }

    /**
     * Format zoom ratio for display: "2.3x" or "1x" for integers.
     */
    fun formatZoomRatio(zoomRatio: Float): String {
        return if (zoomRatio == zoomRatio.toInt().toFloat() || abs(zoomRatio - zoomRatio.toInt()) < 0.05f) {
            "${zoomRatio.toInt()}x"
        } else {
            "${"%.1f".format(zoomRatio)}x"
        }
    }

    /**
     * Format equivalent focal length for display: "53mm".
     */
    fun formatEquivalentMm(zoomRatio: Float): String {
        val mm = equivalentFocalLengthForZoom(zoomRatio).toInt()
        return "${mm}mm"
    }

    /**
     * Returns a lens region label for UI display.
     * "UW", "MAIN", "TELE", "PERISCOPE", or "DIGITAL" for zoom beyond 5x.
     */
    fun lensRegionLabel(zoomRatio: Float): String {
        if (zoomRatio > 5f + ANCHOR_PROXIMITY_THRESHOLD) return "DIGITAL"
        val nearest = nearestAnchorForZoom(zoomRatio)
        return when (nearest.label) {
            "0.6x" -> "UW"
            "1x" -> "MAIN"
            "3x" -> "TELE"
            "5x" -> "PERISCOPE"
            else -> "DIGITAL"
        }
    }
}
