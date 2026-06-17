package com.roll24.camera

import com.roll24.sensor.S24UltraSensorDossier
import com.roll24.sensor.SensorNoiseModel

/**
 * Sensor-specific capture recommendations for the film-emulation pipeline.
 *
 * These hints are derived from the matched [com.roll24.sensor.SensorSpec] and
 * [com.roll24.sensor.NoiseModel]. They are intentionally advisory: they are
 * logged on camera initialization but never override user manual settings.
 */
data class CaptureHint(
    val recommendedBaseIso: Int,
    val maxUsableIso: Int,
    val preferredOutputSize: String,
    val whiteBalanceKelvinHint: Int
) {
    override fun toString(): String =
        "CaptureHint(baseIso=$recommendedBaseIso, maxIso=$maxUsableIso, " +
            "output=$preferredOutputSize, wb=${whiteBalanceKelvinHint}K)"
}

object SensorCaptureHints {

    private const val DEFAULT_WB_K = 5500

    /**
     * Returns capture hints for a [SensorProfile].
     *
     * - For known S24 Ultra sensors, returns sensor-specific base ISO, max ISO,
     *   preferred output size and white-balance hint.
     * - For unknown sensors, returns safe generic defaults so the pipeline can
     *   continue without a matched dossier spec.
     */
    fun forProfile(profile: SensorProfile): CaptureHint {
        val spec = profile.matchedDossierSpec
        val model = spec?.modelName
        val label = spec?.lensLabel

        return when {
            label == "1x" || model == "ISOCELL HP2" -> CaptureHint(
                recommendedBaseIso = SensorNoiseModel.HP2.baseIso,
                maxUsableIso = 1600,
                preferredOutputSize = "50MP binned",
                whiteBalanceKelvinHint = DEFAULT_WB_K
            )
            label == "0.6x" || model == "IMX563" -> CaptureHint(
                recommendedBaseIso = SensorNoiseModel.IMX563.baseIso,
                maxUsableIso = 3200,
                preferredOutputSize = "12MP",
                whiteBalanceKelvinHint = DEFAULT_WB_K
            )
            label == "3x" || model == "IMX754" -> CaptureHint(
                recommendedBaseIso = SensorNoiseModel.IMX754.baseIso,
                maxUsableIso = 3200,
                preferredOutputSize = "10MP",
                whiteBalanceKelvinHint = DEFAULT_WB_K
            )
            label == "5x" || model == "IMX854" -> CaptureHint(
                recommendedBaseIso = SensorNoiseModel.IMX854.baseIso,
                maxUsableIso = 3200,
                preferredOutputSize = "50MP",
                whiteBalanceKelvinHint = DEFAULT_WB_K
            )
            else -> genericDefaults()
        }
    }

    private fun genericDefaults(): CaptureHint = CaptureHint(
        recommendedBaseIso = 50,
        maxUsableIso = 3200,
        preferredOutputSize = "Native",
        whiteBalanceKelvinHint = DEFAULT_WB_K
    )
}
