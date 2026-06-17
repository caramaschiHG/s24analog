package com.roll24.film

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class GpuPipelineCombinationTest {

    private val evidenceDir = File("C:/Users/SylvianAliceCaramasc/Desktop/24mm cam/.sisyphus/evidence").apply { mkdirs() }

    @After
    fun restoreDefaults() {
        FeatureFlags.useGpuHdCurve = false
        FeatureFlags.useGpuColorAdjust = false
    }

    @Test
    fun fourCombinationsDoNotCrashAndPreserveDimensions() {
        val engine = FilmDevelopmentEngine()
        val profile = FilmProfileRepository.getProfile("portra_400")
            ?: FilmProfileRepository.getDefaultProfile()
        val input = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until input.height) {
            for (x in 0 until input.width) {
                val v = ((x * 4) + y) % 256
                input.setPixel(x, y, Color.rgb(v, v / 2, 255 - v))
            }
        }

        val combinations = listOf(
            false to false,
            true to false,
            false to true,
            true to true
        )

        val results = mutableMapOf<Pair<Boolean, Boolean>, Bitmap>()
        for ((useGpuHdCurve, useGpuColorAdjust) in combinations) {
            FeatureFlags.useGpuHdCurve = useGpuHdCurve
            FeatureFlags.useGpuColorAdjust = useGpuColorAdjust

            val output = engine.develop(input, profile, FilmLabSettings())

            assertEquals(
                "Dimensions must match for (useGpuHdCurve=$useGpuHdCurve, useGpuColorAdjust=$useGpuColorAdjust)",
                input.width,
                output.width
            )
            assertEquals(
                "Dimensions must match for (useGpuHdCurve=$useGpuHdCurve, useGpuColorAdjust=$useGpuColorAdjust)",
                input.height,
                output.height
            )
            results[useGpuHdCurve to useGpuColorAdjust] = output
        }

        val cpuOnly = results[false to false]!!
        val gpuColorOnly = results[false to true]!!
        val gpuHdOnly = results[true to false]!!
        val gpuBoth = results[true to true]!!

        val madCpuVsGpuColor = meanAbsoluteDifference(cpuOnly, gpuColorOnly)
        val madCpuVsGpuHd = meanAbsoluteDifference(cpuOnly, gpuHdOnly)
        val madCpuVsGpuBoth = meanAbsoluteDifference(cpuOnly, gpuBoth)

        File(evidenceDir, "raw-gpu-task-8-gpu-color-integration.txt").writeText(
            buildString {
                appendLine("T8 GPU color integration - four flag combinations")
                appendLine("Profile: ${profile.id} (${profile.name})")
                appendLine("Input dimensions: ${input.width}x${input.height}")
                combinations.forEach { (hd, color) ->
                    val output = results[hd to color]!!
                    appendLine("useGpuHdCurve=$hd, useGpuColorAdjust=$color -> ${output.width}x${output.height}")
                }
                appendLine("CPU-only vs GPU-color MAD: $madCpuVsGpuColor")
                appendLine("CPU-only vs GPU-H&D MAD: $madCpuVsGpuHd")
                appendLine("CPU-only vs GPU-both MAD: $madCpuVsGpuBoth")
                appendLine("GPU falls back to CPU in Robolectric, so differences should be near zero.")
            }
        )

        assertTrue(
            "CPU and GPU-color outputs should be nearly identical (Robolectric fallback)",
            madCpuVsGpuColor < 2.0f
        )
        assertTrue(
            "CPU and GPU-both outputs should be nearly identical (Robolectric fallback)",
            madCpuVsGpuBoth < 2.0f
        )
    }

    @Test
    fun blackAndWhiteSkipsGpuColorPath() {
        val engine = FilmDevelopmentEngine()
        val profile = FilmProfileRepository.getProfile("mono_press_400")
            ?: FilmProfileRepository.getDefaultProfile()

        FeatureFlags.useGpuColorAdjust = true

        val input = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        input.eraseColor(Color.rgb(128, 128, 128))

        val output = engine.develop(input, profile, FilmLabSettings())

        assertEquals(input.width, output.width)
        assertEquals(input.height, output.height)

        // A grayscale output should be neutral R == G == B (allowing tiny rounding drift).
        var maxChannelDiff = 0
        for (y in 0 until output.height) {
            for (x in 0 until output.width) {
                val pixel = output.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                maxChannelDiff = maxOf(maxChannelDiff, kotlin.math.abs(r - g), kotlin.math.abs(g - b), kotlin.math.abs(r - b))
            }
        }
        assertTrue("B&W conversion must leave R==G==B (max diff $maxChannelDiff)", maxChannelDiff <= 1)
    }

    private fun meanAbsoluteDifference(a: Bitmap, b: Bitmap): Float {
        require(a.width == b.width && a.height == b.height)
        var diff = 0L
        var count = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val pa = a.getPixel(x, y)
                val pb = b.getPixel(x, y)
                diff += kotlin.math.abs(Color.red(pa) - Color.red(pb))
                diff += kotlin.math.abs(Color.green(pa) - Color.green(pb))
                diff += kotlin.math.abs(Color.blue(pa) - Color.blue(pb))
                count += 3
            }
        }
        return if (count == 0) 0f else diff / count.toFloat()
    }
}
