package com.roll24.spike.gpu

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.Log
import com.roll24.film.processors.HdChannelParams
import com.roll24.film.processors.HdCurveParams
import com.roll24.film.processors.HdCurveProcessor
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * GPU spike for the per-channel H&D curve LUT.
 *
 * Uses AGSL [RuntimeShader] (API 31+) to apply a 256-entry lookup table to each
 * RGB channel. If the runtime environment does not support AGSL, or if shader
 * creation fails for any reason, the spike transparently falls back to the
 * existing CPU [HdCurveProcessor].
 *
 * This class is intentionally isolated from [com.roll24.film.FilmDevelopmentEngine]
 * and is not wired into the production pipeline.
 */
class HdCurveGpuSpike {

    /**
     * Returns true when the device/runtime claims to support AGSL RuntimeShader.
     * Note that a true result does not guarantee a working GPU driver on
     * emulators or stubbed test environments.
     */
    fun isGpuAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Applies the H&D curve described by [params] to [bitmap].
     *
     * On API 31+ devices this path runs an AGSL fragment shader. On older
     * devices, or if shader setup fails, it delegates to [HdCurveProcessor].
     */
    fun apply(bitmap: Bitmap, params: HdCurveParams): Bitmap {
        if (!isGpuAvailable()) {
            return cpuFallback(bitmap, params, reason = "API ${Build.VERSION.SDK_INT} < 31")
        }
        return try {
            applyWithRuntimeShader(bitmap, params)
        } catch (t: Throwable) {
            Log.w(TAG, "RuntimeShader path failed, falling back to CPU", t)
            cpuFallback(bitmap, params, reason = t.message ?: "shader exception")
        }
    }

    /**
     * Benchmarks CPU vs GPU processing on the supplied bitmap.
     *
     * The CPU side always runs [HdCurveProcessor]. The GPU side attempts AGSL
     * and falls back to CPU when AGSL is unavailable, recording that fact in
     * [BenchmarkResult.gpuPathUsed].
     */
    fun benchmark(bitmap: Bitmap, params: HdCurveParams): BenchmarkResult {
        val cpuMs = measure {
            HdCurveProcessor().process(bitmap.copy(Bitmap.Config.ARGB_8888, true), params)
        }

        var gpuPathUsed = false
        var gpuError: String? = null
        val gpuMs = measure {
            if (!isGpuAvailable()) {
                gpuError = "API ${Build.VERSION.SDK_INT} < 31 (no RuntimeShader)"
                HdCurveProcessor().process(bitmap.copy(Bitmap.Config.ARGB_8888, true), params)
            } else {
                try {
                    applyWithRuntimeShader(bitmap, params)
                    gpuPathUsed = true
                } catch (t: Throwable) {
                    gpuError = t.message ?: t.javaClass.simpleName
                    HdCurveProcessor().process(bitmap.copy(Bitmap.Config.ARGB_8888, true), params)
                }
            }
        }

        return BenchmarkResult(
            cpuMs = cpuMs,
            gpuMs = gpuMs,
            gpuPathUsed = gpuPathUsed,
            gpuError = gpuError
        )
    }

    private fun applyWithRuntimeShader(bitmap: Bitmap, params: HdCurveParams): Bitmap {
        val lutBitmap = buildLutBitmap(params)

        val runtimeShader = RuntimeShader(AGSL_SHADER)
        val lutShader = BitmapShader(lutBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        runtimeShader.setInputShader("lutTexture", lutShader)

        val renderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, INPUT_UNIFORM)
        val renderNode = RenderNode("hdCurveGpu").apply {
            setPosition(0, 0, bitmap.width, bitmap.height)
            setRenderEffect(renderEffect)
        }

        val recordingCanvas = renderNode.beginRecording()
        recordingCanvas.drawBitmap(bitmap, 0f, 0f, null)
        renderNode.endRecording()

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawRenderNode(renderNode)
        return output
    }

    private fun cpuFallback(bitmap: Bitmap, params: HdCurveParams, reason: String): Bitmap {
        Log.i(TAG, "CPU fallback: $reason")
        return HdCurveProcessor().process(bitmap.copy(Bitmap.Config.ARGB_8888, true), params)
    }

    private fun buildLutBitmap(params: HdCurveParams): Bitmap {
        val redCurve = buildLutArray(params.red ?: params.base)
        val greenCurve = buildLutArray(params.green ?: params.base)
        val blueCurve = buildLutArray(params.blue ?: params.base)

        val lut = Bitmap.createBitmap(LUT_SIZE, 3, Bitmap.Config.ARGB_8888)
        for (i in 0 until LUT_SIZE) {
            lut.setPixel(i, 0, monoColor(redCurve[i]))
            lut.setPixel(i, 1, monoColor(greenCurve[i]))
            lut.setPixel(i, 2, monoColor(blueCurve[i]))
        }
        return lut
    }

    private fun buildLutArray(params: HdChannelParams): IntArray {
        val lut = IntArray(LUT_SIZE)
        val range = params.dMax - params.dMin
        val k = 4f + 8f * (params.toe + params.shoulder)
        val x0 = 0.5f + (params.shoulder - params.toe) * 0.25f

        for (i in 0 until LUT_SIZE) {
            val x = i / 255f
            val sigmoid = params.dMin + range / (1f + exp(-k * (x - x0)))
            val gammaCorrected = sigmoid.pow(1f / params.gamma.coerceAtLeast(0.1f))
            lut[i] = (gammaCorrected * 255f).roundToInt().coerceIn(0, 255)
        }
        return lut
    }

    private fun monoColor(value: Int): Int {
        return Color.argb(255, value, value, value)
    }

    private inline fun measure(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000L
    }

    /**
     * Result of a CPU/GPU benchmark run.
     */
    data class BenchmarkResult(
        val cpuMs: Long,
        val gpuMs: Long,
        val gpuPathUsed: Boolean,
        val gpuError: String?
    ) {
        fun summary(): String = buildString {
            appendLine("CPU time: ${cpuMs} ms")
            appendLine("GPU time: ${gpuMs} ms")
            appendLine("GPU path used: $gpuPathUsed")
            if (gpuError != null) appendLine("GPU error/fallback reason: $gpuError")
        }
    }

    companion object {
        private const val TAG = "HdCurveGpuSpike"
        private const val LUT_SIZE = 256
        private const val INPUT_UNIFORM = "inputImage"

        private const val AGSL_SHADER = """
            uniform shader $INPUT_UNIFORM;
            uniform shader lutTexture;

            half4 main(float2 coord) {
                half4 c = $INPUT_UNIFORM.eval(coord);
                half r = lutTexture.eval(float2(c.r * 255.0 + 0.5, 0.5)).r;
                half g = lutTexture.eval(float2(c.g * 255.0 + 0.5, 1.5)).r;
                half b = lutTexture.eval(float2(c.b * 255.0 + 0.5, 2.5)).r;
                return half4(r, g, b, c.a);
            }
        """
    }
}
