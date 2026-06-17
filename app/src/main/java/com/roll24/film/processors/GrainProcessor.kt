package com.roll24.film.processors

import android.graphics.Bitmap
import com.roll24.sensor.SensorNoiseModel
import com.roll24.sensor.SensorSpec
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class GrainProcessor {

    /**
     * Applies procedural film grain.
     *
     * The [size] parameter is reserved for future spatial scaling and currently
     * only affects the amplitude slightly.
     *
     * @param captureIso Optional ISO at which the image was captured. When
     *        provided together with [sensorSpec], the grain amplitude is scaled
     *        by the ratio of capture ISO to the sensor's estimated base ISO.
     * @param sensorSpec Optional sensor specification used to look up the
     *        estimated noise model.
     */
    fun apply(
        bitmap: Bitmap,
        amount: Float,
        size: Float,
        captureIso: Int? = null,
        sensorSpec: SensorSpec? = null
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val noiseModel = sensorSpec?.let { SensorNoiseModel.forSensor(it) }
        val baseIso = noiseModel?.baseIso ?: 100
        val isoRatio = if (captureIso != null && baseIso > 0) {
            sqrt((captureIso.toFloat() / baseIso)).coerceIn(0.5f, 4f)
        } else {
            1f
        }

        val sizeFactor = (0.8f + size * 0.4f).coerceIn(0.5f, 2f)
        val grainIntensity = amount * 50f * isoRatio * sizeFactor

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                var r = (pixel shr 16) and 0xFF
                var g = (pixel shr 8) and 0xFF
                var b = pixel and 0xFF

                // Luminance-based grain: more visible in midtones, preserves hue.
                val lum = (r * 0.299 + g * 0.587 + b * 0.114) / 255f
                val grainFactor = 1f - kotlin.math.abs(lum - 0.5f) * 2f

                val grain = (Random.nextFloat() - 0.5f) * grainIntensity * grainFactor

                r = min(255, max(0, (r + grain).toInt()))
                g = min(255, max(0, (g + grain).toInt()))
                b = min(255, max(0, (b + grain).toInt()))

                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
