package com.roll24.film.processors

import android.graphics.Bitmap
import android.graphics.Color
import com.roll24.film.FilmProfile
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Parameters for a single channel of an H&D (Hurter-Driffield) style curve.
 *
 * @param toe shadow compression strength (0..1). Higher values deepen the toe.
 * @param shoulder highlight rolloff strength (0..1). Higher values soften the shoulder.
 * @param gamma mid-tone contrast exponent. 1.0 is linear, >1 darkens mids.
 * @param dMin minimum output density in [0,1]. Raises the black floor.
 * @param dMax maximum output density in [0,1]. Lowers the white ceiling.
 */
data class HdChannelParams(
    val toe: Float = 0.1f,
    val shoulder: Float = 0.18f,
    val gamma: Float = 1f,
    val dMin: Float = 0f,
    val dMax: Float = 1f
) {
    init {
        require(toe in 0f..1f) { "toe must be in [0,1]" }
        require(shoulder in 0f..1f) { "shoulder must be in [0,1]" }
        require(dMin in 0f..1f) { "dMin must be in [0,1]" }
        require(dMax in 0f..1f) { "dMax must be in [0,1]" }
        require(dMax >= dMin) { "dMax must be >= dMin" }
    }
}

/**
 * Per-channel H&D curve parameters.
 *
 * [base] is used for every channel unless a channel-specific override is provided.
 */
data class HdCurveParams(
    val base: HdChannelParams,
    val red: HdChannelParams? = null,
    val green: HdChannelParams? = null,
    val blue: HdChannelParams? = null
) {
    companion object {
        /**
         * Derives sensible default H&D parameters from a [FilmProfile].
         *
         * This mapping is tuned to approximate the previous inline
         * `applyAnalogResponse()` behaviour for the existing `NEUTRAL` profile
         * while exposing the new toe/shoulder/gamma controls.
         */
        fun defaultFromProfile(profile: FilmProfile): HdCurveParams {
            val shoulder = (0.18f + profile.highlightCompression * 0.28f).coerceIn(0f, 0.55f)
            val toe = (0.10f + profile.blackPoint * 1.8f).coerceIn(0f, 0.38f)
            val gamma = 1f + profile.contrast * 0.5f
            val dMin = (profile.blackPoint * 0.25f).coerceIn(0f, 0.1f)
            val dMax = (1f - shoulder * 0.15f).coerceIn(0.9f, 1f)
            val channel = HdChannelParams(toe, shoulder, gamma, dMin, dMax)
            return HdCurveParams(base = channel)
        }
    }
}

/**
 * Applies a parametric per-channel H&D curve to a bitmap.
 *
 * The curve is a sigmoid remapping of the input linear value:
 *
 *     output = dMin + (dMax - dMin) / (1 + exp(-k * (input - x0)))
 *
 * where [k] and [x0] are derived from toe/shoulder to produce a film-like
 * toe/shoulder shape, followed by a gamma power function.
 */
class HdCurveProcessor {

    /**
     * Processes [bitmap] in place using the supplied [params].
     */
    fun process(bitmap: Bitmap, params: HdCurveParams): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val redCurve = buildLut(params.red ?: params.base)
        val greenCurve = buildLut(params.green ?: params.base)
        val blueCurve = buildLut(params.blue ?: params.base)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = Color.alpha(pixel)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            pixels[i] = Color.argb(a, redCurve[r], greenCurve[g], blueCurve[b])
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun buildLut(params: HdChannelParams): IntArray {
        val lut = IntArray(256)
        val range = params.dMax - params.dMin
        // Steepness: gentle default, stronger with more toe+shoulder.
        val k = 4f + 8f * (params.toe + params.shoulder)
        // Pivot: move toward shadows for toe, toward highlights for shoulder.
        val x0 = 0.5f + (params.shoulder - params.toe) * 0.25f

        for (i in 0..255) {
            val x = i / 255f
            val sigmoid = params.dMin + range / (1f + exp(-k * (x - x0)))
            val gammaCorrected = sigmoid.pow(1f / params.gamma.coerceAtLeast(0.1f))
            lut[i] = (gammaCorrected * 255f).roundToInt().coerceIn(0, 255)
        }
        return lut
    }
}
