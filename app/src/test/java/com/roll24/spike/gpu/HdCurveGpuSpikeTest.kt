package com.roll24.spike.gpu

import android.graphics.Bitmap
import android.graphics.Color
import com.roll24.film.processors.HdChannelParams
import com.roll24.film.processors.HdCurveParams
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class HdCurveGpuSpikeTest {

    private val evidenceDir = File(
        "C:/Users/SylvianAliceCaramasc/Desktop/24mm cam/.sisyphus/evidence"
    ).apply { mkdirs() }

    @Test
    fun task28GpuSpikeBenchmark() {
        val width = 1920
        val height = 1080
        val input = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Fill with a non-trivial gradient so the LUT is exercised across values.
        for (y in 0 until height step 16) {
            for (x in 0 until width step 16) {
                val r = (x * 255 / width).coerceIn(0, 255)
                val g = (y * 255 / height).coerceIn(0, 255)
                val b = ((x + y) * 255 / (width + height)).coerceIn(0, 255)
                val pixel = Color.rgb(r, g, b)
                for (yy in y until minOf(y + 16, height)) {
                    for (xx in x until minOf(x + 16, width)) {
                        input.setPixel(xx, yy, pixel)
                    }
                }
            }
        }

        val params = HdCurveParams(
            base = HdChannelParams(toe = 0.12f, shoulder = 0.25f, gamma = 1.05f),
            red = HdChannelParams(toe = 0.12f, shoulder = 0.25f, gamma = 1.05f),
            green = HdChannelParams(toe = 0.10f, shoulder = 0.20f, gamma = 1.00f),
            blue = HdChannelParams(toe = 0.08f, shoulder = 0.18f, gamma = 0.98f)
        )

        val spike = HdCurveGpuSpike()
        val result = spike.benchmark(input, params)

        // Sanity check: output can be produced even when GPU is unavailable.
        val output = spike.apply(input, params)
        check(output.width == width && output.height == height)

        File(evidenceDir, "task-28-gpu.txt").writeText(
            buildString {
                appendLine("T28 GPU acceleration spike (AGSL/OpenGL ES)")
                appendLine("Bitmap size: ${width}x${height}")
                appendLine("GPU available (API 31+): ${spike.isGpuAvailable()}")
                appendLine(result.summary())
                appendLine("Output dimensions: ${output.width}x${output.height}")
                appendLine("Output identical to input: ${output.sameAs(input)}")
            }
        )
    }
}
