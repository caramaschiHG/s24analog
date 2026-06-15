package com.roll24.film.processors

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

class BloomProcessor {
    
    /**
     * Applies bloom effect (soft glow around bright areas)
     */
    fun apply(bitmap: Bitmap, amount: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Create a copy for the bloom layer
        val bloomBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Extract bright areas
        val pixels = IntArray(width * height)
        bloomBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val threshold = 180 // Brightness threshold for bloom
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            val lum = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            
            if (lum > threshold) {
                // Keep bright areas
                val intensity = (lum - threshold) / 75f
                pixels[i] = (a shl 24) or 
                           (min(255, (r * intensity).toInt()) shl 16) or 
                           (min(255, (g * intensity).toInt()) shl 8) or 
                           min(255, (b * intensity).toInt())
            } else {
                // Dark areas become transparent
                pixels[i] = 0
            }
        }
        
        bloomBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        
        // Blur the bloom layer
        val blurredBloom = blurBitmap(bloomBitmap, 12f * amount)
        
        // Blend with original
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        blendScreen(result, blurredBloom, amount * 0.6f)
        
        bloomBitmap.recycle()
        blurredBloom.recycle()
        
        return result
    }
    
    private fun blurBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        paint.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    private fun blendScreen(base: Bitmap, overlay: Bitmap, opacity: Float) {
        val width = base.width
        val height = base.height
        val basePixels = IntArray(width * height)
        val overlayPixels = IntArray(width * height)
        
        base.getPixels(basePixels, 0, width, 0, 0, width, height)
        overlay.getPixels(overlayPixels, 0, width, 0, 0, width, height)
        
        for (i in basePixels.indices) {
            val basePixel = basePixels[i]
            val overlayPixel = overlayPixels[i]
            
            val a = (basePixel shr 24) and 0xFF
            val baseR = (basePixel shr 16) and 0xFF
            val baseG = (basePixel shr 8) and 0xFF
            val baseB = basePixel and 0xFF
            
            val overlayR = (overlayPixel shr 16) and 0xFF
            val overlayG = (overlayPixel shr 8) and 0xFF
            val overlayB = overlayPixel and 0xFF
            
            // Screen blend mode: 1 - (1-base) * (1-overlay)
            val r = min(255, baseR + overlayR - (baseR * overlayR) / 255)
            val g = min(255, baseG + overlayG - (baseG * overlayG) / 255)
            val b = min(255, baseB + overlayB - (baseB * overlayB) / 255)
            
            // Apply opacity
            val finalR = (baseR + (r - baseR) * opacity).toInt()
            val finalG = (baseG + (g - baseG) * opacity).toInt()
            val finalB = (baseB + (b - baseB) * opacity).toInt()
            
            basePixels[i] = (a shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
        }
        
        base.setPixels(basePixels, 0, width, 0, 0, width, height)
    }
}
