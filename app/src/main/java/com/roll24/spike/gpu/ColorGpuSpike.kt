package com.roll24.spike.gpu

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import com.roll24.film.processors.ColorProcessor

/**
 * GPU spike for the color adjustment step (saturation, warmth, tint).
 *
 * Uses AGSL [RuntimeShader] (API 31+) to apply the same color transforms that
 * [ColorProcessor] performs on the CPU. If the runtime environment does not
 * support AGSL, or if shader creation fails for any reason, the spike
 * transparently falls back to the existing CPU [ColorProcessor].
 *
 * This class is intentionally isolated from [com.roll24.film.FilmDevelopmentEngine]
 * and is not wired into the production pipeline.
 */
class ColorGpuSpike {

    /**
     * Returns true when the device/runtime claims to support AGSL RuntimeShader.
     * Note that a true result does not guarantee a working GPU driver on
     * emulators or stubbed test environments.
     */
    fun isGpuAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Applies the color adjustment described by [params] to [bitmap].
     *
     * On API 31+ devices this path runs an AGSL fragment shader. On older
     * devices, or if shader setup fails, it delegates to [ColorProcessor].
     */
    fun apply(bitmap: Bitmap, params: ColorGpuParams): Bitmap {
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
     * The CPU side always runs [ColorProcessor]. The GPU side attempts AGSL
     * and falls back to CPU when AGSL is unavailable, recording that fact in
     * [BenchmarkResult.gpuPathUsed].
     */
    fun benchmark(bitmap: Bitmap, params: ColorGpuParams): BenchmarkResult {
        val cpuMs = measure {
            ColorProcessor().adjust(
                bitmap.copy(Bitmap.Config.ARGB_8888, true),
                params.saturation,
                params.warmth,
                params.tint
            )
        }

        var gpuPathUsed = false
        var gpuError: String? = null
        val gpuMs = measure {
            if (!isGpuAvailable()) {
                gpuError = "API ${Build.VERSION.SDK_INT} < 31 (no RuntimeShader)"
                ColorProcessor().adjust(
                    bitmap.copy(Bitmap.Config.ARGB_8888, true),
                    params.saturation,
                    params.warmth,
                    params.tint
                )
            } else {
                try {
                    applyWithRuntimeShader(bitmap, params)
                    gpuPathUsed = true
                } catch (t: Throwable) {
                    gpuError = t.message ?: t.javaClass.simpleName
                    ColorProcessor().adjust(
                        bitmap.copy(Bitmap.Config.ARGB_8888, true),
                        params.saturation,
                        params.warmth,
                        params.tint
                    )
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

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private fun applyWithRuntimeShader(bitmap: Bitmap, params: ColorGpuParams): Bitmap {
        val runtimeShader = RuntimeShader(AGSL_SHADER).apply {
            setFloatUniform("saturation", params.saturation)
            setFloatUniform("warmth", params.warmth)
            setFloatUniform("tint", params.tint)
        }

        val renderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, INPUT_UNIFORM)
        val renderNode = RenderNode("colorGpu").apply {
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

    private fun cpuFallback(bitmap: Bitmap, params: ColorGpuParams, reason: String): Bitmap {
        Log.i(TAG, "CPU fallback: $reason")
        return ColorProcessor().adjust(
            bitmap.copy(Bitmap.Config.ARGB_8888, true),
            params.saturation,
            params.warmth,
            params.tint
        )
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
        private const val TAG = "ColorGpuSpike"
        private const val INPUT_UNIFORM = "inputImage"

        private const val AGSL_SHADER = """
            uniform shader $INPUT_UNIFORM;
            uniform float saturation;
            uniform float warmth;
            uniform float tint;

            half4 main(float2 coord) {
                half4 c = $INPUT_UNIFORM.eval(coord);
                half r = c.r;
                half g = c.g;
                half b = c.b;

                // Saturation: luminance-preserving scaling around Rec.601 luma.
                half gray = 0.299 * r + 0.587 * g + 0.114 * b;
                half sat = 1.0 + saturation;
                r = gray + (r - gray) * sat;
                g = gray + (g - gray) * sat;
                b = gray + (b - gray) * sat;

                // Warmth: red/yellow channel shift.
                half warmAmount = warmth * 30.0 / 255.0;
                r = min(1.0, r + warmAmount);
                g = min(1.0, g + warmAmount * 0.5);
                b = max(0.0, b - warmAmount * 0.3);

                // Tint: green/magenta channel shift.
                half tintAmount = tint * 20.0 / 255.0;
                g = min(1.0, g + tintAmount);
                r = max(0.0, r - tintAmount * 0.3);

                return half4(clamp(r, 0.0, 1.0), clamp(g, 0.0, 1.0), clamp(b, 0.0, 1.0), c.a);
            }
        """
    }
}

/**
 * Parameters for [ColorGpuSpike].
 *
 * @property saturation Saturation multiplier offset; 0.0 keeps the original
 *   saturation, positive values increase it, negative values decrease it.
 * @property warmth Warm/cool shift offset; positive values add warm red/yellow,
 *   negative values add cool blue.
 * @property tint Green/magenta shift offset; positive values add green,
 *   negative values add magenta.
 */
data class ColorGpuParams(
    val saturation: Float,
    val warmth: Float,
    val tint: Float
)
