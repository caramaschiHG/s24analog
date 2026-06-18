package com.roll24.film

import android.graphics.Bitmap
import android.graphics.Color
import com.roll24.film.pipeline.BitmapToFilmBuffer
import com.roll24.film.pipeline.FilmBufferToBitmap
import com.roll24.film.pipeline.FilmBufferMath
import com.roll24.film.pipeline.FilmPixelBuffer
import com.roll24.film.pipeline.HighlightRolloffProcessor
import com.roll24.film.pipeline.PhysicalHalationProcessor
import com.roll24.film.pipeline.ScannerTransformProcessor
import com.roll24.film.pipeline.StructuredGrainProcessor
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PhysicalFilmPipelineTest {
    @After
    fun resetFlags() {
        FeatureFlags.usePhysicalPipeline = true
        FeatureFlags.useNewPipeline = true
    }

    @Test
    fun bitmapBoundaryRoundTripKeepsSrgbMidtone() {
        val input = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        input.setPixel(0, 0, Color.rgb(128, 96, 64))

        val output = FilmBufferToBitmap.convert(BitmapToFilmBuffer.convert(input)).getPixel(0, 0)

        assertTrue(kotlin.math.abs(128 - Color.red(output)) <= 1)
        assertTrue(kotlin.math.abs(96 - Color.green(output)) <= 1)
        assertTrue(kotlin.math.abs(64 - Color.blue(output)) <= 1)
        assertTrue(BitmapToFilmBuffer.convert(input).a == null)
    }

    @Test
    fun tiledBlurMatchesFullFrameAtTileBoundaries() {
        val width = 11
        val height = 205
        val radius = 4
        val input = FloatArray(width * height) { i -> ((i * 17) % 101) / 100f }
        val expected = FilmBufferMath.boxBlur(input, width, height, radius)
        val actual = FloatArray(input.size)

        FilmBufferMath.forEachBlurredTile(input, width, height, radius) { startY, endY, tile, sourceStartY ->
            for (y in startY until endY) {
                val tileOffset = (y - sourceStartY) * width
                tile.copyInto(actual, y * width, tileOffset, tileOffset + width)
            }
        }

        assertArrayEquals(expected, actual, 0.00001f)
    }

    @Test
    fun highlightRolloffCompressesLuminanceAndPreservesHueRatios() {
        val buffer = FilmPixelBuffer(1, 1, floatArrayOf(1.6f), floatArrayOf(0.8f), floatArrayOf(0.4f))
        val initialLuminance = buffer.luminance(0)

        HighlightRolloffProcessor().process(buffer, 1f)

        assertTrue(buffer.luminance(0) < initialLuminance)
        assertEquals(2f, buffer.r[0] / buffer.g[0], 0.0001f)
        assertEquals(2f, buffer.g[0] / buffer.b[0], 0.0001f)
    }

    @Test
    fun structuredGrainIsDeterministicAndGrainSizeChangesPattern() {
        val processor = StructuredGrainProcessor()
        fun render(size: Float, seed: Long): FloatArray {
            val buffer = constantBuffer(24, 24, 0.35f)
            processor.process(buffer, 0.8f, size, false, seed)
            return buffer.r
        }

        val first = render(1f, 42L)
        val repeated = render(1f, 42L)
        val larger = render(2f, 42L)

        assertArrayEquals(first, repeated, 0f)
        assertNotEquals(first.contentHashCode(), larger.contentHashCode())
    }

    @Test
    fun blackAndWhiteGrainUsesOneDensityPatternForAllChannels() {
        val buffer = constantBuffer(12, 12, 0.4f)

        StructuredGrainProcessor().process(buffer, 1f, 1f, true, 7L)

        assertArrayEquals(buffer.r, buffer.g, 0f)
        assertArrayEquals(buffer.g, buffer.b, 0f)
    }

    @Test
    fun scannerCalibratesOrangeBaseInsideNegativeDomain() {
        val buffer = FilmPixelBuffer(1, 1, floatArrayOf(0.4f), floatArrayOf(0.4f), floatArrayOf(0.4f))

        ScannerTransformProcessor().process(buffer, FilmType.C41)

        assertEquals(buffer.r[0], buffer.g[0], 0.0001f)
        assertEquals(buffer.g[0], buffer.b[0], 0.0001f)
    }

    @Test
    fun halationAffectsNeighborhoodMoreThanDistantPixels() {
        val source = constantBuffer(25, 25, 0f)
        for (y in 11..13) for (x in 11..13) {
            val i = y * source.width + x
            source.r[i] = 1.5f
            source.g[i] = 1.5f
            source.b[i] = 1.5f
        }
        val target = source.deepCopy()

        PhysicalHalationProcessor().process(target, source, 1f, 0.7f, Color.rgb(255, 80, 20))

        val near = 12 * target.width + 8
        val far = 0
        assertTrue(target.r[near] > target.r[far])
        assertTrue(target.r[near] > target.g[near])
    }

    @Test
    fun featureFlagRetainsLegacyFallbackAndRequestedDimensions() {
        val input = Bitmap.createBitmap(20, 12, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(110, 120, 130))
        }
        val engine = FilmDevelopmentEngine()
        val settings = FilmLabSettings(digitalLookReduction = 0f)

        FeatureFlags.usePhysicalPipeline = true
        val physical = engine.develop(input, FilmProfile.NEUTRAL, settings, ProcessingResolution.HALF)
        FeatureFlags.usePhysicalPipeline = false
        val legacy = engine.develop(input, FilmProfile.NEUTRAL, settings, ProcessingResolution.HALF)

        assertEquals(10, physical.width)
        assertEquals(6, physical.height)
        assertEquals(10, legacy.width)
        assertEquals(6, legacy.height)
    }

    private fun constantBuffer(width: Int, height: Int, value: Float): FilmPixelBuffer {
        val size = width * height
        return FilmPixelBuffer(
            width,
            height,
            FloatArray(size) { value },
            FloatArray(size) { value },
            FloatArray(size) { value }
        )
    }
}
