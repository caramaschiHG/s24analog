package com.roll24.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import com.roll24.film.FeatureFlags
import com.roll24.image.OrientationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

/**
 * S24-first Bayer renderer.
 *
 * Critical fix: white balance gains are applied by COLOR CHANNEL (red/green/blue),
 * not by CFA position index. This ensures correct rendering for ANY CFA pattern
 * (RGGB, GRBG, GBRG, BGGR).
 */
object RawRenderer {
    private const val TAG = "RawRenderer"

    suspend fun render(frame: RawFrame): RawRenderResult = withContext(Dispatchers.Default) {
        val startedAt = SystemClock.elapsedRealtime()
        val pixels = IntArray(frame.width * frame.height)
        var clipped = 0L

        // ─── Debug stats accumulators (sampled every 64th pixel for speed) ───────
        var sensorRSum = 0.0; var sensorGSum = 0.0; var sensorBSum = 0.0
        var linearRSum = 0.0; var linearGSum = 0.0; var linearBSum = 0.0
        var sensorRMax = 0f; var sensorGMax = 0f; var sensorBMax = 0f
        var linearRMax = 0f; var linearGMax = 0f; var linearBMax = 0f
        var statSamples = 0L

        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val color = frame.colorAt(x, y)
                val center = frame.sample(x, y, color)
                val sensorR: Float
                val sensorG: Float
                val sensorB: Float
                when (color) {
                    RED -> {
                        sensorR = center
                        sensorG = frame.average(x, y, GREEN, CARDINAL)
                        sensorB = frame.average(x, y, BLUE, DIAGONAL)
                    }
                    BLUE -> {
                        sensorR = frame.average(x, y, RED, DIAGONAL)
                        sensorG = frame.average(x, y, GREEN, CARDINAL)
                        sensorB = center
                    }
                    else -> {
                        val redHorizontal = frame.colorAt((x - 1).coerceAtLeast(0), y) == RED ||
                            frame.colorAt((x + 1).coerceAtMost(frame.width - 1), y) == RED
                        sensorR = frame.average(x, y, RED, if (redHorizontal) HORIZONTAL else VERTICAL)
                        sensorG = center
                        sensorB = frame.average(x, y, BLUE, if (redHorizontal) VERTICAL else HORIZONTAL)
                    }
                }

                val m = if (FeatureFlags.transposeRawColorTransform) {
                    // Transposed: swap rows/columns
                    floatArrayOf(
                        frame.colorTransform[0], frame.colorTransform[3], frame.colorTransform[6],
                        frame.colorTransform[1], frame.colorTransform[4], frame.colorTransform[7],
                        frame.colorTransform[2], frame.colorTransform[5], frame.colorTransform[8]
                    )
                } else {
                    frame.colorTransform
                }
                val linearR = m[0] * sensorR + m[1] * sensorG + m[2] * sensorB
                val linearG = m[3] * sensorR + m[4] * sensorG + m[5] * sensorB
                val linearB = m[6] * sensorR + m[7] * sensorG + m[8] * sensorB
                if (linearR > 1f || linearG > 1f || linearB > 1f) clipped++

                // Stats sampling (every 64th pixel to avoid overhead)
                if ((y * frame.width + x) and 63 == 0) {
                    sensorRSum += sensorR; sensorGSum += sensorG; sensorBSum += sensorB
                    linearRSum += linearR; linearGSum += linearG; linearBSum += linearB
                    if (sensorR > sensorRMax) sensorRMax = sensorR
                    if (sensorG > sensorGMax) sensorGMax = sensorG
                    if (sensorB > sensorBMax) sensorBMax = sensorB
                    if (linearR > linearRMax) linearRMax = linearR
                    if (linearG > linearGMax) linearGMax = linearG
                    if (linearB > linearBMax) linearBMax = linearB
                    statSamples++
                }

                val r = toSrgb(highlightRolloff(linearR)).to8Bit()
                val g = toSrgb(highlightRolloff(linearG)).to8Bit()
                val b = toSrgb(highlightRolloff(linearB)).to8Bit()
                pixels[y * frame.width + x] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // ─── Log per-stage stats ─────────────────────────────────────────────────
        val n = statSamples.coerceAtLeast(1).toFloat()
        Log.i(TAG, "RawRendererStats stage=sensor " +
            "avgR=%.3f avgG=%.3f avgB=%.3f maxR=%.3f maxG=%.3f maxB=%.3f".format(
                sensorRSum / n, sensorGSum / n, sensorBSum / n,
                sensorRMax, sensorGMax, sensorBMax
            ))
        Log.i(TAG, "RawRendererStats stage=linear " +
            "avgR=%.3f avgG=%.3f avgB=%.3f maxR=%.3f maxG=%.3f maxB=%.3f".format(
                linearRSum / n, linearGSum / n, linearBSum / n,
                linearRMax, linearGMax, linearBMax
            ))
        Log.i(TAG, "RawRendererStats clipped=${clipped}/${pixels.size} " +
            "(%.2f%%)".format(clipped * 100.0 / pixels.size.coerceAtLeast(1)))
        // ─────────────────────────────────────────────────────────────────────────

        val rotationDegrees = OrientationUtils.sanitizeRotationDegrees(frame.rotationDegrees)
        val unrotated = Bitmap.createBitmap(pixels, frame.width, frame.height, Bitmap.Config.ARGB_8888)
        val bitmap = if (rotationDegrees == 0) {
            unrotated
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(
                unrotated,
                0,
                0,
                unrotated.width,
                unrotated.height,
                matrix,
                true
            ).also { if (it !== unrotated) unrotated.recycle() }
        }

        RawRenderResult(
            bitmap = bitmap,
            metadata = frame.metadata,
            metrics = RawRenderMetrics(
                elapsedMillis = SystemClock.elapsedRealtime() - startedAt,
                sourceMegapixels = frame.width * frame.height / 1_000_000f,
                clippedPixelRatio = clipped.toFloat() / pixels.size.coerceAtLeast(1)
            )
        )
    }

    /**
     * Sample a single photosite, apply black level subtraction, white balance by COLOR,
     * and lens shading correction.
     *
     * CRITICAL: WB gain is looked up by the COLOR of this photosite (red/green/blue),
     * NOT by its CFA position index. This is the fix for green/radioactive output.
     */
    private fun RawFrame.sample(x: Int, y: Int, color: Int): Float {
        val safeX = x.coerceIn(0, width - 1)
        val safeY = y.coerceIn(0, height - 1)
        val cfaIdx = cfaIndex(safeX, safeY)
        val raw = samples[safeY * width + safeX].toInt() and 0xffff
        val normalized = ((raw - blackLevels[cfaIdx]) /
            (whiteLevel - blackLevels[cfaIdx]).coerceAtLeast(1f)).coerceAtLeast(0f)
        // WB by COLOR, not by CFA position
        return normalized * whiteBalanceGains.forColor(color) * shadingGain(safeX, safeY, color)
    }

    private fun RawFrame.average(x: Int, y: Int, wanted: Int, offsets: IntArray): Float {
        var total = 0f
        var count = 0
        var index = 0
        while (index < offsets.size) {
            val px = x + offsets[index]
            val py = y + offsets[index + 1]
            if (px in 0 until width && py in 0 until height && colorAt(px, py) == wanted) {
                total += sample(px, py, wanted)
                count++
            }
            index += 2
        }
        return if (count == 0) sample(x, y, colorAt(x, y)) else total / count
    }

    private fun RawFrame.shadingGain(x: Int, y: Int, color: Int): Float {
        val map = lensShading ?: return 1f
        if (lensShadingWidth <= 0 || lensShadingHeight <= 0) return 1f
        val column = ((x.toFloat() / (width - 1).coerceAtLeast(1)) * (lensShadingWidth - 1))
            .toInt().coerceIn(0, lensShadingWidth - 1)
        val row = ((y.toFloat() / (height - 1).coerceAtLeast(1)) * (lensShadingHeight - 1))
            .toInt().coerceIn(0, lensShadingHeight - 1)
        val channel = when (color) {
            RED -> 0
            BLUE -> 3
            else -> if (cfaIndex(x, y) == 1) 1 else 2
        }
        return map[(row * lensShadingWidth + column) * 4 + channel]
    }

    private fun RawFrame.colorAt(x: Int, y: Int): Int = when (cfaIndex(x, y)) {
        0 -> when (cfaPattern) {
            CfaPattern.RGGB -> RED
            CfaPattern.BGGR -> BLUE
            else -> GREEN
        }
        1 -> when (cfaPattern) {
            CfaPattern.GRBG -> RED
            CfaPattern.GBRG -> BLUE
            else -> GREEN
        }
        2 -> when (cfaPattern) {
            CfaPattern.GRBG -> BLUE
            CfaPattern.GBRG -> RED
            else -> GREEN
        }
        else -> when (cfaPattern) {
            CfaPattern.RGGB -> BLUE
            CfaPattern.BGGR -> RED
            else -> GREEN
        }
    }

    private fun cfaIndex(x: Int, y: Int): Int = (y and 1) * 2 + (x and 1)

    private fun highlightRolloff(value: Float): Float {
        val positive = value.coerceAtLeast(0f)
        if (positive <= 0.8f) return positive
        val excess = positive - 0.8f
        return 0.8f + 0.2f * (excess / (excess + 0.2f))
    }

    private fun toSrgb(linear: Float): Float = if (linear <= 0.0031308f) {
        linear * 12.92f
    } else {
        1.055f * linear.pow(1f / 2.4f) - 0.055f
    }

    private fun Float.to8Bit(): Int = (coerceIn(0f, 1f) * 255f + 0.5f).toInt()

    private const val RED = 0
    private const val GREEN = 1
    private const val BLUE = 2
    private val CARDINAL = intArrayOf(-1, 0, 1, 0, 0, -1, 0, 1)
    private val DIAGONAL = intArrayOf(-1, -1, 1, -1, -1, 1, 1, 1)
    private val HORIZONTAL = intArrayOf(-1, 0, 1, 0)
    private val VERTICAL = intArrayOf(0, -1, 0, 1)
}
