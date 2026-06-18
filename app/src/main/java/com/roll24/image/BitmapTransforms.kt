package com.roll24.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.ByteArrayInputStream
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapTransforms {
    fun decodeJpegWithExif(bytes: ByteArray): Bitmap? {
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        return rotateFromExif(decoded, exif)
    }

    fun decodeJpegWithExif(file: File): Bitmap? {
        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val exif = ExifInterface(file.absolutePath)
        return rotateFromExif(decoded, exif)
    }

    private fun rotateFromExif(decoded: Bitmap, exif: ExifInterface): Bitmap {
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        return if (rotation == 0f) {
            decoded
        } else {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }
    }

    fun centerCropToAspect(bitmap: Bitmap, targetRatio: Float): Bitmap {
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val cropWidth: Int
        val cropHeight: Int

        if (currentRatio > targetRatio) {
            cropHeight = bitmap.height
            cropWidth = (cropHeight * targetRatio).roundToInt()
        } else {
            cropWidth = bitmap.width
            cropHeight = (cropWidth / targetRatio).roundToInt()
        }

        val left = max(0, (bitmap.width - cropWidth) / 2)
        val top = max(0, (bitmap.height - cropHeight) / 2)
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }
}
