package com.roll24.camera

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.LensShadingMap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.roll24.image.CaptureMetadata
import java.nio.ByteOrder

enum class CfaPattern {
    RGGB,
    GRBG,
    GBRG,
    BGGR
}

/** Immutable copy of a RAW_SENSOR frame and the metadata needed to render it. */
data class RawFrame(
    val width: Int,
    val height: Int,
    val samples: ShortArray,
    val cfaPattern: CfaPattern,
    val blackLevels: FloatArray,
    val whiteLevel: Float,
    val whiteBalanceGains: FloatArray,
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
        require(whiteBalanceGains.size == 4)
        require(colorTransform.size == 9)
    }

    companion object {
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
            val whiteBalance = floatArrayOf(
                gains?.red ?: 1f,
                gains?.greenEven ?: 1f,
                gains?.greenOdd ?: 1f,
                gains?.blue ?: 1f
            )
            val transform = result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)?.toFloatArray()
                ?: characteristics.get(CameraCharacteristics.SENSOR_FORWARD_MATRIX1)
                    ?.toFloatArray()
                    ?.let(::xyzToSrgb)
                ?: IDENTITY_MATRIX.copyOf()
            val shadingMap = result.get(CaptureResult.STATISTICS_LENS_SHADING_CORRECTION_MAP)

            return RawFrame(
                width = image.width,
                height = image.height,
                samples = samples,
                cfaPattern = characteristics.cfaPattern(),
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
            return FloatArray(9) { index ->
                val rational = getElement(index % 3, index / 3)
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
