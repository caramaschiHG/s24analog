package com.roll24.camera.zoom

import android.util.Log
import androidx.camera.core.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Controls zoom on the S24 Ultra via CameraX.
 * Manages smooth zoom application, clamping, anchor magnetism, and state emission.
 *
 * This controller is the single source of truth for the current zoom ratio
 * applied to the camera. The UI observes [zoomState] for display updates.
 *
 * Premium behaviors:
 * - Low-pass filter smoothing to avoid jitter from finger tremor
 * - Magnetic snap zones around optical anchors (0.6x, 1x, 3x, 5x)
 * - Dead zone on gesture start to prevent accidental zoom
 * - Smooth interpolation between values sent to camera hardware
 */
class S24UltraZoomController {

    companion object {
        private const val TAG = "ZoomController"

        /** Maximum creative zoom ratio for Roll24. */
        const val ROLL24_MAX_CREATIVE_ZOOM = 10f

        /**
         * Low-pass filter coefficient.
         * 0.0 = no filtering (raw), 1.0 = frozen (never updates).
         * 0.35 gives premium smoothness without noticeable lag.
         */
        private const val SMOOTHING_FACTOR = 0.35f

        /**
         * Dead zone: minimum cumulative gesture delta before zoom starts responding.
         * Prevents accidental zoom from small finger adjustments during tap-to-focus.
         */
        private const val DEAD_ZONE_THRESHOLD = 0.018f

        /**
         * Magnetic attraction zone radius around each optical anchor.
         * Within this zone the zoom subtly pulls toward the anchor.
         */
        private const val MAGNETIC_ZONE_RADIUS = 0.06f

        /**
         * Magnetic pull strength (0 = none, 1 = instant snap).
         * 0.25 gives a gentle "detent valley" feel.
         */
        private const val MAGNETIC_STRENGTH = 0.25f
    }

    private var camera: Camera? = null
    private var hardwareMinZoom: Float = 0.6f
    private var hardwareMaxZoom: Float = 20f

    private val _zoomState = MutableStateFlow(ZoomUiState())
    val zoomState: StateFlow<ZoomUiState> = _zoomState.asStateFlow()

    /** The effective max zoom shown to the user. */
    val effectiveMaxZoom: Float
        get() = hardwareMaxZoom.coerceAtMost(ROLL24_MAX_CREATIVE_ZOOM)

    // ─── Smoothing state ─────────────────────────────────────────────────────────

    /** Smoothed ratio (what the camera actually targets). */
    private var smoothedRatio: Float = 1f

    /** Raw accumulated ratio from gesture (before filtering). */
    private var rawGestureRatio: Float = 1f

    /** Gesture start ratio for dead-zone calculation. */
    private var gestureStartRatio: Float = 1f

    /** Whether the dead zone has been cleared in the current gesture. */
    private var deadZoneCleared: Boolean = false

    /**
     * Attach to a camera instance. Reads min/max zoom from CameraInfo.
     */
    fun attachCamera(camera: Camera) {
        this.camera = camera
        val zoomState = camera.cameraInfo.zoomState.value
        if (zoomState != null) {
            hardwareMinZoom = zoomState.minZoomRatio
            hardwareMaxZoom = zoomState.maxZoomRatio
            Log.d(TAG, "Attached: min=$hardwareMinZoom max=$hardwareMaxZoom effective=$effectiveMaxZoom")
        }
        val currentRatio = zoomState?.zoomRatio ?: 1f
        smoothedRatio = currentRatio
        rawGestureRatio = currentRatio
        updateState(currentRatio, isUserZooming = false)
    }

    fun detachCamera() {
        camera = null
    }

    /**
     * Notify the controller that a new pinch gesture has started.
     * Resets dead-zone and smoothing accumulators.
     */
    fun onGestureStart() {
        gestureStartRatio = smoothedRatio
        rawGestureRatio = smoothedRatio
        deadZoneCleared = false
    }

    /**
     * Process a raw pinch gesture zoom factor.
     * Applies dead-zone → smoothing → magnetism → clamping → camera.
     * Returns the actual ratio applied (or null if dead zone not yet cleared).
     */
    fun applyGestureZoom(rawMultiplier: Float): Float? {
        // Accumulate raw gesture target
        rawGestureRatio *= rawMultiplier

        // Dead zone check: require minimum movement before responding
        if (!deadZoneCleared) {
            val delta = abs(rawGestureRatio - gestureStartRatio)
            if (delta < DEAD_ZONE_THRESHOLD) {
                return null // Still in dead zone, don't move
            }
            deadZoneCleared = true
        }

        // Low-pass filter: smooth the transition
        val target = rawGestureRatio
        smoothedRatio = smoothedRatio + (1f - SMOOTHING_FACTOR) * (target - smoothedRatio)

        // Magnetic attraction toward nearest anchor
        val magnetized = applyMagnetism(smoothedRatio)

        // Clamp and apply
        return applyZoom(magnetized, isUserGesture = true)
    }

    /**
     * Apply zoom ratio directly (for programmatic changes and anchor snaps).
     * Bypasses gesture smoothing.
     */
    fun applyZoom(requestedRatio: Float, isUserGesture: Boolean = true): Float {
        val clamped = requestedRatio.coerceIn(hardwareMinZoom, effectiveMaxZoom)
        val activeCamera = camera
        if (activeCamera != null) {
            runCatching {
                activeCamera.cameraControl.setZoomRatio(clamped)
            }.onFailure {
                Log.w(TAG, "setZoomRatio($clamped) failed", it)
            }
        }
        if (isUserGesture) {
            smoothedRatio = clamped
            rawGestureRatio = clamped
        }
        updateState(clamped, isUserZooming = isUserGesture)
        return clamped
    }

    /**
     * Set zoom to a specific anchor (e.g., from lens button tap).
     */
    fun snapToAnchor(anchor: ZoomAnchor) {
        smoothedRatio = anchor.zoomRatio
        rawGestureRatio = anchor.zoomRatio
        applyZoom(anchor.zoomRatio, isUserGesture = false)
    }

    /**
     * Mark that the user has stopped zooming (fingers lifted).
     */
    fun endUserZoom() {
        _zoomState.value = _zoomState.value.copy(isUserZooming = false)
    }

    /**
     * Get the current zoom ratio.
     */
    val currentZoomRatio: Float
        get() = _zoomState.value.zoomRatio

    // ─── Private ─────────────────────────────────────────────────────────────────

    /**
     * Apply magnetic pull toward the nearest optical anchor if within range.
     * Creates a subtle "detent valley" that makes anchors feel sticky.
     */
    private fun applyMagnetism(ratio: Float): Float {
        val nearestAnchor = S24UltraZoomModel.ANCHORS.minByOrNull { abs(it.zoomRatio - ratio) }
            ?: return ratio

        val distance = abs(ratio - nearestAnchor.zoomRatio)
        if (distance > MAGNETIC_ZONE_RADIUS || distance < 0.001f) {
            return ratio
        }

        // Pull strength increases as we get closer (quadratic falloff)
        val normalizedDist = distance / MAGNETIC_ZONE_RADIUS
        val pull = MAGNETIC_STRENGTH * (1f - normalizedDist) * (1f - normalizedDist)

        // Interpolate toward anchor
        return ratio + (nearestAnchor.zoomRatio - ratio) * pull
    }

    private fun updateState(zoomRatio: Float, isUserZooming: Boolean) {
        _zoomState.value = ZoomUiState.fromZoomRatio(
            zoomRatio = zoomRatio,
            isUserZooming = isUserZooming,
            minZoomRatio = hardwareMinZoom,
            maxZoomRatio = effectiveMaxZoom
        )
    }
}
