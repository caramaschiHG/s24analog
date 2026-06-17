package com.roll24.film

/**
 * Feature flags for the film development pipeline.
 *
 * These flags control the new Wave-3 physical emulation steps and provide an
 * A/B toggle between the new pipeline and the legacy path. Defaults are
 * conservative: the new pipeline is enabled, but spectral normalization only
 * runs when a matched S24 Ultra sensor is present.
 */
object FeatureFlags {
    /**
     * Master switch for the Wave-3 physical film emulation pipeline.
     *
     * When false, [FilmDevelopmentEngine.developLegacy] behavior is used:
     * spectral normalize, digital-look reduction, orange-mask removal and
     * ISO-aware/spectral grain are skipped.
     */
    var useNewPipeline: Boolean = true

    /**
     * Enables sensor-spectral normalization in the normalize step.
     *
     * Only has an effect when [useNewPipeline] is true and the passed
     * [com.roll24.camera.SensorProfile] has a matched S24 Ultra dossier spec.
     */
    var useSpectralNormalize: Boolean = true

    /**
     * Enables orange mask removal for C-41 and Vision3 negative films.
     */
    var useOrangeMaskRemoval: Boolean = true

    /**
     * Enables GPU-accelerated HD tone-curve processing.
     */
    var useGpuHdCurve: Boolean = false

    /**
     * Enables GPU-accelerated color adjustment processing.
     */
    var useGpuColorAdjust: Boolean = false
}
