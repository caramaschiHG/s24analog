package com.roll24.camera

import android.graphics.Bitmap
import com.roll24.image.BitmapTransforms
import com.roll24.image.CaptureMetadata

enum class InputEncoding {
    SRGB
}

/** Complete payload from one exposure. RAW_DNG is only used when [rawFrame] is real Bayer data. */
data class RawCaptureResult(
    val source: CaptureSource,
    val encoding: InputEncoding,
    val jpegBytes: ByteArray?,
    val rawBytes: ByteArray?,
    val rawFrame: RawFrame?,
    val metadata: CaptureMetadata?,
    val width: Int,
    val height: Int,
    val fallbackReason: String? = null
) {
    fun toJpegBitmap(): Bitmap? = jpegBytes?.let(BitmapTransforms::decodeJpegWithExif)
}
