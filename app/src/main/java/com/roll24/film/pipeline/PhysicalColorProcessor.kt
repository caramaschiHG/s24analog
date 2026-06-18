package com.roll24.film.pipeline

import com.roll24.film.FilmProfile
import com.roll24.film.FilmType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PhysicalColorProcessor {
    fun process(buffer: FilmPixelBuffer, profile: FilmProfile, intensity: Float) {
        if (profile.blackAndWhite) {
            convertBlackAndWhite(buffer, profile)
            return
        }
        val matrix = matrixFor(profile)
        val saturation = (1f + profile.saturation * intensity).coerceIn(0.25f, 1.8f)
        for (i in 0 until buffer.size) {
            val sourceR = buffer.r[i]
            val sourceG = buffer.g[i]
            val sourceB = buffer.b[i]
            var r = matrix[0] * sourceR + matrix[1] * sourceG + matrix[2] * sourceB
            var g = matrix[3] * sourceR + matrix[4] * sourceG + matrix[5] * sourceB
            var b = matrix[6] * sourceR + matrix[7] * sourceG + matrix[8] * sourceB

            val luminance = r * 0.2126f + g * 0.7152f + b * 0.0722f
            r = luminance + (r - luminance) * saturation
            g = luminance + (g - luminance) * saturation
            b = luminance + (b - luminance) * saturation

            // Density-dependent split tone: cooler shadows, warmer highlights.
            val shadowWeight = ((0.48f - luminance) / 0.48f).coerceIn(0f, 1f)
            val highlightWeight = ((luminance - 0.52f) / 0.48f).coerceIn(0f, 1f)
            val warmth = profile.warmth * intensity
            val tint = profile.tint * intensity
            r *= 1f + highlightWeight * warmth * 0.12f + shadowWeight * tint * 0.035f
            g *= 1f + tint * 0.055f
            b *= 1f - highlightWeight * warmth * 0.10f + shadowWeight * 0.025f

            // Broad hue response rather than channel offsets.
            val maxChannel = max(r, max(g, b))
            val minChannel = min(r, min(g, b))
            val chroma = (maxChannel - minChannel).coerceAtLeast(0f)
            if (chroma > 0.01f) {
                when (maxChannel) {
                    r -> {
                        r *= 1f + profile.saturation.coerceAtLeast(0f) * 0.08f
                        g *= 1f + profile.warmth * 0.025f
                    }
                    g -> g *= 1f - profile.tint * 0.05f
                    else -> b *= 1f - profile.warmth * 0.04f
                }
            }

            // Skin-like orange hues retain a portion of the gentler matrix-only response.
            val skin = skinWeight(sourceR, sourceG, sourceB)
            if (skin > 0f) {
                val protect = skin * 0.35f
                r += (sourceR - r) * protect
                g += (sourceG - g) * protect
                b += (sourceB - b) * protect
            }
            buffer.r[i] = r.coerceAtLeast(0f)
            buffer.g[i] = g.coerceAtLeast(0f)
            buffer.b[i] = b.coerceAtLeast(0f)
        }
    }

    private fun convertBlackAndWhite(buffer: FilmPixelBuffer, profile: FilmProfile) {
        val redWeight = if (profile.filmStockId.contains("tri", ignoreCase = true)) 0.32f else 0.27f
        val greenWeight = if (redWeight > 0.3f) 0.60f else 0.66f
        val blueWeight = 1f - redWeight - greenWeight
        for (i in 0 until buffer.size) {
            val value = buffer.r[i] * redWeight + buffer.g[i] * greenWeight + buffer.b[i] * blueWeight
            buffer.r[i] = value
            buffer.g[i] = value
            buffer.b[i] = value
        }
    }

    private fun skinWeight(r: Float, g: Float, b: Float): Float {
        if (r <= g || g <= b || r <= 0.08f) return 0f
        val rg = r / g.coerceAtLeast(0.001f)
        val gb = g / b.coerceAtLeast(0.001f)
        if (rg !in 1.04f..1.65f || gb !in 1.02f..1.8f) return 0f
        return (1f - abs(rg - 1.24f) / 0.4f).coerceIn(0f, 1f)
    }

    private fun matrixFor(profile: FilmProfile): FloatArray = when (profile.filmStockId) {
        "portra_400" -> matrix(1.035f, -0.020f, -0.015f, -0.010f, 1.015f, -0.005f, 0.005f, -0.025f, 1.020f)
        "ektar_100" -> matrix(1.090f, -0.050f, -0.040f, -0.025f, 1.065f, -0.040f, -0.010f, -0.045f, 1.055f)
        "pro_400h" -> matrix(0.990f, -0.010f, 0.020f, -0.015f, 1.045f, -0.030f, 0.015f, -0.020f, 1.005f)
        "velvia_50" -> matrix(1.085f, -0.040f, -0.045f, -0.035f, 1.105f, -0.070f, -0.030f, -0.030f, 1.060f)
        "vision3_250d" -> matrix(1.025f, -0.010f, -0.015f, -0.005f, 1.020f, -0.015f, 0.010f, -0.020f, 1.010f)
        "gold_200" -> matrix(1.060f, -0.025f, -0.035f, -0.005f, 1.025f, -0.020f, 0.010f, -0.045f, 1.035f)
        "fujicolor_c200" -> matrix(0.995f, -0.005f, 0.010f, -0.020f, 1.050f, -0.030f, -0.005f, -0.010f, 1.015f)
        else -> when (profile.filmType) {
            FilmType.VISION3 -> matrix(1.02f, -0.01f, -0.01f, -0.01f, 1.02f, -0.01f, 0f, -0.01f, 1.01f)
            else -> matrix(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        }
    }

    private fun matrix(vararg values: Float): FloatArray = values
}
