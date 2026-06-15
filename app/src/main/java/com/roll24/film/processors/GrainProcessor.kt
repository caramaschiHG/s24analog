package com.roll24.film.processors

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GrainProcessor {
    
    /**
     * Applies procedural film grain
     */
    fun apply(bitmap: Bitmap, amount: Float, size: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val grainIntensity = amount * 50 // Max grain variation
        
        // Apply grain to each pixel
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                var r = (pixel shr 16) and 0xFF
                var g = (pixel shr 8) and 0xFF
                var b = pixel and 0xFF
                
                // Generate grain (luminance-based - more grain in midtones)
                val lum = (r * 0.299 + g * 0.587 + b * 0.114) / 255f
                val grainFactor = 1f - kotlin.math.abs(lum - 0.5f) * 2f // Peak at midtones
                
                val grain = (Random.nextFloat() - 0.5f) * grainIntensity * grainFactor
                
                r = min(255, max(0, (r + grain).toInt()))
                g = min(255, max(0, (g + grain).toInt()))
                b = min(255, max(0, (b + grain).toInt()))
                
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        
        // TODO: Apply size-based blur for larger grain
        // For now, grain is pixel-level
        
        return bitmap
    }
}
