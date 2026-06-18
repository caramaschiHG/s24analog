package com.roll24.film.pipeline

import kotlin.math.floor
import kotlin.math.sqrt

/** Deterministic, multi-scale density grain. Grain size changes spatial structure, not blur. */
class StructuredGrainProcessor {
    fun process(
        buffer: FilmPixelBuffer,
        amount: Float,
        grainSize: Float,
        blackAndWhite: Boolean,
        seed: Long,
        isoScale: Float = 1f
    ) {
        val strength = amount.coerceAtLeast(0f) * isoScale.coerceIn(0.7f, 2.5f) * 0.10f
        if (strength <= 0f) return
        val fineScale = (0.65f + grainSize.coerceIn(0.2f, 3f) * 1.35f)
        val clusterScale = fineScale * 3.7f
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val i = y * buffer.width + x
                val common = valueNoise(x / fineScale, y / fineScale, seed) * 0.68f +
                    valueNoise(x / clusterScale, y / clusterScale, seed xor SEED_CLUSTER) * 0.32f
                val luminance = buffer.luminance(i).coerceIn(0f, 1f)
                val densityResponse = 0.28f + sqrt((luminance * (1f - luminance)).coerceAtLeast(0f))
                val amplitude = strength * densityResponse
                if (blackAndWhite) {
                    val grain = common * amplitude
                    buffer.r[i] = (buffer.r[i] + grain).coerceAtLeast(0f)
                    buffer.g[i] = (buffer.g[i] + grain).coerceAtLeast(0f)
                    buffer.b[i] = (buffer.b[i] + grain).coerceAtLeast(0f)
                } else {
                    buffer.r[i] = (buffer.r[i] + (common * 0.84f + valueNoise(x / fineScale, y / fineScale, seed xor SEED_R) * 0.16f) * amplitude).coerceAtLeast(0f)
                    buffer.g[i] = (buffer.g[i] + (common * 0.90f + valueNoise(x / fineScale, y / fineScale, seed xor SEED_G) * 0.10f) * amplitude).coerceAtLeast(0f)
                    buffer.b[i] = (buffer.b[i] + (common * 0.80f + valueNoise(x / fineScale, y / fineScale, seed xor SEED_B) * 0.20f) * amplitude).coerceAtLeast(0f)
                }
            }
        }
    }

    private fun valueNoise(x: Float, y: Float, seed: Long): Float {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val tx = smooth(x - x0)
        val ty = smooth(y - y0)
        val top = lerp(hash(x0, y0, seed), hash(x0 + 1, y0, seed), tx)
        val bottom = lerp(hash(x0, y0 + 1, seed), hash(x0 + 1, y0 + 1, seed), tx)
        return lerp(top, bottom, ty)
    }

    private fun hash(x: Int, y: Int, seed: Long): Float {
        var value = seed xor (x.toLong() * 0x632BE59BD9B4E019UL.toLong()) xor
            (y.toLong() * 0x9E3779B97F4A7C15UL.toLong())
        value = (value xor (value ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
        value = (value xor (value ushr 27)) * 0x94D049BB133111EBUL.toLong()
        value = value xor (value ushr 31)
        return (((value ushr 40) and 0xffffffL).toFloat() / 0x7fffff) - 1f
    }

    private fun smooth(value: Float): Float = value * value * (3f - 2f * value)
    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private companion object {
        const val SEED_CLUSTER = 0x4F1BBCDC1234L
        const val SEED_R = 0x13579BDF2468L
        const val SEED_G = 0x2468ACE01357L
        const val SEED_B = 0x55AA33CC77DDL
    }
}
