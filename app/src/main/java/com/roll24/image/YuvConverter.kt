package com.roll24.image

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer

object YuvConverter {
    
    private const val TAG = "YuvConverter"
    
    /**
     * Converts YUV_420_888 Image to Bitmap
     */
    fun yuvToBitmap(image: Image): Bitmap? {
        return try {
            val width = image.width
            val height = image.height
            
            val yBuffer = image.planes[0].buffer // Y
            val uBuffer = image.planes[1].buffer // U
            val vBuffer = image.planes[2].buffer // V
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            // U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = android.graphics.YuvImage(
                nv21,
                ImageFormat.NV21,
                width,
                height,
                null
            )
            
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, out)
            val imageBytes = out.toByteArray()
            
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert YUV to Bitmap", e)
            null
        }
    }
    
    /**
     * Converts YUV_420_888 Image to Bitmap with reduced size for preview
     */
    fun yuvToBitmapScaled(image: Image, maxWidth: Int = 1024): Bitmap? {
        val fullBitmap = yuvToBitmap(image) ?: return null
        
        return if (fullBitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / fullBitmap.width
            val newHeight = (fullBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(fullBitmap, maxWidth, newHeight, true)
        } else {
            fullBitmap
        }
    }
}
