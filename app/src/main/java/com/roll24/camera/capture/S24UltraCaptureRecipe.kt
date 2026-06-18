package com.roll24.camera.capture

/**
 * Preferred capture output format strategy for the S24 Ultra.
 * Ordered by preference: the engine tries the first available format.
 */
enum class PreferredCaptureFormat {
    /** RAW first, then YUV, then JPEG as fallback. Maximum data preservation. */
    RAW_THEN_YUV_THEN_JPEG,
    /** RAW+JPEG dual output (for simultaneous preview JPEG and RAW processing). */
    RAW_JPEG,
    /** YUV only — good for real-time processing without RAW overhead. */
    YUV,
    /** JPEG only — lightweight, lowest control. */
    JPEG
}

/**
 * ISP processing preference for noise reduction and edge enhancement.
 */
enum class ProcessingPreference {
    /** Completely disabled — preserve raw signal. */
    OFF,
    /** Minimal processing — gentle denoise, no sharpening. */
    MINIMAL,
    /** Fast mode — acceptable quality with low latency. */
    FAST,
    /** High quality — full ISP pipeline (not recommended for Roll24). */
    HIGH_QUALITY,
    /** Device default — whatever Samsung decides. */
    DEVICE_DEFAULT
}

/**
 * Tonemap mode preference — controls how the sensor's linear data is mapped to output.
 */
enum class TonemapPreference {
    /** Neutral linear mapping — no tone curve applied. Best for Roll24's own pipeline. */
    RAW_NEUTRAL,
    /** Custom contrast curve. */
    CONTRAST_CURVE,
    /** Fast automatic tonemap. */
    FAST,
    /** High quality automatic tonemap. */
    HIGH_QUALITY,
    /** Device default. */
    DEVICE_DEFAULT
}

/**
 * Complete capture recipe describing how to configure Camera2 for a specific
 * film profile + lens + settings combination on the S24 Ultra.
 *
 * This is a contract/intent — the actual Camera2 request builder reads this
 * and maps it to CaptureRequest parameters.
 */
data class S24UltraCaptureRecipe(
    val lensLabel: String,
    val preferredFormat: PreferredCaptureFormat,
    val targetIso: Int?,
    val targetExposureNanos: Long?,
    val exposureBiasEv: Float,
    val whiteBalanceKelvin: Int?,
    val noiseReductionPreference: ProcessingPreference,
    val edgePreference: ProcessingPreference,
    val tonemapPreference: TonemapPreference,
    val maxCreativeZoom: Float
)
