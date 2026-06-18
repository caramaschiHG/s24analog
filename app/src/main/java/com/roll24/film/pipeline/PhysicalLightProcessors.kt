package com.roll24.film.pipeline

import android.graphics.Color
import kotlin.math.sqrt

/** Builds halation from the uncompressed highlight signal, without blurring the image itself. */
class PhysicalHalationProcessor {
    fun process(
        target: FilmPixelBuffer,
        highlightSource: FilmPixelBuffer,
        amount: Float,
        threshold: Float,
        color: Int
    ) {
        val strength = amount.coerceIn(0f, 1.5f)
        if (strength <= 0f) return
        val mask = FloatArray(target.size)
        val knee = threshold.coerceIn(0.45f, 0.98f)
        for (i in mask.indices) {
            val redWeighted = highlightSource.r[i] * 0.62f +
                highlightSource.g[i] * 0.30f + highlightSource.b[i] * 0.08f
            mask[i] = ((redWeighted - knee) / (1f - knee).coerceAtLeast(0.02f)).coerceIn(0f, 2f)
        }
        val tintR = Color.red(color) / 255f
        val tintG = Color.green(color) / 255f
        val tintB = Color.blue(color) / 255f
        FilmBufferMath.forEachBlurredTile(mask, target.width, target.height, 2) { startY, endY, inner, sourceStartY ->
            for (y in startY until endY) for (x in 0 until target.width) {
                val i = y * target.width + x
                val local = (y - sourceStartY) * target.width + x
                val halo = (inner[local] * 0.58f - mask[i] * 0.32f).coerceAtLeast(0f) * strength * 0.12f
                target.r[i] += halo * tintR
                target.g[i] += halo * tintG
                target.b[i] += halo * tintB
            }
        }
        FilmBufferMath.forEachBlurredTile(mask, target.width, target.height, 7) { startY, endY, outer, sourceStartY ->
            for (y in startY until endY) for (x in 0 until target.width) {
                val i = y * target.width + x
                val local = (y - sourceStartY) * target.width + x
                val halo = outer[local] * 0.42f * strength * 0.12f
                target.r[i] += halo * tintR
                target.g[i] += halo * tintG
                target.b[i] += halo * tintB
            }
        }
    }
}

/** Adds a broad, neutral veil around strong highlights while retaining their cores. */
class PhysicalBloomProcessor {
    fun process(target: FilmPixelBuffer, highlightSource: FilmPixelBuffer, amount: Float, threshold: Float) {
        val strength = amount.coerceIn(0f, 1.5f)
        if (strength <= 0f) return
        val knee = threshold.coerceIn(0.4f, 0.98f)
        val mask = FloatArray(target.size)
        for (i in mask.indices) {
            val luminance = highlightSource.luminance(i)
            mask[i] = ((luminance - knee) / (1f - knee).coerceAtLeast(0.02f)).coerceIn(0f, 1.5f)
        }
        FilmBufferMath.forEachBlurredTile(mask, target.width, target.height, 10) { startY, endY, bloom, sourceStartY ->
            for (y in startY until endY) for (x in 0 until target.width) {
                val i = y * target.width + x
                val local = (y - sourceStartY) * target.width + x
                val veil = bloom[local] * strength * 0.045f
                target.r[i] += veil
                target.g[i] += veil
                target.b[i] += veil
            }
        }
    }
}

/** Reduces digital microcontrast and diffuses only highlights; it is not a global blur. */
class MicrocontrastSoftnessProcessor {
    fun process(buffer: FilmPixelBuffer, amount: Float) {
        val strength = amount.coerceIn(0f, 1f)
        if (strength <= 0f) return
        val luminance = FloatArray(buffer.size) { buffer.luminance(it) }
        FilmBufferMath.forEachBlurredTile(luminance, buffer.width, buffer.height, 2) { startY, endY, local, sourceStartY ->
            for (y in startY until endY) for (x in 0 until buffer.width) {
                val i = y * buffer.width + x
                val tileIndex = (y - sourceStartY) * buffer.width + x
                val detail = luminance[i] - local[tileIndex]
                val edgeProtection = 1f - sqrt((detail * 7f).let { it * it }).coerceIn(0f, 1f)
                val correction = detail * strength * 0.35f * edgeProtection
                buffer.r[i] = (buffer.r[i] - correction).coerceAtLeast(0f)
                buffer.g[i] = (buffer.g[i] - correction).coerceAtLeast(0f)
                buffer.b[i] = (buffer.b[i] - correction).coerceAtLeast(0f)
            }
        }
        for (i in luminance.indices) {
            luminance[i] = ((luminance[i] - 0.62f) / 0.38f).coerceIn(0f, 1f)
        }
        FilmBufferMath.forEachBlurredTile(luminance, buffer.width, buffer.height, 4) { startY, endY, diffusion, sourceStartY ->
            for (y in startY until endY) for (x in 0 until buffer.width) {
                val i = y * buffer.width + x
                val tileIndex = (y - sourceStartY) * buffer.width + x
                val veil = diffusion[tileIndex] * strength * 0.018f
                buffer.r[i] += veil
                buffer.g[i] += veil
                buffer.b[i] += veil
            }
        }
    }
}
