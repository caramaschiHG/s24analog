package com.roll24.film.pipeline

import android.graphics.Bitmap
import kotlin.math.pow
import kotlin.math.roundToInt

data class FilmPixelBuffer(
    val width: Int,
    val height: Int,
    val r: FloatArray,
    val g: FloatArray,
    val b: FloatArray,
    val a: FloatArray? = null
) {
    init {
        val size = width * height
        require(r.size == size && g.size == size && b.size == size)
        require(a == null || a.size == size)
    }

    val size: Int get() = width * height

    fun deepCopy(): FilmPixelBuffer = FilmPixelBuffer(
        width,
        height,
        r.copyOf(),
        g.copyOf(),
        b.copyOf(),
        a?.copyOf()
    )

    fun luminance(index: Int): Float =
        r[index] * 0.2126f + g[index] * 0.7152f + b[index] * 0.0722f
}

object BitmapToFilmBuffer {
    fun convert(bitmap: Bitmap): FilmPixelBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val size = width * height
        val r = FloatArray(size)
        val g = FloatArray(size)
        val b = FloatArray(size)
        val rowPixels = IntArray(width * ROW_CHUNK)
        var hasTransparency = false
        var startY = 0
        while (startY < height) {
            val rows = ROW_CHUNK.coerceAtMost(height - startY)
            bitmap.getPixels(rowPixels, 0, width, 0, startY, width, rows)
            val count = width * rows
            val destination = startY * width
            for (sourceIndex in 0 until count) {
                val pixel = rowPixels[sourceIndex]
                val i = destination + sourceIndex
                hasTransparency = hasTransparency || ((pixel ushr 24) and 0xff) != 0xff
                r[i] = srgbToLinear((pixel ushr 16) and 0xff)
                g[i] = srgbToLinear((pixel ushr 8) and 0xff)
                b[i] = srgbToLinear(pixel and 0xff)
            }
            startY += rows
        }
        val a = if (hasTransparency) {
            FloatArray(size).also { alpha ->
                startY = 0
                while (startY < height) {
                    val rows = ROW_CHUNK.coerceAtMost(height - startY)
                    bitmap.getPixels(rowPixels, 0, width, 0, startY, width, rows)
                    val count = width * rows
                    val destination = startY * width
                    for (sourceIndex in 0 until count) {
                        alpha[destination + sourceIndex] = ((rowPixels[sourceIndex] ushr 24) and 0xff) / 255f
                    }
                    startY += rows
                }
            }
        } else {
            null
        }
        return FilmPixelBuffer(width, height, r, g, b, a)
    }

    private fun srgbToLinear(value: Int): Float {
        val encoded = value / 255f
        return if (encoded <= 0.04045f) {
            encoded / 12.92f
        } else {
            ((encoded + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    private const val ROW_CHUNK = 64
}

object FilmBufferToBitmap {
    fun convert(buffer: FilmPixelBuffer): Bitmap {
        val bitmap = Bitmap.createBitmap(buffer.width, buffer.height, Bitmap.Config.ARGB_8888)
        val rowPixels = IntArray(buffer.width * ROW_CHUNK)
        var startY = 0
        while (startY < buffer.height) {
            val rows = ROW_CHUNK.coerceAtMost(buffer.height - startY)
            val count = buffer.width * rows
            val source = startY * buffer.width
            for (destinationIndex in 0 until count) {
                val i = source + destinationIndex
                val alpha = ((buffer.a?.get(i) ?: 1f).coerceIn(0f, 1f) * 255f).roundToInt()
                rowPixels[destinationIndex] = (alpha shl 24) or
                    (linearToSrgb8(buffer.r[i]) shl 16) or
                    (linearToSrgb8(buffer.g[i]) shl 8) or
                    linearToSrgb8(buffer.b[i])
            }
            bitmap.setPixels(rowPixels, 0, buffer.width, 0, startY, buffer.width, rows)
            startY += rows
        }
        return bitmap
    }

    private fun linearToSrgb8(value: Float): Int {
        val linear = value.coerceIn(0f, 1f)
        val encoded = if (linear <= 0.0031308f) {
            linear * 12.92f
        } else {
            1.055f * linear.pow(1f / 2.4f) - 0.055f
        }
        return (encoded * 255f).roundToInt().coerceIn(0, 255)
    }

    private const val ROW_CHUNK = 64
}

internal object FilmBufferMath {
    private const val TILE_ROWS = 96

    fun boxBlur(values: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        if (radius <= 0) return values.copyOf()
        val horizontal = FloatArray(values.size)
        val output = FloatArray(values.size)
        for (y in 0 until height) {
            var sum = 0f
            for (x in -radius..radius) sum += values[y * width + x.coerceIn(0, width - 1)]
            for (x in 0 until width) {
                horizontal[y * width + x] = sum / (radius * 2 + 1)
                val removeX = (x - radius).coerceIn(0, width - 1)
                val addX = (x + radius + 1).coerceIn(0, width - 1)
                sum += values[y * width + addX] - values[y * width + removeX]
            }
        }
        for (x in 0 until width) {
            var sum = 0f
            for (y in -radius..radius) sum += horizontal[y.coerceIn(0, height - 1) * width + x]
            for (y in 0 until height) {
                output[y * width + x] = sum / (radius * 2 + 1)
                val removeY = (y - radius).coerceIn(0, height - 1)
                val addY = (y + radius + 1).coerceIn(0, height - 1)
                sum += horizontal[addY * width + x] - horizontal[removeY * width + x]
            }
        }
        return output
    }

    /**
     * Blurs bounded horizontal bands with enough vertical halo for an exact box blur.
     * Only a few rows are materialized, keeping 12 MP processing below the Android heap limit.
     */
    fun forEachBlurredTile(
        values: FloatArray,
        width: Int,
        height: Int,
        radius: Int,
        consume: (startY: Int, endY: Int, blurred: FloatArray, sourceStartY: Int) -> Unit
    ) {
        var startY = 0
        while (startY < height) {
            val endY = (startY + TILE_ROWS).coerceAtMost(height)
            val sourceStartY = (startY - radius).coerceAtLeast(0)
            val sourceEndY = (endY + radius).coerceAtMost(height)
            val sourceHeight = sourceEndY - sourceStartY
            val source = FloatArray(width * sourceHeight)
            values.copyInto(
                source,
                destinationOffset = 0,
                startIndex = sourceStartY * width,
                endIndex = sourceEndY * width
            )
            val blurred = boxBlur(source, width, sourceHeight, radius)
            consume(startY, endY, blurred, sourceStartY)
            startY = endY
        }
    }
}
