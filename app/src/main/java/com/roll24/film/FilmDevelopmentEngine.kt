package com.roll24.film

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.roll24.camera.CaptureSource
import com.roll24.camera.SensorProfile
import com.roll24.film.FeatureFlags
import com.roll24.film.processors.*
import com.roll24.image.CaptureMetadata
import com.roll24.sensor.SensorSpectralResponse
import kotlin.math.pow
import kotlin.math.roundToInt

class FilmDevelopmentEngine {

    companion object {
        private const val TAG = "FilmDevelopmentEngine"
    }

    private val toneCurveProcessor = ToneCurveProcessor()
    private val colorProcessor = ColorProcessor()
    private val grainProcessor = GrainProcessor()
    private val halationProcessor = HalationProcessor()
    private val bloomProcessor = BloomProcessor()
    private val vignetteProcessor = VignetteProcessor()
    private val softnessProcessor = SoftnessProcessor()
    private val hdCurveProcessor = HdCurveProcessor()
    private val orangeMaskProcessor = OrangeMaskProcessor()

    data class DevelopedPair(
        val negative: Bitmap,
        val developed: Bitmap
    )

    fun developPair(
        bitmap: Bitmap,
        profile: FilmProfile,
        labSettings: FilmLabSettings,
        sensorProfile: SensorProfile? = null,
        captureIso: Int? = null,
        captureMetadata: CaptureMetadata? = null
    ): DevelopedPair {
        val negative = createNegative(bitmap, profile, labSettings)
        val developed = develop(bitmap, profile, labSettings, sensorProfile, captureIso, captureMetadata = captureMetadata)
        return DevelopedPair(negative = negative, developed = developed)
    }

    /**
     * Main development pipeline - applies film simulation to bitmap
     * Pipeline order:
     * 1. Normalize
     * 2. Reduce digital look
     * 3. Tone curve
     * 4. Highlight compression
     * 5. Analog response (H&D curve)
     * 6. Shadow control
     * 7. Orange mask removal (negative films only)
     * 8. Color adjustment
     * 9. B&W conversion (if applicable)
     * 10. Halation
     * 11. Bloom
     * 12. Vignette
     * 13. Grain
     * 14. Softness
     */
    fun develop(bitmap: Bitmap, profile: FilmProfile): Bitmap {
        return develop(bitmap, profile, FilmLabSettings())
    }

    fun develop(
        bitmap: Bitmap,
        profile: FilmProfile,
        labSettings: FilmLabSettings
    ): Bitmap {
        return developInternal(
            bitmap = bitmap,
            profile = profile,
            labSettings = labSettings,
            sensorProfile = null,
            captureIso = null,
            resolution = ProcessingResolution.FULL,
            useNewPipeline = false
        )
    }

    /**
     * Simple resolution-aware overload that defaults to the legacy/identity
     * path because no sensor profile is supplied.
     */
    fun develop(
        bitmap: Bitmap,
        profile: FilmProfile,
        labSettings: FilmLabSettings,
        resolution: ProcessingResolution
    ): Bitmap {
        return developInternal(
            bitmap = bitmap,
            profile = profile,
            labSettings = labSettings,
            sensorProfile = null,
            captureIso = null,
            resolution = resolution,
            useNewPipeline = false
        )
    }

    /**
     * Legacy development path for A/B comparison.
     *
     * Runs the same pipeline as [develop] but with all new Wave-3 processors
     * disabled, reproducing the pre-physical-emulation look.
     */
    fun developLegacy(bitmap: Bitmap, profile: FilmProfile): Bitmap {
        return develop(bitmap, profile, FilmLabSettings(), ProcessingResolution.FULL)
    }

    /**
     * Full development pipeline with sensor awareness and feature-flag gating.
     *
     * When [sensorProfile] is null the new pipeline still follows the feature
     * flags, but spectral normalization is skipped because no sensor match is
     * available. Callers that do not need the new pipeline can use the
     * [developLegacy] path or the [develop] overloads without sensor data.
     */
    fun develop(
        bitmap: Bitmap,
        profile: FilmProfile,
        labSettings: FilmLabSettings,
        sensorProfile: SensorProfile?,
        captureIso: Int? = null,
        resolution: ProcessingResolution = ProcessingResolution.FULL,
        captureMetadata: CaptureMetadata? = null
    ): Bitmap {
        val effectiveIso = captureIso ?: captureMetadata?.iso
        return developInternal(
            bitmap = bitmap,
            profile = profile,
            labSettings = labSettings,
            sensorProfile = sensorProfile,
            captureIso = effectiveIso,
            resolution = resolution,
            useNewPipeline = FeatureFlags.useNewPipeline
        )
    }

    private fun developInternal(
        bitmap: Bitmap,
        profile: FilmProfile,
        labSettings: FilmLabSettings,
        sensorProfile: SensorProfile?,
        captureIso: Int?,
        resolution: ProcessingResolution,
        useNewPipeline: Boolean
    ): Bitmap {
        Log.d(TAG, "Starting film development with profile: ${profile.name}, resolution: $resolution, newPipeline: $useNewPipeline, captureSource: ${labSettings.captureSource}")
        val startTime = System.currentTimeMillis()
        val adjustedProfile = profile.withLabSettings(labSettings)

        // Resolution-aware downsampling. The pipeline works at the target size
        // and the final bitmap dimensions match the requested resolution.
        val (targetWidth, targetHeight) = resolution.computeTargetSize(
            bitmap.width,
            bitmap.height,
            labSettings.targetOutputWidth.takeIf { it > 0 },
            labSettings.targetOutputHeight.takeIf { it > 0 }
        )

        val working = if (targetWidth == bitmap.width && targetHeight == bitmap.height) {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }

        var result = working

        try {
            // 1. Normalize (prepare for processing)
            result = normalize(
                result,
                sensorProfile,
                labSettings.normalizeAmount,
                useNewPipeline,
                labSettings.captureSource
            )

            // 2. Reduce digital look (subtle softening of harsh digital characteristics)
            if (useNewPipeline) {
                result = reduceDigitalLook(result, labSettings.digitalLookReduction, labSettings.captureSource)
            }

            // 3. Apply tone curve
            result = toneCurveProcessor.apply(result, adjustedProfile.contrast, adjustedProfile.blackPoint)

            // 4. Highlight compression
            result = toneCurveProcessor.compressHighlights(result, adjustedProfile.highlightCompression)

            // 5. Analog response: per-channel H&D curve.
            val hdParams = if (useNewPipeline) {
                adjustedProfile.hdCurveParams ?: HdCurveParams.defaultFromProfile(adjustedProfile)
            } else {
                HdCurveParams.defaultFromProfile(adjustedProfile)
            }
            result = hdCurveProcessor.process(result, hdParams)

            // 6. Shadow control
            result = toneCurveProcessor.liftShadows(result, adjustedProfile.shadowLift)

            // 7. Orange mask removal for color negative films.
            val applyOrangeMask = useNewPipeline &&
                FeatureFlags.useOrangeMaskRemoval &&
                (adjustedProfile.filmType == FilmType.C41 || adjustedProfile.filmType == FilmType.VISION3)
            if (applyOrangeMask) {
                result = orangeMaskProcessor.process(result, adjustedProfile.filmType)
            }

            // 8. Color adjustment (saturation, warmth, tint)
            if (!adjustedProfile.blackAndWhite) {
                result = colorProcessor.adjust(
                    result,
                    adjustedProfile.saturation,
                    adjustedProfile.warmth,
                    adjustedProfile.tint
                )
            }

            // 9. B&W conversion if needed
            if (adjustedProfile.blackAndWhite) {
                result = colorProcessor.convertToBlackAndWhite(result)
            }

            // 10. Halation (red/orange glow around bright areas)
            if (adjustedProfile.halationAmount > 0f) {
                result = halationProcessor.apply(
                    result,
                    adjustedProfile.halationAmount,
                    adjustedProfile.halationColor,
                    adjustedProfile.halationThreshold
                )
            }

            // 11. Bloom (soft glow around bright areas)
            if (adjustedProfile.bloomAmount > 0f) {
                result = bloomProcessor.apply(
                    result,
                    adjustedProfile.bloomAmount,
                    adjustedProfile.bloomThreshold
                )
            }

            // 12. Vignette
            if (adjustedProfile.vignetteAmount > 0f) {
                result = vignetteProcessor.apply(result, adjustedProfile.vignetteAmount)
            }

            // 12b. Lens/sensor signature: subtle corner falloff and microcontrast variation.
            result = applyLensSignature(result, adjustedProfile)

            // 13. Grain
            if (adjustedProfile.grainAmount > 0f) {
                val sensorSpec = if (useNewPipeline) sensorProfile?.matchedDossierSpec else null
                result = grainProcessor.apply(
                    result,
                    adjustedProfile.grainAmount,
                    adjustedProfile.grainSize,
                    captureIso,
                    sensorSpec
                )
            }

            // 14. Final softness
            if (adjustedProfile.softnessAmount > 0f) {
                result = softnessProcessor.apply(result, adjustedProfile.softnessAmount)
            }

            result = blendWithOriginal(working, result, labSettings.filmIntensity.coerceIn(0f, 1.25f))

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Film development completed in ${elapsed}ms")

        } catch (e: Exception) {
            Log.e(TAG, "Error during film development", e)
            // Return working bitmap (already at target resolution) on error
            result = working
        }

        return result
    }

    private fun createNegative(
        bitmap: Bitmap,
        profile: FilmProfile,
        labSettings: FilmLabSettings
    ): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val warmth = (profile.warmth + labSettings.warmth).coerceIn(-1f, 1f)
        val maskRed = (34 + warmth * 18f).roundToInt()
        val maskGreen = (18 + warmth * 8f).roundToInt()
        val maskBlue = 4

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                val red = (255 - Color.red(pixel) + maskRed).coerceIn(0, 255)
                val green = (255 - Color.green(pixel) + maskGreen).coerceIn(0, 255)
                val blue = (255 - Color.blue(pixel) + maskBlue).coerceIn(0, 255)
                output.setPixel(x, y, Color.argb(alpha, red, green, blue))
            }
        }

        return output
    }

    private fun FilmProfile.withLabSettings(labSettings: FilmLabSettings): FilmProfile {
        return copy(
            exposureCompensation = exposureCompensation + labSettings.pushPull,
            contrast = contrast + labSettings.contrast + (labSettings.pushPull * 0.08f),
            saturation = saturation + (labSettings.filmIntensity - 1f) * 0.12f,
            warmth = warmth + labSettings.warmth,
            shadowLift = shadowLift - (labSettings.pushPull * 0.03f),
            highlightCompression = highlightCompression + (labSettings.pushPull * 0.04f),
            grainAmount = grainAmount * labSettings.grainAmount,
            halationAmount = halationAmount * labSettings.halationAmount,
            bloomAmount = bloomAmount * labSettings.bloomAmount,
            vignetteAmount = vignetteAmount * labSettings.vignetteAmount
        )
    }

    private fun blendWithOriginal(original: Bitmap, developed: Bitmap, intensity: Float): Bitmap {
        if (intensity >= 0.99f && intensity <= 1.01f) return developed
        val output = Bitmap.createBitmap(developed.width, developed.height, Bitmap.Config.ARGB_8888)
        val safeIntensity = intensity.coerceIn(0f, 1.25f)

        for (y in 0 until developed.height) {
            val sourceY = (y * original.height / developed.height).coerceIn(0, original.height - 1)
            for (x in 0 until developed.width) {
                val sourceX = (x * original.width / developed.width).coerceIn(0, original.width - 1)
                val base = original.getPixel(sourceX, sourceY)
                val film = developed.getPixel(x, y)
                output.setPixel(
                    x,
                    y,
                    Color.argb(
                        Color.alpha(film),
                        mix(Color.red(base), Color.red(film), safeIntensity),
                        mix(Color.green(base), Color.green(film), safeIntensity),
                        mix(Color.blue(base), Color.blue(film), safeIntensity)
                    )
                )
            }
        }

        return output
    }

    private fun mix(base: Int, film: Int, intensity: Float): Int {
        return (base + ((film - base) * intensity)).roundToInt().coerceIn(0, 255)
    }

    private fun applyLensSignature(bitmap: Bitmap, profile: FilmProfile): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val centerX = (output.width - 1) / 2f
        val centerY = (output.height - 1) / 2f
        val maxDistance = kotlin.math.sqrt(centerX * centerX + centerY * centerY).coerceAtLeast(1f)
        val falloffStrength = (0.025f + profile.vignetteAmount * 0.035f).coerceIn(0f, 0.12f)
        val edgeWarmth = profile.warmth * 5f

        for (y in 0 until output.height) {
            for (x in 0 until output.width) {
                val pixel = output.getPixel(x, y)
                val dx = x - centerX
                val dy = y - centerY
                val distance = kotlin.math.sqrt(dx * dx + dy * dy) / maxDistance
                val falloff = (1f - distance * distance * falloffStrength).coerceIn(0.82f, 1f)
                val edge = distance * distance
                val red = (Color.red(pixel) * falloff + edgeWarmth * edge).roundToInt().coerceIn(0, 255)
                val green = (Color.green(pixel) * falloff).roundToInt().coerceIn(0, 255)
                val blue = (Color.blue(pixel) * falloff - edgeWarmth * edge).roundToInt().coerceIn(0, 255)
                output.setPixel(x, y, Color.argb(Color.alpha(pixel), red, green, blue))
            }
        }

        return output
    }
    
    /**
     * Converts the input bitmap to a linear working space.
     *
     * If a matched S24 Ultra sensor is present, the inverse of its estimated
     * spectral response matrix is applied to move from sensor RGB toward a
     * neutral film working RGB. The amount of that correction is controlled by
     * [amount]. When no sensor match exists, only a gamma (sRGB EOTF) decode
     * is applied.
     */
    private fun normalize(
        bitmap: Bitmap,
        sensorProfile: SensorProfile?,
        amount: Float,
        useNewPipeline: Boolean,
        captureSource: CaptureSource
    ): Bitmap {
        if (amount <= 0f) return bitmap

        Log.d(TAG, "normalize path: $captureSource")
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // RAW_DNG input is already closer to linear sensor data, so skip the
        // per-sensor inverse spectral remap and apply only gamma linearization.
        val spec = sensorProfile?.matchedDossierSpec
        val spectralMatrix = if (
            captureSource != CaptureSource.RAW_DNG &&
            useNewPipeline &&
            FeatureFlags.useSpectralNormalize &&
            spec != null
        ) {
            SensorSpectralResponse.forSensor(spec)
        } else {
            null
        }

        // Build the 3x3 inverse spectral matrix once. If spectral normalization
        // is disabled, no sensor match is available, or the input came from RAW,
        // use the identity matrix (gamma-only normalization).
        val inv = if (spectralMatrix != null) invert3x3(spectralMatrix) else identity3x3()
        val width = output.width
        val height = output.height
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = sRgbToLinear((pixel shr 16) and 0xFF)
            val g = sRgbToLinear((pixel shr 8) and 0xFF)
            val b = sRgbToLinear(pixel and 0xFF)

            // Apply inverse spectral matrix in linear space.
            val rn = inv[0] * r + inv[1] * g + inv[2] * b
            val gn = inv[3] * r + inv[4] * g + inv[5] * b
            val bn = inv[6] * r + inv[7] * g + inv[8] * b

            // Blend between gamma-only and sensor-corrected by amount.
            val rr = lerp(r, rn, amount)
            val rg = lerp(g, gn, amount)
            val rb = lerp(b, bn, amount)

            pixels[i] = (a shl 24) or
                (linearToSrgb(rr) shl 16) or
                (linearToSrgb(rg) shl 8) or
                linearToSrgb(rb)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Reduces harsh digital characteristics by blending the image with a
     * slightly blurred version of itself. The blend intensity is controlled by
     * [amount]; 0 leaves the image unchanged.
     */
    private fun reduceDigitalLook(bitmap: Bitmap, amount: Float, captureSource: CaptureSource): Bitmap {
        // RAW sensor data has not been through the ISP digital look, so only a
        // tiny fraction of digital-look reduction is appropriate.
        val effectiveAmount = when (captureSource) {
            CaptureSource.RAW_DNG -> amount * 0.2f
            CaptureSource.JPEG -> amount
        }
        Log.d(TAG, "reduceDigitalLook path: $captureSource, effectiveAmount: $effectiveAmount")
        if (effectiveAmount <= 0f) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val original = IntArray(width * height)
        val blurred = IntArray(width * height)
        bitmap.getPixels(original, 0, width, 0, 0, width, height)
        bitmap.getPixels(blurred, 0, width, 0, 0, width, height)

        // Small-radius separable box blur preserves edges better than a full
        // Gaussian blur while still softening fine digital sharpening artifacts.
        val radius = (1f + effectiveAmount * 2f).toInt().coerceAtLeast(1)
        boxBlur(blurred, width, height, radius)

        for (i in original.indices) {
            val orig = original[i]
            val blur = blurred[i]
            val a = (orig shr 24) and 0xFF
            val origR = (orig shr 16) and 0xFF
            val origG = (orig shr 8) and 0xFF
            val origB = orig and 0xFF
            val br = (blur shr 16) and 0xFF
            val bg = (blur shr 8) and 0xFF
            val bb = blur and 0xFF

            // Edge-preserving blend: weight the blur less where local contrast
            // is high. A simple approximation uses the per-channel difference
            // to reduce blur strength on edges.
            val edgeR = kotlin.math.abs(origR - br) / 255f
            val edgeG = kotlin.math.abs(origG - bg) / 255f
            val edgeB = kotlin.math.abs(origB - bb) / 255f
            val edge = kotlin.math.max(edgeR, kotlin.math.max(edgeG, edgeB))
            val localAmount = effectiveAmount * (1f - edge * 0.7f)

            original[i] = (a shl 24) or
                (lerpInt(origR, br, localAmount) shl 16) or
                (lerpInt(origG, bg, localAmount) shl 8) or
                lerpInt(origB, bb, localAmount)
        }

        bitmap.setPixels(original, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun sRgbToLinear(v: Int): Float {
        val s = v / 255f
        return if (s <= 0.04045f) s / 12.92f else ((s + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSrgb(v: Float): Int {
        val c = v.coerceIn(0f, 1f)
        val s = if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f
        return (s * 255f).roundToInt().coerceIn(0, 255)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t.coerceIn(0f, 1f)
    }

    private fun lerpInt(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
    }

    private fun invert3x3(matrix: com.roll24.sensor.SpectralMatrix): FloatArray {
        // Matrix is stored as rows [r0 r1 r2 g0 g1 g2 b0 b1 b2].
        val a = floatArrayOf(
            matrix.r[0], matrix.r[1], matrix.r[2],
            matrix.g[0], matrix.g[1], matrix.g[2],
            matrix.b[0], matrix.b[1], matrix.b[2]
        )

        val inv = FloatArray(9)
        val det =
            a[0] * (a[4] * a[8] - a[5] * a[7]) -
            a[1] * (a[3] * a[8] - a[5] * a[6]) +
            a[2] * (a[3] * a[7] - a[4] * a[6])

        if (det == 0f) return identity3x3()

        val invDet = 1f / det
        inv[0] = (a[4] * a[8] - a[5] * a[7]) * invDet
        inv[1] = (a[2] * a[7] - a[1] * a[8]) * invDet
        inv[2] = (a[1] * a[5] - a[2] * a[4]) * invDet
        inv[3] = (a[5] * a[6] - a[3] * a[8]) * invDet
        inv[4] = (a[0] * a[8] - a[2] * a[6]) * invDet
        inv[5] = (a[2] * a[3] - a[0] * a[5]) * invDet
        inv[6] = (a[3] * a[7] - a[4] * a[6]) * invDet
        inv[7] = (a[1] * a[6] - a[0] * a[7]) * invDet
        inv[8] = (a[0] * a[4] - a[1] * a[3]) * invDet
        return inv
    }

    private fun identity3x3(): FloatArray {
        return floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    }

    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int) {
        if (radius <= 0) return
        val temp = IntArray(pixels.size)

        // Horizontal pass.
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                val start = kotlin.math.max(0, x - radius)
                val end = kotlin.math.min(width - 1, x + radius)
                for (xi in start..end) {
                    val p = pixels[y * width + xi]
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                    count++
                }
                val idx = y * width + x
                temp[idx] = (0xFF shl 24) or
                    ((r / count) shl 16) or
                    ((g / count) shl 8) or
                    (b / count)
            }
        }

        // Vertical pass.
        for (x in 0 until width) {
            for (y in 0 until height) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                val start = kotlin.math.max(0, y - radius)
                val end = kotlin.math.min(height - 1, y + radius)
                for (yi in start..end) {
                    val p = temp[yi * width + x]
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                    count++
                }
                val idx = y * width + x
                pixels[idx] = (0xFF shl 24) or
                    ((r / count) shl 16) or
                    ((g / count) shl 8) or
                    (b / count)
            }
        }
    }
}
