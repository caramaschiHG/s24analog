package com.roll24.image

import android.util.Log

/**
 * Detects image byte format by magic bytes.
 * Used to prevent saving JPEG bytes as .dng files.
 */
object ImageByteFormatDetector {
    private const val TAG = "ImageFormatDetector"

    enum class Kind {
        JPEG,
        DNG_TIFF,
        PNG,
        UNKNOWN
    }

    /**
     * Detect format from the first bytes of a byte array.
     * Returns UNKNOWN if the array is too small or doesn't match known signatures.
     */
    fun detect(bytes: ByteArray): Kind {
        if (bytes.size < 4) return Kind.UNKNOWN

        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) {
            return Kind.JPEG
        }

        // DNG/TIFF little-endian: 49 49 2A 00
        if (bytes[0] == 0x49.toByte() &&
            bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x2A.toByte() &&
            bytes[3] == 0x00.toByte()
        ) {
            return Kind.DNG_TIFF
        }

        // DNG/TIFF big-endian: 4D 4D 00 2A
        if (bytes[0] == 0x4D.toByte() &&
            bytes[1] == 0x4D.toByte() &&
            bytes[2] == 0x00.toByte() &&
            bytes[3] == 0x2A.toByte()
        ) {
            return Kind.DNG_TIFF
        }

        // PNG: 89 50 4E 47
        if (bytes.size >= 4 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte()
        ) {
            return Kind.PNG
        }

        return Kind.UNKNOWN
    }

    /**
     * Validate that raw bytes are actually DNG/TIFF before saving as .dng.
     * Logs a warning if the format doesn't match expectations.
     * Returns true if the bytes are DNG/TIFF.
     */
    fun validateDng(bytes: ByteArray): Boolean {
        val kind = detect(bytes)
        Log.d(TAG, "Validating DNG: detected=$kind size=${bytes.size} bytes")
        return when (kind) {
            Kind.DNG_TIFF -> true
            Kind.JPEG -> {
                Log.e(TAG, "JPEG bytes (${bytes.size} bytes) being treated as DNG! This is a bug.")
                false
            }
            else -> {
                Log.w(TAG, "Unknown format (${bytes.size} bytes) being treated as DNG")
                false
            }
        }
    }
}
