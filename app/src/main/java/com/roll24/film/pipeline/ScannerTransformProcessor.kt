package com.roll24.film.pipeline

import android.graphics.Bitmap
import android.graphics.Color
import com.roll24.film.FilmType
import kotlin.math.pow
import kotlin.math.roundToInt

/** Converts normalized dye density into display-linear RGB, including negative-base calibration. */
class ScannerTransformProcessor {
    fun process(buffer: FilmPixelBuffer, filmType: FilmType) {
        val params = when (filmType) {
            FilmType.C41 -> ScannerParams(0.16f, 0.08f, 0.025f, 0.96f, 0.015f)
            FilmType.VISION3 -> ScannerParams(0.20f, 0.11f, 0.035f, 0.90f, 0.025f)
            FilmType.E6 -> ScannerParams(0f, 0f, 0f, 1.08f, 0.01f)
            FilmType.BLACK_AND_WHITE -> ScannerParams(0f, 0f, 0f, 1.02f, 0.02f)
        }
        for (i in 0 until buffer.size) {
            // Orange base exists only in the simulated negative/scanner domain.
            val densityR = buffer.r[i] + params.maskR
            val densityG = buffer.g[i] + params.maskG
            val densityB = buffer.b[i] + params.maskB
            buffer.r[i] = scanChannel(densityR, params.maskR, params)
            buffer.g[i] = scanChannel(densityG, params.maskG, params)
            buffer.b[i] = scanChannel(densityB, params.maskB, params)
        }
    }

    fun negativePreview(density: FilmPixelBuffer, filmType: FilmType): FilmPixelBuffer {
        val preview = density.deepCopy()
        val negative = filmType == FilmType.C41 || filmType == FilmType.VISION3 || filmType == FilmType.BLACK_AND_WHITE
        for (i in 0 until preview.size) {
            if (negative) {
                val orange = if (filmType == FilmType.BLACK_AND_WHITE) 0f else 1f
                preview.r[i] = (1f - preview.r[i]) * (0.58f + orange * 0.18f)
                preview.g[i] = (1f - preview.g[i]) * (0.50f + orange * 0.08f)
                preview.b[i] = (1f - preview.b[i]) * (0.38f + orange * 0.02f)
            }
        }
        return preview
    }

    fun negativePreviewBitmap(density: FilmPixelBuffer, filmType: FilmType): Bitmap {
        val bitmap = Bitmap.createBitmap(density.width, density.height, Bitmap.Config.ARGB_8888)
        val rowPixels = IntArray(density.width * ROW_CHUNK)
        val negative = filmType == FilmType.C41 || filmType == FilmType.VISION3 || filmType == FilmType.BLACK_AND_WHITE
        var startY = 0
        while (startY < density.height) {
            val rows = ROW_CHUNK.coerceAtMost(density.height - startY)
            val count = density.width * rows
            val source = startY * density.width
            for (destinationIndex in 0 until count) {
                val i = source + destinationIndex
                var r = density.r[i]
                var g = density.g[i]
                var b = density.b[i]
                if (negative) {
                    val orange = if (filmType == FilmType.BLACK_AND_WHITE) 0f else 1f
                    r = (1f - r) * (0.58f + orange * 0.18f)
                    g = (1f - g) * (0.50f + orange * 0.08f)
                    b = (1f - b) * (0.38f + orange * 0.02f)
                }
                rowPixels[destinationIndex] = Color.argb(
                    ((density.a?.get(i) ?: 1f).coerceIn(0f, 1f) * 255f).roundToInt(),
                    linearToSrgb8(r),
                    linearToSrgb8(g),
                    linearToSrgb8(b)
                )
            }
            bitmap.setPixels(rowPixels, 0, density.width, 0, startY, density.width, rows)
            startY += rows
        }
        return bitmap
    }

    private fun scanChannel(density: Float, base: Float, params: ScannerParams): Float {
        val calibrated = (density - base).coerceAtLeast(0f)
        return (calibrated + params.blackLift).pow(params.contrast).coerceAtLeast(0f)
    }

    private fun linearToSrgb8(value: Float): Int {
        val linear = value.coerceIn(0f, 1f)
        val encoded = if (linear <= 0.0031308f) linear * 12.92f else
            1.055f * linear.pow(1f / 2.4f) - 0.055f
        return (encoded * 255f).roundToInt().coerceIn(0, 255)
    }

    private data class ScannerParams(
        val maskR: Float,
        val maskG: Float,
        val maskB: Float,
        val contrast: Float,
        val blackLift: Float
    )

    private companion object { const val ROW_CHUNK = 64 }
}
