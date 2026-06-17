package com.roll24.gallery

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roll24.image.CaptureMetadata

enum class CaptureStatus {
    QUEUED,
    CAPTURING,
    DEVELOPING,
    SAVED,
    FAILED
}

@Entity(tableName = "captures")
data class CaptureRecord(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val filmId: String,
    val filmName: String,
    val lensId: String?,
    val lensLabel: String?,
    val aspect: String,
    val rawUri: String?,
    val negativeUri: String?,
    val developedUri: String?,
    val thumbnailUri: String?,
    val galleryUri: String?,
    val status: CaptureStatus,
    val error: String?,
    val usedFallback: Boolean,
    val metadata: CaptureMetadata? = null
)
