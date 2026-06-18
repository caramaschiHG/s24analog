package com.roll24.camera.zoom

import android.util.Log
import com.roll24.haptics.Roll24Haptics
import kotlin.math.abs

/**
 * Zoom haptics: one click per millimeter, heavier click on lens switch.
 *
 * Design:
 * - Every mm change = one tactile click (lens ring with mm detents)
 * - Lens switch (crossing optical anchor) = one deeper/grave click
 * - Boundary = firm end-stop
 * - Nothing else. No buzz, no patterns.
 */
class ZoomHapticsController(private val haptics: Roll24Haptics) {

    companion object {
        private const val TAG = "ZoomHaptics"
        private const val ANCHOR_MIN_INTERVAL_MS = 300L
        private const val BOUNDARY_MIN_INTERVAL_MS = 400L
        private const val ANCHOR_HAPTIC_RADIUS = 0.04f
    }

    private var lastMm: Int = -1
    private var lastAnchorTime = 0L
    private var lastBoundaryTime = 0L
    private var lastAnchorTriggered: Float? = null

    /**
     * Called on each zoom update during a user pinch.
     * Fires one click per mm, heavier click on lens switch.
     */
    fun onZoomChanged(
        previousZoom: Float,
        currentZoom: Float,
        currentMm: Int,
        minZoom: Float,
        maxZoom: Float,
        isUserGesture: Boolean
    ) {
        if (!isUserGesture) return
        val now = System.currentTimeMillis()

        // Boundary: end of travel
        if (currentZoom <= minZoom + 0.01f || currentZoom >= maxZoom - 0.01f) {
            if (now - lastBoundaryTime >= BOUNDARY_MIN_INTERVAL_MS) {
                haptics.zoomBoundary()
                lastBoundaryTime = now
            }
            return
        }

        // Lens switch: crossing an optical anchor
        val hitAnchor = S24UltraZoomModel.ANCHORS.find {
            abs(it.zoomRatio - currentZoom) < ANCHOR_HAPTIC_RADIUS
        }
        if (hitAnchor != null && lastAnchorTriggered != hitAnchor.zoomRatio) {
            val wasOutside = abs(hitAnchor.zoomRatio - previousZoom) >= ANCHOR_HAPTIC_RADIUS
            if (wasOutside && now - lastAnchorTime >= ANCHOR_MIN_INTERVAL_MS) {
                haptics.zoomOpticalAnchor()
                lastAnchorTime = now
                lastAnchorTriggered = hitAnchor.zoomRatio
                lastMm = currentMm
                Log.d(TAG, "lens switch → ${hitAnchor.label}")
                return
            }
        }

        // Reset anchor when moving away
        if (hitAnchor == null && lastAnchorTriggered != null) {
            val distFromLast = S24UltraZoomModel.ANCHORS.find { it.zoomRatio == lastAnchorTriggered }
                ?.let { abs(it.zoomRatio - currentZoom) } ?: 1f
            if (distFromLast > ANCHOR_HAPTIC_RADIUS * 3f) {
                lastAnchorTriggered = null
            }
        }

        // One click per mm change
        if (currentMm != lastMm && lastMm != -1) {
            haptics.zoomFineStep()
            lastMm = currentMm
        } else if (lastMm == -1) {
            lastMm = currentMm
        }
    }

    /**
     * Reset state when a new pinch gesture begins.
     */
    fun onGestureStart(currentZoom: Float, currentMm: Int) {
        lastMm = currentMm
        lastAnchorTriggered = null
    }
}
