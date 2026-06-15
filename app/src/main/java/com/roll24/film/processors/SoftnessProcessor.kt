package com.roll24.film.processors

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint

class SoftnessProcessor {
    
    /**
     * Applies softness effect (subtle blur to reduce digital sharpness)
     */
    fun apply(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return bitmap
        
        val radius = amount * 3f // Max 3px blur
        
        return blurBitmap(bitmap, radius)
    }
    
    private fun blurBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        paint.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}
