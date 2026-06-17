package com.roll24.sensor

import kotlin.math.abs

data class SensorSpec(
    val lensLabel: String,
    val manufacturer: String,
    val modelName: String,
    val resolutionMp: Float,
    val pixelSizeUm: Float,
    val opticalFormat: String,
    val cfaPattern: String,
    val binningModes: List<String>,
    val hasOis: Boolean,
    val technologies: List<String>,
    val nativeFocalLengthMm: Float,
    val equivalentFocalLengthMm: Float,
    val aperture: Float?,
    val isoRange: String?,
    val exposureRange: String?
)

object S24UltraSensorDossier {
    private val entries: List<SensorSpec> = listOf(
        SensorSpec(
            lensLabel = "1x",
            manufacturer = "Samsung",
            modelName = "ISOCELL HP2",
            resolutionMp = 200f,
            pixelSizeUm = 0.6f,
            opticalFormat = "1/1.3\"",
            cfaPattern = "RGGB",
            binningModes = listOf("1x1", "4x4", "16x1"),
            hasOis = true,
            technologies = listOf("Tetra²pixel", "Super QPD"),
            nativeFocalLengthMm = 6.3f,
            equivalentFocalLengthMm = 23f,
            aperture = 1.7f,
            isoRange = "12-3200",
            exposureRange = "1/12000s - 10s"
        ),
        SensorSpec(
            lensLabel = "0.6x",
            manufacturer = "Sony",
            modelName = "IMX563",
            resolutionMp = 12f,
            pixelSizeUm = 1.4f,
            opticalFormat = "1/2.55\"",
            cfaPattern = "RGGB",
            binningModes = listOf("1x1"),
            hasOis = false,
            technologies = listOf("Dual Pixel"),
            nativeFocalLengthMm = 2.2f,
            equivalentFocalLengthMm = 13f,
            aperture = 2.2f,
            isoRange = "50-3200",
            exposureRange = "1/12000s - 10s"
        ),
        SensorSpec(
            lensLabel = "3x",
            manufacturer = "Sony",
            modelName = "IMX754",
            resolutionMp = 10f,
            pixelSizeUm = 1.12f,
            opticalFormat = "1/3.52\"",
            cfaPattern = "RGGB",
            binningModes = listOf("1x1"),
            hasOis = true,
            technologies = listOf("Dual Pixel"),
            nativeFocalLengthMm = 7.9f,
            equivalentFocalLengthMm = 69f,
            aperture = 2.4f,
            isoRange = "50-3200",
            exposureRange = "1/12000s - 10s"
        ),
        SensorSpec(
            lensLabel = "5x",
            manufacturer = "Sony",
            modelName = "IMX854",
            resolutionMp = 50f,
            pixelSizeUm = 0.7f,
            opticalFormat = "1/2.52\"",
            cfaPattern = "RGGB",
            binningModes = listOf("1x1", "2x2"),
            hasOis = true,
            technologies = listOf("Super PD"),
            nativeFocalLengthMm = 18.6f,
            equivalentFocalLengthMm = 115f,
            aperture = 3.4f,
            isoRange = "50-3200",
            exposureRange = "1/12000s - 10s"
        )
    )

    fun byLensLabel(label: String): SensorSpec? =
        entries.find { it.lensLabel.equals(label, ignoreCase = true) }

    fun byFocalLengthMm(focal: Float): SensorSpec? =
        entries.find { abs(it.nativeFocalLengthMm - focal) <= 0.2f }

    val allRear: List<SensorSpec> = entries
}
