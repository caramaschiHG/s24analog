package com.roll24.film.pipeline

import android.graphics.Bitmap
import android.util.Log
import com.roll24.camera.CaptureSource
import com.roll24.film.FilmLabSettings
import com.roll24.film.FilmProfile
import com.roll24.film.ProcessingResolution
import com.roll24.film.processors.HdCurveParams
import com.roll24.image.CaptureMetadata
import kotlin.math.pow
import kotlin.math.sqrt

/** Float, display-linear film pipeline. Bitmap conversion occurs only at its boundaries. */
class FilmPipelineEngine {
    data class Result(val negative: Bitmap, val developed: Bitmap)

    private val exposure = NegativeExposureProcessor()
    private val density = HdDensityProcessor()
    private val scanner = ScannerTransformProcessor()
    private val color = PhysicalColorProcessor()
    private val halation = PhysicalHalationProcessor()
    private val bloom = PhysicalBloomProcessor()
    private val rolloff = HighlightRolloffProcessor()
    private val grain = StructuredGrainProcessor()
    private val softness = MicrocontrastSoftnessProcessor()

    fun process(
        bitmap: Bitmap,
        profile: FilmProfile,
        settings: FilmLabSettings,
        captureIso: Int?,
        resolution: ProcessingResolution,
        metadata: CaptureMetadata?
    ): Result {
        val started = System.nanoTime()
        Log.d(TAG, "physical=true profile=${profile.id} source=${settings.captureSource} encoding=${settings.inputEncoding}")
        val (width, height) = resolution.computeTargetSize(
            bitmap.width,
            bitmap.height,
            settings.targetOutputWidth.takeIf { it > 0 },
            settings.targetOutputHeight.takeIf { it > 0 }
        )
        val input = if (width == bitmap.width && height == bitmap.height) bitmap else
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (settings.filmIntensity <= 0f) {
            val unchanged = input.copy(Bitmap.Config.ARGB_8888, false)
            return Result(unchanged, unchanged.copy(Bitmap.Config.ARGB_8888, false))
        }

        var stageStart = System.nanoTime()
        val buffer = BitmapToFilmBuffer.convert(input)
        stage("decode_linear", stageStart)

        stageStart = System.nanoTime()
        val sourceReduction = if (settings.captureSource == CaptureSource.RAW_DNG) 0.18f else 1f
        softness.process(buffer, settings.digitalLookReduction * sourceReduction)
        stage("digital_neutralization", stageStart)

        stageStart = System.nanoTime()
        exposure.process(buffer, profile.exposureCompensation, profile.blackAndWhite)
        val params = profile.hdCurveParams ?: HdCurveParams.defaultFromProfile(profile)
        density.process(buffer, params)
        val negative = scanner.negativePreviewBitmap(buffer, profile.filmType)
        stage("negative_hd_density", stageStart)

        stageStart = System.nanoTime()
        scanner.process(buffer, profile.filmType)
        applyShadowLift(buffer, profile.shadowLift)
        color.process(buffer, profile, settings.filmIntensity)
        stage("scanner_color", stageStart)

        stageStart = System.nanoTime()
        halation.process(
            buffer,
            buffer,
            profile.halationAmount,
            profile.halationThreshold,
            profile.halationColor
        )
        bloom.process(buffer, buffer, profile.bloomAmount, profile.bloomThreshold)
        rolloff.process(buffer, profile.highlightCompression)
        applyVignette(buffer, profile.vignetteAmount)
        stage("optical_response", stageStart)

        stageStart = System.nanoTime()
        val isoScale = sqrt(((captureIso ?: profile.baseIso).toFloat() / profile.baseIso.coerceAtLeast(1)).coerceAtLeast(0.5f))
        grain.process(
            buffer,
            profile.grainAmount,
            profile.grainSize,
            profile.blackAndWhite,
            stableSeed(profile, metadata, width, height),
            isoScale
        )
        softness.process(buffer, profile.softnessAmount)
        stage("grain_softness", stageStart)

        stageStart = System.nanoTime()
        val developed = FilmBufferToBitmap.convert(buffer)
        stage("encode_srgb", stageStart)
        Log.d(TAG, "physical_complete elapsedMs=${elapsedMs(started)}")
        return Result(negative, developed)
    }

    private fun applyShadowLift(buffer: FilmPixelBuffer, amount: Float) {
        val lift = amount.coerceIn(-0.25f, 0.5f)
        if (lift == 0f) return
        for (i in 0 until buffer.size) {
            val weight = (1f - buffer.luminance(i).coerceIn(0f, 1f)).pow(2)
            val delta = lift * weight * 0.16f
            buffer.r[i] = (buffer.r[i] + delta).coerceAtLeast(0f)
            buffer.g[i] = (buffer.g[i] + delta).coerceAtLeast(0f)
            buffer.b[i] = (buffer.b[i] + delta).coerceAtLeast(0f)
        }
    }

    private fun applyVignette(buffer: FilmPixelBuffer, amount: Float) {
        val strength = amount.coerceIn(0f, 1f) * 0.28f
        if (strength <= 0f) return
        val centerX = (buffer.width - 1) * 0.5f
        val centerY = (buffer.height - 1) * 0.5f
        val maxDistance = sqrt(centerX * centerX + centerY * centerY).coerceAtLeast(1f)
        for (y in 0 until buffer.height) for (x in 0 until buffer.width) {
            val i = y * buffer.width + x
            val dx = x - centerX
            val dy = y - centerY
            val radius = sqrt(dx * dx + dy * dy) / maxDistance
            val scale = 1f - strength * radius.pow(2.4f)
            buffer.r[i] *= scale
            buffer.g[i] *= scale
            buffer.b[i] *= scale
        }
    }

    private fun stableSeed(profile: FilmProfile, metadata: CaptureMetadata?, width: Int, height: Int): Long {
        var seed = 1125899906842597L
        fun mix(value: Any?) { seed = seed * 31L + (value?.hashCode() ?: 0) }
        mix(profile.id)
        mix(metadata?.dateTimeOriginal)
        mix(metadata?.exposureTime)
        mix(metadata?.iso)
        mix(width)
        mix(height)
        return seed
    }

    private fun stage(name: String, started: Long) = Log.d(TAG, "stage=$name elapsedMs=${elapsedMs(started)}")
    private fun elapsedMs(started: Long): Long = (System.nanoTime() - started) / 1_000_000L

    private companion object { const val TAG = "FilmPhysicalPipeline" }
}
