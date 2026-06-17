package com.roll24.sensor

/**
 * 3x3 row-major spectral response matrix.
 *
 * Each of [r], [g], [b] represents a row that maps camera RGB (RGGB Bayer
 * inferred) toward a film-like RGB working space.
 *
 * @property isEstimated When true, the values are modeled/estimated rather than
 *                       instrumentally measured.
 */
data class SpectralMatrix(
    val r: FloatArray,
    val g: FloatArray,
    val b: FloatArray,
    val isEstimated: Boolean = true
) {
    init {
        require(r.size == 3 && g.size == 3 && b.size == 3) {
            "SpectralMatrix rows must contain exactly 3 values"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpectralMatrix) return false
        return r.contentEquals(other.r) &&
            g.contentEquals(other.g) &&
            b.contentEquals(other.b) &&
            isEstimated == other.isEstimated
    }

    override fun hashCode(): Int {
        var result = r.contentHashCode()
        result = 31 * result + g.contentHashCode()
        result = 31 * result + b.contentHashCode()
        result = 31 * result + isEstimated.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        appendLine("SpectralMatrix(isEstimated=$isEstimated)")
        appendLine("  [${r.joinToString()}]")
        appendLine("  [${g.joinToString()}]")
        appendLine("  [${b.joinToString()}]")
    }
}

/**
 * Estimated RGGB -> film RGB spectral response matrices for the Samsung Galaxy
 * S24 Ultra rear camera sensors.
 *
 * TODO: All values below are hand-tuned estimates. They are NOT scientifically
 *       measured spectral response curves. Replace with instrumented data when
 *       available.
 */
object SensorSpectralResponse {

    /** ISOCELL HP2 - 1x wide (200 MP). */
    val HP2 = SpectralMatrix(
        r = floatArrayOf(0.95f, 0.03f, 0.02f),
        g = floatArrayOf(0.02f, 0.94f, 0.04f),
        b = floatArrayOf(0.01f, 0.05f, 0.94f),
        isEstimated = true
    )

    /** Sony IMX563 - 0.6x ultrawide (12 MP). */
    val IMX563 = SpectralMatrix(
        r = floatArrayOf(0.96f, 0.02f, 0.02f),
        g = floatArrayOf(0.03f, 0.93f, 0.04f),
        b = floatArrayOf(0.02f, 0.04f, 0.94f),
        isEstimated = true
    )

    /** Sony IMX754 - 3x telephoto (10 MP). */
    val IMX754 = SpectralMatrix(
        r = floatArrayOf(0.94f, 0.04f, 0.02f),
        g = floatArrayOf(0.02f, 0.95f, 0.03f),
        b = floatArrayOf(0.01f, 0.04f, 0.95f),
        isEstimated = true
    )

    /** Sony IMX854 - 5x periscope telephoto (50 MP). */
    val IMX854 = SpectralMatrix(
        r = floatArrayOf(0.93f, 0.05f, 0.02f),
        g = floatArrayOf(0.03f, 0.94f, 0.03f),
        b = floatArrayOf(0.02f, 0.05f, 0.93f),
        isEstimated = true
    )

    /**
     * Returns the estimated spectral response matrix for the given [spec],
     * matching against [SensorSpec.modelName] or [SensorSpec.lensLabel].
     */
    fun forSensor(spec: SensorSpec): SpectralMatrix? = when {
        spec.modelName.contains("HP2", ignoreCase = true) ||
            spec.lensLabel == "1x" -> HP2
        spec.modelName.contains("IMX563", ignoreCase = true) ||
            spec.lensLabel == "0.6x" -> IMX563
        spec.modelName.contains("IMX754", ignoreCase = true) ||
            spec.lensLabel == "3x" -> IMX754
        spec.modelName.contains("IMX854", ignoreCase = true) ||
            spec.lensLabel == "5x" -> IMX854
        else -> null
    }
}
