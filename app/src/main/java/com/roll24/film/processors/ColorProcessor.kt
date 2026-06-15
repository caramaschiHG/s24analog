package com.roll24.film.processors

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

class ColorProcessor {
    
    /**
     * Adjusts saturation, warmth, and tint
     */
    fun adjust(bitmap: Bitmap, saturation: Float, warmth: Float, tint: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF
            
            // Apply saturation
            if (saturation != 0f) {
                val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                val sat = 1f + saturation
                r = (gray + (r - gray) * sat).toInt()
                g = (gray + (g - gray) * sat).toInt()
                b = (gray + (b - gray) * sat).toInt()
            }
            
            // Apply warmth (red/yellow shift)
            if (warmth != 0f) {
                val warmAmount = (warmth * 30).toInt()
                r = min(255, r + warmAmount)
                g = min(255, g + (warmAmount * 0.5).toInt())
                b = max(0, b - (warmAmount * 0.3).toInt())
            }
            
            // Apply tint (green/magenta shift)
            if (tint != 0f) {
                val tintAmount = (tint * 20).toInt()
                g = min(255, g + tintAmount)
                r = max(0, r - (tintAmount * 0.3).toInt())
            }
            
            pixels[i] = (a shl 24) or 
                       (min(255, max(0, r)) shl 16) or 
                       (min(255, max(0, g)) shl 8) or 
                       min(255, max(0, b))
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    /**
     * Converts to black and white using luminance-preserving formula
     */
    fun convertToBlackAndWhite(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // Luminance-preserving conversion
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            
            pixels[i] = (a shl 24) or (gray shl 16) or (gray shl 8) or gray
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
