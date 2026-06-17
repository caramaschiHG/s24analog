package com.roll24.camera

/**
 * Feature flags for experimental capture paths.
 *
 * These flags are deliberately conservative (default false) so that work-in-progress
 * features cannot accidentally break the main capture flow.
 */
object FeatureFlags {
    /**
     * When true, [CameraSensorScanner] will invoke [RawCapabilityProbe] for every
     * back camera and log the result. This is an investigation-only path; RAW
     * capture is never enabled in the main pipeline by this flag.
     */
    val enableRawInvestigation: Boolean = false
}
