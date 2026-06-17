package com.roll24.sensor

/**
 * Estimated per-sensor noise/ISO model for the Samsung Galaxy S24 Ultra rear sensors.
 *
 * All values are approximate estimates derived from public sensor specifications
 * (pixel pitch, optical format, CFA) and typical smartphone sensor behavior.
 * They are not calibrated measurements.
 */
data class NoiseModel(
    val baseIso: Int,
    val maxRecommendedIso: Int,
    val readNoiseEv: Float,
    val photonResponseNonUniformity: Float
)

/**
 * Estimated noise/ISO models for each known S24 Ultra rear sensor.
 *
 * These constants are intended to feed the film-emulation pipeline (e.g. grain
 * strength, shadow noise, and ISO recommendations) and must be replaced with
 * measured calibration data if/when it becomes available.
 */
object SensorNoiseModel {

    /**
     * Estimated noise model for the Samsung ISOCELL HP2 (1x wide, 200 MP).
     * Base ISO assumes tetra-pixel (4x4) high-sensitivity binning mode.
     */
    val HP2 = NoiseModel(
        baseIso = 12,
        maxRecommendedIso = 1600,
        readNoiseEv = 0.004f,
        photonResponseNonUniformity = 0.008f
    )

    /**
     * Estimated noise model for the Sony IMX563 (0.6x ultra-wide, 12 MP).
     */
    val IMX563 = NoiseModel(
        baseIso = 50,
        maxRecommendedIso = 3200,
        readNoiseEv = 0.003f,
        photonResponseNonUniformity = 0.006f
    )

    /**
     * Estimated noise model for the Sony IMX754 (3x telephoto, 10 MP).
     */
    val IMX754 = NoiseModel(
        baseIso = 50,
        maxRecommendedIso = 3200,
        readNoiseEv = 0.0045f,
        photonResponseNonUniformity = 0.009f
    )

    /**
     * Estimated noise model for the Sony IMX854 (5x periscope, 50 MP).
     */
    val IMX854 = NoiseModel(
        baseIso = 50,
        maxRecommendedIso = 1600,
        readNoiseEv = 0.005f,
        photonResponseNonUniformity = 0.01f
    )

    private val byModelName: Map<String, NoiseModel> = mapOf(
        "ISOCELL HP2" to HP2,
        "IMX563" to IMX563,
        "IMX754" to IMX754,
        "IMX854" to IMX854
    )

    private val byLensLabel: Map<String, NoiseModel> = mapOf(
        "1x" to HP2,
        "0.6x" to IMX563,
        "3x" to IMX754,
        "5x" to IMX854
    )

    /**
     * Returns the estimated [NoiseModel] for the given [SensorSpec], matching by
     * [SensorSpec.modelName] first, then by [SensorSpec.lensLabel].
     * Returns null if no known model matches.
     */
    fun forSensor(spec: SensorSpec): NoiseModel? =
        byModelName[spec.modelName] ?: byLensLabel[spec.lensLabel]
}
