package com.roll24.film.pipeline

import com.roll24.film.processors.HdChannelParams
import com.roll24.film.processors.HdCurveParams
import kotlin.math.exp
import kotlin.math.pow

class NegativeExposureProcessor {
    fun process(buffer: FilmPixelBuffer, exposureStops: Float, blackAndWhite: Boolean) {
        val multiplier = 2f.pow(exposureStops.coerceIn(-4f, 4f))
        for (i in 0 until buffer.size) {
            if (blackAndWhite) {
                // Panchromatic response with slightly stronger green sensitivity.
                val exposure = (buffer.r[i] * 0.24f + buffer.g[i] * 0.68f + buffer.b[i] * 0.08f) * multiplier
                buffer.r[i] = exposure
                buffer.g[i] = exposure
                buffer.b[i] = exposure
            } else {
                buffer.r[i] = (buffer.r[i] * multiplier).coerceAtLeast(0f)
                buffer.g[i] = (buffer.g[i] * multiplier).coerceAtLeast(0f)
                buffer.b[i] = (buffer.b[i] * multiplier).coerceAtLeast(0f)
            }
        }
    }
}

/** H&D is the primary tone curve. Values remain linear and may exceed 1 before scanning. */
class HdDensityProcessor {
    fun process(buffer: FilmPixelBuffer, params: HdCurveParams) {
        val red = params.red ?: params.base
        val green = params.green ?: params.base
        val blue = params.blue ?: params.base
        for (i in 0 until buffer.size) {
            buffer.r[i] = curve(buffer.r[i], red)
            buffer.g[i] = curve(buffer.g[i], green)
            buffer.b[i] = curve(buffer.b[i], blue)
        }
    }

    private fun curve(exposure: Float, params: HdChannelParams): Float {
        val x = exposure.coerceAtLeast(0f)
        val toeScale = 0.018f + params.toe * 0.16f
        val toe = x / (x + toeScale)
        val gamma = toe.pow(params.gamma.coerceIn(0.35f, 2.5f))
        val shoulderStrength = 0.35f + params.shoulder * 3.5f
        val shouldered = 1f - exp(-gamma * shoulderStrength)
        val normalization = (1f - exp(-shoulderStrength)).coerceAtLeast(0.001f)
        val normalized = shouldered / normalization
        return params.dMin + (params.dMax - params.dMin) * normalized
    }
}

/** Luminance-domain shoulder with a soft knee; RGB is scaled together to preserve hue. */
class HighlightRolloffProcessor {
    fun process(buffer: FilmPixelBuffer, amount: Float) {
        val strength = amount.coerceIn(0f, 1.5f)
        if (strength <= 0f) return
        val knee = (0.82f - strength * 0.14f).coerceIn(0.58f, 0.82f)
        for (i in 0 until buffer.size) {
            val luminance = buffer.luminance(i)
            if (luminance <= knee || luminance <= 0f) continue
            val excess = luminance - knee
            val span = (1f - knee).coerceAtLeast(0.05f)
            val rolled = knee + excess / (1f + excess * (1.5f + strength * 2.5f) / span)
            val scale = rolled / luminance
            buffer.r[i] *= scale
            buffer.g[i] *= scale
            buffer.b[i] *= scale
        }
    }
}
