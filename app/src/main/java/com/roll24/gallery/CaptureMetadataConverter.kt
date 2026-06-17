package com.roll24.gallery

import androidx.room.TypeConverter
import com.roll24.image.CaptureMetadata

object CaptureMetadataConverter {
    @TypeConverter
    @JvmStatic
    fun fromMetadata(metadata: CaptureMetadata?): String? = metadata?.toJson()

    @TypeConverter
    @JvmStatic
    fun toMetadata(json: String?): CaptureMetadata? = json?.let { CaptureMetadata.fromJson(it) }
}
