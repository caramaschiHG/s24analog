package com.roll24.film.processors

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class VignetteProcessor {
    
    /**
     * Applies vignette effect (darkening towards edges)
     */
    fun apply(bitmap: Bitmap, amount: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val maxDistance = sqrt(centerX * centerX + centerY * centerY)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                
                // Calculate distance from center
                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt(dx * dx + dy * dy)
                val normalizedDistance = distance / maxDistance
                
                // Vignette strength increases towards edges
                val vignetteStrength = normalizedDistance * normalizedDistance * amount
                
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                var r = (pixel shr 16) and 0xFF
                var g = (pixel shr 8) and 0xFF
                var b = pixel and 0xFF
                
                // Darken based on vignette strength
                val darkenFactor = 1f - vignetteStrength
                r = max(0, (r * darkenFactor).toInt())
                g = max(0, (g * darkenFactor).toInt())
                b = max(0, (b * darkenFactor).toInt())
                
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
