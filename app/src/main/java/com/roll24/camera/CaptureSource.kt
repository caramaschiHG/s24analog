package com.roll24.camera

/**
 * Identifies the upstream capture source used by the film lab.
 */
enum class CaptureSource {
    /** Standard compressed JPEG capture from the camera framework. */
    JPEG,

    /** Lossless raw sensor DNG capture. */
    RAW_DNG
}
