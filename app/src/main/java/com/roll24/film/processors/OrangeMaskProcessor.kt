package com.roll24.film.processors

import android.graphics.Bitmap
import android.graphics.Color
import com.roll24.film.FilmType
import kotlin.math.max
import kotlin.math.min

/**
 * Orange mask removal for C-41 and Vision3 color negative films.
 *
 * The processor estimates the orange base mask from dark/orange pixels in the
 * image, subtracts it primarily from the red and green channels, and rebalances
 * the blue channel. It is skipped for slide (E6) and black-and-white films.
 */
class OrangeMaskProcessor {

    /**
     * Removes the estimated orange mask when [filmType] is [FilmType.C41] or
     * [FilmType.VISION3]. For other film types the bitmap is returned unchanged.
     */
    fun process(bitmap: Bitmap, filmType: FilmType): Bitmap {
        if (filmType != FilmType.C41 && filmType != FilmType.VISION3) {
            return bitmap
        }

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val (maskR, maskG, maskB) = estimateOrangeMask(pixels)
        val strength = 0.6f

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF

            // Subtract the orange mask from red and green; add a small blue
            // boost so neutral tones rebalance instead of simply darkening.
            r = (r - maskR * strength).toInt()
            g = (g - maskG * strength).toInt()
            b = (b + maskB * strength * 0.25f).toInt()

            pixels[i] = (a shl 24) or
                (min(255, max(0, r)) shl 16) or
                (min(255, max(0, g)) shl 8) or
                min(255, max(0, b))
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Estimates the orange mask color by averaging dark, orange-ish pixels.
     * Falls back to a typical C-41 orange mask if no suitable samples are found.
     */
    private fun estimateOrangeMask(pixels: IntArray): Triple<Float, Float, Float> {
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

            // Dark negative base area with a clear orange cast.
            val isOrange = r > g && g > b && (r - g) > 15 && (g - b) > 10
            if (lum < 100 && isOrange) {
                sumR += r
                sumG += g
                sumB += b
                count++
            }
        }

        return if (count > 0) {
            Triple(sumR / count.toFloat(), sumG / count.toFloat(), sumB / count.toFloat())
        } else {
            // Typical C-41 orange mask estimate.
            Triple(180f, 120f, 40f)
        }
    }
}
