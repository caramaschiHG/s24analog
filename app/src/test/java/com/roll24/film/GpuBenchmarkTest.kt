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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Benchmark comparing CPU vs GPU execution of the H&D curve and color adjustment
 * stages across processing resolutions.
 *
 * Run with:
 *   ./gradlew testDebugUnitTest --tests "com.roll24.film.GpuBenchmarkTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class GpuBenchmarkTest {

    private val evidenceDir = File("C:/Users/SylvianAliceCaramasc/Desktop/24mm cam/.sisyphus/evidence").apply { mkdirs() }

    @After
    fun restoreDefaults() {
        FeatureFlags.useGpuHdCurve = false
        FeatureFlags.useGpuColorAdjust = false
    }

    @Test
    fun benchmarkCpuVsGpuAcrossResolutions() {
        val engine = FilmDevelopmentEngine()
        val profile = FilmProfileRepository.getProfile("portra_400")
            ?: FilmProfileRepository.getDefaultProfile()
        val labSettings = FilmLabSettings()

        val inputWidth = 256
        val inputHeight = 256
        val input = createDeterministicBitmap(inputWidth, inputHeight)

        val resolutions = listOf(
            ProcessingResolution.FULL,
            ProcessingResolution.HALF,
            ProcessingResolution.QUARTER
        )

        val configs = listOf(
            ConfigLabel("CPU", useGpuHdCurve = false, useGpuColorAdjust = false),
            ConfigLabel("GPU-HD-only", useGpuHdCurve = true, useGpuColorAdjust = false),
            ConfigLabel("GPU-color-only", useGpuHdCurve = false, useGpuColorAdjust = true),
            ConfigLabel("GPU-both", useGpuHdCurve = true, useGpuColorAdjust = true)
        )

        val warmUpIterations = 3
        val measuredIterations = 5

        val results = mutableListOf<BenchmarkResult>()

        for (resolution in resolutions) {
            val targetSize = resolution.computeTargetSize(input.width, input.height)

            for (config in configs) {
                FeatureFlags.useGpuHdCurve = config.useGpuHdCurve
                FeatureFlags.useGpuColorAdjust = config.useGpuColorAdjust

                // Warm-up iterations: do not measure.
                repeat(warmUpIterations) {
                    val output = engine.develop(
                        input,
                        profile,
                        labSettings,
                        sensorProfile = null,
                        resolution = resolution
                    )
                    assertOutputValid(output, targetSize, config, resolution)
                }

                // Measured iterations.
                val timesNs = LongArray(measuredIterations)
                repeat(measuredIterations) { i ->
                    val start = System.nanoTime()
                    val output = engine.develop(
                        input,
                        profile,
                        labSettings,
                        sensorProfile = null,
                        resolution = resolution
                    )
                    val elapsedNs = System.nanoTime() - start
                    timesNs[i] = elapsedNs
                    assertOutputValid(output, targetSize, config, resolution)
                }

                val medianNs = timesNs.sorted()[timesNs.size / 2]
                val medianMs = medianNs / 1_000_000.0

                results.add(
                    BenchmarkResult(
                        resolution = resolution,
                        config = config,
                        medianMs = medianMs,
                        targetSize = targetSize
                    )
                )

                // Reduce cross-config noise by requesting GC before the next configuration.
                System.gc()
            }
        }

        val report = buildReport(
            profile = profile,
            inputWidth = inputWidth,
            inputHeight = inputHeight,
            results = results
        )

        File(evidenceDir, "raw-gpu-task-9-benchmark-report.txt").writeText(report)

        // The benchmark passes if every configuration completed without crashing and
        // produced the expected output dimensions.
        assertTrue("All benchmark configurations must complete", results.size == resolutions.size * configs.size)
    }

    private fun createDeterministicBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val v = ((x * 4) + y) % 256
                bitmap.setPixel(x, y, Color.rgb(v, v / 2, 255 - v))
            }
        }
        return bitmap
    }

    private fun assertOutputValid(
        output: Bitmap,
        targetSize: Pair<Int, Int>,
        config: ConfigLabel,
        resolution: ProcessingResolution
    ) {
        assertEquals(
            "Width must match target for ${config.name} @ $resolution",
            targetSize.first,
            output.width
        )
        assertEquals(
            "Height must match target for ${config.name} @ $resolution",
            targetSize.second,
            output.height
        )
    }

    private fun buildReport(
        profile: FilmProfile,
        inputWidth: Int,
        inputHeight: Int,
        results: List<BenchmarkResult>
    ): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        return buildString {
            appendLine("T9 GPU benchmark - CPU vs GPU H&D curve + color adjustment")
            appendLine("Date/time: $timestamp")
            appendLine("Environment: Robolectric / host JVM")
            appendLine("Input size: ${inputWidth}x${inputHeight}")
            appendLine("Profile: ${profile.id} (${profile.name})")
            appendLine("Warm-up iterations: 3")
            appendLine("Measured iterations: 5")
            appendLine("Metric: median elapsed time in ms")
            appendLine()
            appendLine("Table:")
            appendLine(String.format("%-12s %-15s %-15s %-12s %-12s", "resolution", "GPU-HD", "GPU-color", "config", "median ms"))
            appendLine("-".repeat(70))

            for (result in results) {
                appendLine(
                    String.format(
                        "%-12s %-15s %-15s %-12s %-12.3f",
                        result.resolution.name,
                        result.config.useGpuHdCurve,
                        result.config.useGpuColorAdjust,
                        result.config.name,
                        result.medianMs
                    )
                )
            }

            appendLine()
            appendLine("Notes:")
            appendLine("- On Robolectric AGSL falls back to CPU, so numbers reflect CPU fallback unless run on a real API 31+ device.")
            appendLine("- On a real device, GPU paths may show upload/read-back overhead; this benchmark captures end-to-end pipeline time.")
            appendLine("- Lower resolutions (HALF, QUARTER) reduce per-pixel work and memory pressure.")
        }
    }

    private data class BenchmarkResult(
        val resolution: ProcessingResolution,
        val config: ConfigLabel,
        val medianMs: Double,
        val targetSize: Pair<Int, Int>
    )

    private data class ConfigLabel(
        val name: String,
        val useGpuHdCurve: Boolean,
        val useGpuColorAdjust: Boolean
    )
}
