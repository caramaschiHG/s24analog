package com.roll24.camera.zoom

/**
 * Formats zoom information for display in the viewfinder overlay.
 * All outputs are designed for instant readability during pinch gestures.
 */
object ZoomFocalLengthFormatter {

    /**
     * Primary display: "53mm"
     * Rounded to integer for clean display without flicker.
     */
    fun formatMm(zoomRatio: Float): String {
        val mm = S24UltraZoomModel.equivalentFocalLengthForZoom(zoomRatio).toInt()
        return "${mm}mm"
    }

    /**
     * Secondary display: "2.3x" or "1x" for integer ratios.
     */
    fun formatRatio(zoomRatio: Float): String {
        return S24UltraZoomModel.formatZoomRatio(zoomRatio)
    }

    /**
     * Compact lens region label for chip display.
     * "UW", "MAIN", "TELE", "PERISCOPE", "DIGITAL"
     */
    fun formatLensRegion(zoomRatio: Float): String {
        return S24UltraZoomModel.lensRegionLabel(zoomRatio)
    }

    /**
     * Full formatted line: "69mm / 3x TELE"
     * Used for logging and debug.
     */
    fun formatFull(zoomRatio: Float): String {
        val mm = formatMm(zoomRatio)
        val ratio = formatRatio(zoomRatio)
        val region = formatLensRegion(zoomRatio)
        return "$mm / $ratio $region"
    }
}
