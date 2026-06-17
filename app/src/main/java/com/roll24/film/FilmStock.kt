package com.roll24.film

import com.roll24.film.processors.HdCurveParams

/**
 * Simplified color-response parameters for a film stock.
 *
 * These values map to the existing [FilmProfile] tone/color fields.
 */
data class ColorResponseParams(
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val shadowLift: Float = 0f,
    val highlightCompression: Float = 0f,
    val blackPoint: Float = 0f,
    val contrast: Float = 0f
)

/**
 * Canonical film-stock descriptor.
 *
 * A [FilmStock] bundles the physical and aesthetic parameters that define a
 * real-world film. It is the source of truth for building a [FilmProfile] via
 * [FilmProfile.fromStock].
 */
data class FilmStock(
    val id: String,
    val name: String,
    val description: String,
    val filmType: FilmType,
    val baseIso: Int,
    val exposureCompensation: Float = 0f,
    val whiteBalanceKelvin: Int = 5500,
    val curveParams: HdCurveParams = HdCurveParams(base = com.roll24.film.processors.HdChannelParams()),
    val colorResponse: ColorResponseParams = ColorResponseParams(),
    val halationColor: Int = android.graphics.Color.parseColor("#FF5522"),
    val halationThreshold: Float = 0.78f,
    val bloomThreshold: Float = 0.70f,
    val grainBaseAmount: Float = 0.15f,
    val grainSize: Float = 1f,
    val vignetteAmount: Float = 0.1f,
    val softnessAmount: Float = 0.05f,
    val blackAndWhite: Boolean = false,
    val pushPullStopsMin: Float = -2f,
    val pushPullStopsMax: Float = +2f
)
