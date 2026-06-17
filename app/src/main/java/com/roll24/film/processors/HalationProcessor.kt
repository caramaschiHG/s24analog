package com.roll24.film.processors

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Physical halation approximation.
 *
 * Creates a red/orange glow around bright areas by building a highlight mask,
 * blurring it with a separable box blur, tinting it, and blending it back with
 * an additive/screen mix. This replaces the previous [BlurMaskFilter] based
 * implementation.
 */
class HalationProcessor {

    /**
     * @param halationColor Tint applied to the blurred halation layer (ARGB).
     * @param threshold Normalized luminance threshold above which halation
     *        begins (0..1).
     */
    fun apply(
        bitmap: Bitmap,
        amount: Float,
        halationColor: Int = Color.parseColor("#FF5522"),
        threshold: Float = 0.78f
    ): Bitmap {
        if (amount <= 0f) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val mask = FloatArray(width * height)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val thresholdInt = (threshold.coerceIn(0f, 1f) * 255f).toInt()
        val tintR = Color.red(halationColor) / 255f
        val tintG = Color.green(halationColor) / 255f
        val tintB = Color.blue(halationColor) / 255f

        // Build highlight mask from red + green channels above threshold.
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val avg = (r + g) / 2
            if (avg > thresholdInt) {
                mask[i] = (avg - thresholdInt) / (255 - thresholdInt).toFloat()
            }
        }

        // Blur the mask.
        val radius = (2f + amount * 6f).toInt().coerceAtLeast(1)
        separableBoxBlur(mask, width, height, radius)

        // Blend blurred mask back onto the image.
        val opacity = amount * 0.6f
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val m = mask[i] * opacity
            val addR = (m * 255f * tintR).toInt()
            val addG = (m * 255f * tintG).toInt()
            val addB = (m * 255f * tintB).toInt()

            pixels[i] = (a shl 24) or
                (min(255, r + addR) shl 16) or
                (min(255, g + addG) shl 8) or
                min(255, b + addB)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun separableBoxBlur(mask: FloatArray, width: Int, height: Int, radius: Int) {
        if (radius <= 0) return
        val temp = FloatArray(mask.size)

        // Horizontal pass.
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var count = 0
                val start = max(0, x - radius)
                val end = min(width - 1, x + radius)
                for (xi in start..end) {
                    sum += mask[y * width + xi]
                    count++
                }
                temp[y * width + x] = sum / count
            }
        }

        // Vertical pass.
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sum = 0f
                var count = 0
                val start = max(0, y - radius)
                val end = min(height - 1, y + radius)
                for (yi in start..end) {
                    sum += temp[yi * width + x]
                    count++
                }
                mask[y * width + x] = sum / count
            }
        }
    }
}
