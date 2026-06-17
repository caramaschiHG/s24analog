package com.roll24.film.processors

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/**
 * Physical bloom approximation.
 *
 * Creates a soft glow around bright areas by building a luminance highlight
 * mask, blurring it with a separable box blur, and screen-blending it back.
 * This replaces the previous [BlurMaskFilter] based implementation.
 */
class BloomProcessor {

    /**
     * @param threshold Normalized luminance threshold above which bloom begins
     *        (0..1).
     */
    fun apply(bitmap: Bitmap, amount: Float, threshold: Float = 0.70f): Bitmap {
        if (amount <= 0f) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val mask = FloatArray(width * height)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val thresholdInt = (threshold.coerceIn(0f, 1f) * 255f).toInt()

        // Build luminance highlight mask.
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            if (lum > thresholdInt) {
                mask[i] = (lum - thresholdInt) / (255 - thresholdInt).toFloat()
            }
        }

        // Blur the mask.
        val radius = (2f + amount * 8f).toInt().coerceAtLeast(1)
        separableBoxBlur(mask, width, height, radius)

        // Screen blend with opacity.
        val opacity = amount * 0.7f
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val m = mask[i] * opacity
            val bloomR = (m * 255f).toInt()
            val bloomG = (m * 255f).toInt()
            val bloomB = (m * 255f).toInt()

            // Screen blend: 1 - (1 - base) * (1 - bloom)
            val sr = 255 - ((255 - r) * (255 - bloomR)) / 255
            val sg = 255 - ((255 - g) * (255 - bloomG)) / 255
            val sb = 255 - ((255 - b) * (255 - bloomB)) / 255

            pixels[i] = (a shl 24) or
                (min(255, max(0, sr)) shl 16) or
                (min(255, max(0, sg)) shl 8) or
                min(255, max(0, sb))
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
