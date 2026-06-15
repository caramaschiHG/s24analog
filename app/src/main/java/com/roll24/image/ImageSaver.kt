package com.roll24.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.roll24.film.FilmProfile
import java.text.SimpleDateFormat
import java.util.*

object ImageSaver {
    
    private const val TAG = "ImageSaver"
    
    /**
     * Saves bitmap to MediaStore (gallery)
     * Returns the URI of the saved image, or null if failed
     */
    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        profile: FilmProfile
    ): Uri? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "Roll24_${profile.id}_$timestamp.jpg"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Roll24")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                
                // Add metadata
                put(MediaStore.Images.Media.DESCRIPTION, "Captured with Roll24 - ${profile.name}")
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                
                Log.d(TAG, "Image saved to gallery: $it")
                it
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image to gallery", e)
            null
        }
    }
    
    /**
     * Saves bitmap to app-specific storage
     * Returns the file path, or null if failed
     */
    fun saveToAppStorage(
        context: Context,
        bitmap: Bitmap,
        profile: FilmProfile
    ): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "Roll24_${profile.id}_$timestamp.jpg"
            
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val roll24Dir = java.io.File(picturesDir, "Roll24")
            
            if (!roll24Dir.exists()) {
                roll24Dir.mkdirs()
            }
            
            val file = java.io.File(roll24Dir, filename)
            
            file.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
            
            Log.d(TAG, "Image saved to app storage: ${file.absolutePath}")
            file.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image to app storage", e)
            null
        }
    }
}
