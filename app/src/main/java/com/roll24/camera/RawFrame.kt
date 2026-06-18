package com.roll24.camera

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.LensShadingMap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.roll24.image.CaptureMetadata
import java.nio.ByteOrder

enum class CfaPattern {
    RGGB,
    GRBG,
    GBRG,
    BGGR;

    /**
     * 2x2 matrix description for logging.
     * E.g. for GBRG: "G B\nR G"
     */
    fun matrixDescription(): String = when (this) {
        RGGB -> "R G\nG B"
        GRBG -> "G R\nB G"
        GBRG -> "G B\nR G"
        BGGR -> "B G\nG R"
    }
}

/**
 * Typed white balance gains. Never indexed by CFA position.
 * Always accessed by color channel to prevent CFA-order mismatch.
 */
data class WhiteBalanceGains(
    val red: Float,
    val greenEven: Float,
    val greenOdd: Float,
    val blue: Float
) {
    val greenAverage: Float get() = (greenEven + greenOdd) * 0.5f

    /** Safe gain lookup by color constant (RED=0, GREEN=1, BLUE=2). */
    fun forColor(color: Int): Float = when (color) {
        0 -> red       // RED
        2 -> blue      // BLUE
        else -> greenAverage // GREEN
    }

    override fun toString(): String =
        "WB(r=%.4f gEven=%.4f gOdd=%.4f b=%.4f)".format(red, greenEven, greenOdd, blue)
}

/** Immutable copy of a RAW_SENSOR frame and the metadata needed to render it. */
data class RawFrame(
    val width: Int,
    val height: Int,
    val samples: ShortArray,
    val cfaPattern: CfaPattern,
    val blackLevels: FloatArray,
    val whiteLevel: Float,
    val whiteBalanceGains: WhiteBalanceGains,
    val colorTransform: FloatArray,
    val lensShading: FloatArray?,
    val lensShadingWidth: Int,
    val lensShadingHeight: Int,
    val rotationDegrees: Int,
    val metadata: CaptureMetadata
) {
    init {
        require(samples.size == width * height)
        require(blackLevels.size == 4)
        require(colorTransform.size == 9)
    }

    companion object {
        private const val TAG = "RawFrame"

        @OptIn(ExperimentalGetImage::class)
        fun fromImageProxy(
            image: ImageProxy,
            characteristics: CameraCharacteristics,
            result: CaptureResult
        ): RawFrame {
            require(image.format == ImageFormat.RAW_SENSOR)
            val plane = image.planes.single()
            val buffer = plane.buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            val samples = ShortArray(image.width * image.height)
            for (y in 0 until image.height) {
                val rowOffset = y * plane.rowStride
                for (x in 0 until image.width) {
                    samples[y * image.width + x] = buffer.getShort(rowOffset + x * plane.pixelStride)
                }
            }

            val blackPattern = characteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN)
            val black = FloatArray(4) { index ->
                blackPattern?.getOffsetForIndex(index % 2, index / 2)?.toFloat() ?: 0f
            }

            val gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)
            val whiteBalance = WhiteBalanceGains(
                red = gains?.red ?: 1f,
                greenEven = gains?.greenEven ?: 1f,
                greenOdd = gains?.greenOdd ?: 1f,
                blue = gains?.blue ?: 1f
            )

            val cfa = characteristics.cfaPattern()

            val transform = result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)?.toFloatArray()
                ?: characteristics.get(CameraCharacteristics.SENSOR_FORWARD_MATRIX1)
                    ?.toFloatArray()
                    ?.let(::xyzToSrgb)
                ?: IDENTITY_MATRIX.copyOf()
            val shadingMap = result.get(CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP)

            // ─── Diagnostic logging ──────────────────────────────────────────────
            Log.i(TAG, "RawFrame cfaPattern=${cfa.name} $whiteBalance")
            Log.i(TAG, "CFA matrix:\n${cfa.matrixDescription()}")
            Log.i(TAG, "ColorTransform:\n${formatMatrix(transform)}")
            Log.i(TAG, "BlackLevels=[${black.joinToString()}] WhiteLevel=${
                characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL) ?: 1023
            }")
            // ─────────────────────────────────────────────────────────────────────

            return RawFrame(
                width = image.width,
                height = image.height,
                samples = samples,
                cfaPattern = cfa,
                blackLevels = black,
                whiteLevel = (characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL) ?: 1023).toFloat(),
                whiteBalanceGains = whiteBalance,
                colorTransform = transform,
                lensShading = shadingMap?.toFloatArray(),
                lensShadingWidth = shadingMap?.columnCount ?: 0,
                lensShadingHeight = shadingMap?.rowCount ?: 0,
                rotationDegrees = image.imageInfo.rotationDegrees,
                metadata = CaptureMetadata.fromCamera2(result, image.width, image.height)
            )
        }

        private fun CameraCharacteristics.cfaPattern(): CfaPattern = when (
            get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
        ) {
            CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG -> CfaPattern.GRBG
            CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG -> CfaPattern.GBRG
            CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR -> CfaPattern.BGGR
            else -> CfaPattern.RGGB
        }

        private fun ColorSpaceTransform.toFloatArray(): FloatArray {
            // Android API: getElement(column, row). We store row-major.
            return FloatArray(9) { index ->
                val column = index % 3
                val row = index / 3
                val rational = getElement(column, row)
                rational.numerator.toFloat() / rational.denominator.toFloat()
            }
        }

        private fun xyzToSrgb(sensorToXyz: FloatArray): FloatArray {
            val xyzToSrgb = floatArrayOf(
                3.2406f, -1.5372f, -0.4986f,
                -0.9689f, 1.8758f, 0.0415f,
                0.0557f, -0.2040f, 1.0570f
            )
            return FloatArray(9) { index ->
                val row = index / 3
                val column = index % 3
                xyzToSrgb[row * 3] * sensorToXyz[column] +
                    xyzToSrgb[row * 3 + 1] * sensorToXyz[3 + column] +
                    xyzToSrgb[row * 3 + 2] * sensorToXyz[6 + column]
            }
        }

        private fun LensShadingMap.toFloatArray(): FloatArray = FloatArray(
            rowCount * columnCount * 4
        ) { index ->
            val channel = index % 4
            val cell = index / 4
            getGainFactor(channel, cell % columnCount, cell / columnCount)
        }

        private fun formatMatrix(m: FloatArray): String {
            require(m.size == 9)
            return "[ %+.4f  %+.4f  %+.4f\n  %+.4f  %+.4f  %+.4f\n  %+.4f  %+.4f  %+.4f ]".format(
                m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8]
            )
        }

        private val IDENTITY_MATRIX = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
    }
}

data class RawRenderMetrics(
    val elapsedMillis: Long,
    val sourceMegapixels: Float,
    val clippedPixelRatio: Float
)

data class RawRenderResult(
    val bitmap: android.graphics.Bitmap,
    val metadata: CaptureMetadata,
    val metrics: RawRenderMetrics
)
