package com.roll24.camera

import android.graphics.Color
import com.roll24.image.CaptureMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RawRendererTest {
    @Test
    fun `all CFA patterns render a neutral field without a color cast`() = runBlocking {
        CfaPattern.values().forEach { pattern ->
            val result = RawRenderer.render(uniformFrame(pattern, 512))
            val pixel = result.bitmap.getPixel(3, 3)
            assertTrue(pattern.name, kotlin.math.abs(Color.red(pixel) - Color.green(pixel)) <= 2)
            assertTrue(pattern.name, kotlin.math.abs(Color.green(pixel) - Color.blue(pixel)) <= 2)
            assertTrue(result.metrics.clippedPixelRatio == 0f)
        }
    }

    @Test
    fun `highlight rolloff retains channel detail above sensor white`() = runBlocking {
        val samples = ShortArray(36) { 1023.toShort() }
        val frame = baseFrame(CfaPattern.RGGB, samples).copy(
            whiteBalanceGains = floatArrayOf(2f, 2f, 2f, 2f)
        )

        val result = RawRenderer.render(frame)
        val value = Color.red(result.bitmap.getPixel(2, 2))

        assertTrue(value in 230..254)
        assertTrue(result.metrics.clippedPixelRatio > 0f)
    }

    private fun uniformFrame(pattern: CfaPattern, value: Int): RawFrame =
        baseFrame(pattern, ShortArray(36) { value.toShort() })

    private fun baseFrame(pattern: CfaPattern, samples: ShortArray) = RawFrame(
        width = 6,
        height = 6,
        samples = samples,
        cfaPattern = pattern,
        blackLevels = floatArrayOf(64f, 64f, 64f, 64f),
        whiteLevel = 1023f,
        whiteBalanceGains = floatArrayOf(1f, 1f, 1f, 1f),
        colorTransform = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        ),
        lensShading = null,
        lensShadingWidth = 0,
        lensShadingHeight = 0,
        rotationDegrees = 0,
        metadata = CaptureMetadata(null, null, null, null, null, null, 6, 6)
    )
}
