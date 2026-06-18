package com.roll24.camera.zoom

/**
 * Immutable UI state for the zoom readout overlay.
 * Updated during pinch gestures and camera zoom changes.
 */
data class ZoomUiState(
    val zoomRatio: Float = 1f,
    val equivalentMm: Int = 23,
    val nearestAnchorLabel: String = "1x",
    val nearestLensName: String = "Main",
    val lensRegionLabel: String = "MAIN",
    val isNearOpticalAnchor: Boolean = true,
    val isUserZooming: Boolean = false,
    val minZoomRatio: Float = 0.6f,
    val maxZoomRatio: Float = 10f
) {
    companion object {
        fun fromZoomRatio(
            zoomRatio: Float,
            isUserZooming: Boolean = false,
            minZoomRatio: Float = 0.6f,
            maxZoomRatio: Float = 10f
        ): ZoomUiState {
            val nearest = S24UltraZoomModel.nearestAnchorForZoom(zoomRatio)
            return ZoomUiState(
                zoomRatio = zoomRatio,
                equivalentMm = S24UltraZoomModel.equivalentFocalLengthForZoom(zoomRatio).toInt(),
                nearestAnchorLabel = nearest.label,
                nearestLensName = nearest.lensName,
                lensRegionLabel = S24UltraZoomModel.lensRegionLabel(zoomRatio),
                isNearOpticalAnchor = S24UltraZoomModel.isNearOpticalAnchor(zoomRatio),
                isUserZooming = isUserZooming,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio
            )
        }
    }
}
