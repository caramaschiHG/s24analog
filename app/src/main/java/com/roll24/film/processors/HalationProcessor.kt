package com.roll24.film.processors

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

class HalationProcessor {
    
    /**
     * Applies halation effect (red/orange glow around bright areas)
     * Simulates light scattering in film emulsion
     */
    fun apply(bitmap: Bitmap, amount: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Create a copy for the halation layer
        val halationBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Extract bright areas and shift to red/orange
        val pixels = IntArray(width * height)
        halationBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val threshold = 200 // Brightness threshold for halation
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            val lum = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            
            if (lum > threshold) {
                // Create red/orange halation
                val intensity = ((lum - threshold) / 55f) * amount
                val halationR = min(255, (r + intensity * 50).toInt())
                val halationG = min(255, (g + intensity * 20).toInt())
                val halationB = max(0, (b - intensity * 30).toInt())
                
                pixels[i] = (a shl 24) or (halationR shl 16) or (halationG shl 8) or halationB
            }
        }
        
        halationBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        
        // Blur the halation layer
        val blurredHalation = blurBitmap(halationBitmap, 8f * amount)
        
        // Blend with original
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        blendAdditive(result, blurredHalation, amount * 0.5f)
        
        halationBitmap.recycle()
        blurredHalation.recycle()
        
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
    
    private fun blendAdditive(base: Bitmap, overlay: Bitmap, opacity: Float) {
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
            
            val r = min(255, baseR + (overlayR * opacity).toInt())
            val g = min(255, baseG + (overlayG * opacity).toInt())
            val b = min(255, baseB + (overlayB * opacity).toInt())
            
            basePixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        
        base.setPixels(basePixels, 0, width, 0, 0, width, height)
    }
}
