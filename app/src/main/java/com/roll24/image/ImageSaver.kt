package com.roll24.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.roll24.film.FilmProfile
import com.roll24.image.CaptureMetadata
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object ImageSaver {
    
    private const val TAG = "ImageSaver"
    private val sequence = AtomicInteger(0)

    data class AnalogSaveResult(
        val rawUri: Uri?,
        val negativeUri: Uri?,
        val developedUri: Uri?,
        val thumbnailUri: Uri?,
        val galleryUri: Uri?,
        val label: String
    )

    fun buildUniqueLabel(profile: FilmProfile): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val safeFilmId = profile.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val suffix = sequence.updateAndGet { current -> (current + 1) % 10_000 }
        return "ROLL24_${timestamp}_${"%04d".format(suffix)}_$safeFilmId"
    }

    fun saveAnalogCapture(
        context: Context,
        negative: Bitmap,
        developed: Bitmap,
        profile: FilmProfile,
        rawBytes: ByteArray? = null,
        label: String = buildUniqueLabel(profile),
        metadata: CaptureMetadata? = null
    ): AnalogSaveResult? {
        val rawUri = rawBytes?.let {
            saveBytes(
                context = context,
                bytes = it,
                filename = "${label}_raw.dng",
                relativeFolder = "Roll24/Raw",
                mimeType = "image/x-adobe-dng",
                description = "Roll24 raw sensor capture - ${profile.name}"
            )
        }

        // SINGLE photo visible in the user's gallery: the developed JPG with EXIF.
        val galleryUri = saveBitmap(
            context = context,
            bitmap = developed,
            filename = "${label}.jpg",
            relativeFolder = "Roll24",
            description = "Roll24 - ${profile.name}",
            metadata = metadata
        )

        // Negative and thumbnail stay in app-private storage so Roll24's own UI can
        // show them, but they do NOT clutter the user's system gallery.
        val negativeUri = saveBitmapToAppPrivate(
            context = context,
            bitmap = negative,
            filename = "${label}_negative.jpg",
            subFolder = "negatives"
        )

        val thumbnailUri = saveBitmapToAppPrivate(
            context = context,
            bitmap = createThumbnail(developed),
            filename = "${label}_thumb.jpg",
            subFolder = "thumbs"
        )

        if (galleryUri == null && rawUri == null) return null

        return AnalogSaveResult(
            rawUri = rawUri,
            negativeUri = negativeUri,
            // developedUri intentionally aliases the gallery file - the developed
            // photo IS the gallery photo (avoid saving the same bitmap twice).
            developedUri = galleryUri,
            thumbnailUri = thumbnailUri,
            galleryUri = galleryUri,
            label = label
        )
    }

    private fun saveBitmapToAppPrivate(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        subFolder: String
    ): Uri? {
        return try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            val targetDir = File(baseDir, subFolder)
            if (!targetDir.exists()) targetDir.mkdirs()
            val file = File(targetDir, filename)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap to app-private: $filename", e)
            null
        }
    }
    
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

    private fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        relativeFolder: String,
        description: String,
        metadata: CaptureMetadata? = null
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveBitmapToMediaStore(context, bitmap, filename, relativeFolder, description, metadata)
            } else {
                saveBitmapToPublicPictures(context, bitmap, filename, relativeFolder, metadata)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap: $filename", e)
            null
        }
    }

    private fun saveBytes(
        context: Context,
        bytes: ByteArray,
        filename: String,
        relativeFolder: String,
        mimeType: String,
        description: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$relativeFolder")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                    put(MediaStore.Images.Media.DESCRIPTION, description)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.also {
                    resolver.openOutputStream(it)?.use { outputStream -> outputStream.write(bytes) }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val targetDir = File(picturesDir, relativeFolder)
                if (!targetDir.exists()) targetDir.mkdirs()
                val file = File(targetDir, filename)
                file.writeBytes(bytes)
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bytes: $filename", e)
            null
        }
    }

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val maxSide = 512f
        val scale = minOf(maxSide / bitmap.width, maxSide / bitmap.height, 1f)
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        relativeFolder: String,
        description: String,
        metadata: CaptureMetadata? = null
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$relativeFolder")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            put(MediaStore.Images.Media.DESCRIPTION, description)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return uri?.also {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            metadata?.let { data -> writeExifToUri(context, it, data) }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
            Log.d(TAG, "Image saved to gallery: $it")
        }
    }

    private fun saveBitmapToPublicPictures(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        relativeFolder: String,
        metadata: CaptureMetadata? = null
    ): Uri? {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val targetDir = File(picturesDir, relativeFolder)
        if (!targetDir.exists()) targetDir.mkdirs()

        val file = File(targetDir, filename)
        file.outputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }

        metadata?.let { data -> writeExifToFile(file, data) }

        return Uri.fromFile(file)
    }

    private fun writeExifToUri(context: Context, uri: Uri, metadata: CaptureMetadata) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                applyExifMetadata(exif, metadata)
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write EXIF to $uri", e)
        }
    }

    private fun writeExifToFile(file: File, metadata: CaptureMetadata) {
        try {
            val exif = ExifInterface(file)
            applyExifMetadata(exif, metadata)
            exif.saveAttributes()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write EXIF to ${file.absolutePath}", e)
        }
    }

    private fun applyExifMetadata(exif: ExifInterface, metadata: CaptureMetadata) {
        metadata.iso?.let { exif.setAttribute(ExifInterface.TAG_ISO_SPEED, it.toString()) }
        metadata.exposureTime?.let { exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, it) }
        metadata.focalLengthMm?.let { exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it.toString()) }
        metadata.aperture?.let { exif.setAttribute(ExifInterface.TAG_F_NUMBER, it.toString()) }
        metadata.whiteBalanceMode?.let { exif.setAttribute(ExifInterface.TAG_WHITE_BALANCE, it) }
        metadata.dateTimeOriginal?.let { exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, it) }
    }
    
    /**
     * Deletes a MediaStore URI created by this app.
     * Returns true if deleted.
     */
    fun deleteUri(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete uri: $uri", e)
            false
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
