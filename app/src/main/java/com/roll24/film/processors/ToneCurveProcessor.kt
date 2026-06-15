package com.roll24.film.processors

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

class ToneCurveProcessor {
    
    /**
     * Applies tone curve with contrast and black point adjustment
     */
    fun apply(bitmap: Bitmap, contrast: Float, blackPoint: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Build tone curve lookup table
        val curve = buildToneCurve(contrast, blackPoint)
        
        // Apply curve to each pixel
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            pixels[i] = (a shl 24) or 
                       (curve[r] shl 16) or 
                       (curve[g] shl 8) or 
                       curve[b]
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    /**
     * Compresses highlights to prevent clipping
     */
    fun compressHighlights(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return bitmap
        
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val threshold = (255 * (1f - amount)).toInt()
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF
            
            // Compress values above threshold
            if (r > threshold) r = threshold + ((r - threshold) * (255 - threshold) / (255 - threshold + 1))
            if (g > threshold) g = threshold + ((g - threshold) * (255 - threshold) / (255 - threshold + 1))
            if (b > threshold) b = threshold + ((b - threshold) * (255 - threshold) / (255 - threshold + 1))
            
            pixels[i] = (a shl 24) or 
                       (min(255, max(0, r)) shl 16) or 
                       (min(255, max(0, g)) shl 8) or 
                       min(255, max(0, b))
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    /**
     * Lifts shadows to reveal detail
     */
    fun liftShadows(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return bitmap
        
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val liftAmount = (amount * 50).toInt() // Lift up to 50 levels
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF
            
            // Lift shadows more than highlights (inverse curve)
            val lum = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            val liftFactor = 1f - (lum / 255f) // More lift for darker pixels
            
            r = min(255, (r + liftAmount * liftFactor).toInt())
            g = min(255, (g + liftAmount * liftFactor).toInt())
            b = min(255, (b + liftAmount * liftFactor).toInt())
            
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    private fun buildToneCurve(contrast: Float, blackPoint: Float): IntArray {
        val curve = IntArray(256)
        
        for (i in 0..255) {
            var value = i / 255f
            
            // Apply black point
            value = max(0f, value - blackPoint) / (1f - blackPoint)
            
            // Apply contrast (S-curve)
            value = applyContrast(value, contrast)
            
            curve[i] = (value * 255).toInt().coerceIn(0, 255)
        }
        
        return curve
    }
    
    private fun applyContrast(value: Float, contrast: Float): Float {
        if (contrast == 0f) return value
        
        // S-curve contrast adjustment
        val mid = 0.5f
        val factor = 1f + contrast
        
        return if (value < mid) {
            mid - (mid - value) * factor
        } else {
            mid + (value - mid) * factor
        }.coerceIn(0f, 1f)
    }
}
