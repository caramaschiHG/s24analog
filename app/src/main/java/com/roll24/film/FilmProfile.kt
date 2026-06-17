package com.roll24.film

import android.graphics.Color
import com.roll24.film.processors.HdCurveParams

data class FilmProfile(
    val id: String,
    val name: String,
    val description: String,

    // Capture settings
    val baseIso: Int,
    val exposureCompensation: Float,
    val whiteBalanceKelvin: Int,

    // Tone and color
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val tint: Float,

    // Shadows and highlights
    val shadowLift: Float,
    val highlightCompression: Float,
    val blackPoint: Float,

    // Film effects
    val grainAmount: Float,
    val grainSize: Float,
    val halationAmount: Float,
    val bloomAmount: Float,
    val vignetteAmount: Float,
    val softnessAmount: Float,

    // Special
    val blackAndWhite: Boolean,

    // Physical-film metadata (added in Wave 3)
    val filmType: FilmType = FilmType.C41,
    val filmStockId: String = "",
    val hdCurveParams: HdCurveParams? = null,
    val halationColor: Int = Color.parseColor("#FF5522"),
    val halationThreshold: Float = 0.78f,
    val bloomThreshold: Float = 0.70f
) {
    companion object {
        // Default neutral profile
        val NEUTRAL = FilmProfile(
            id = "neutral",
            name = "Neutral",
            description = "No film simulation",
            baseIso = 400,
            exposureCompensation = 0f,
            whiteBalanceKelvin = 5500,
            contrast = 0f,
            saturation = 0f,
            warmth = 0f,
            tint = 0f,
            shadowLift = 0f,
            highlightCompression = 0f,
            blackPoint = 0f,
            grainAmount = 0f,
            grainSize = 1f,
            halationAmount = 0f,
            bloomAmount = 0f,
            vignetteAmount = 0f,
            softnessAmount = 0f,
            blackAndWhite = false,
            filmType = FilmType.E6,
            filmStockId = "neutral",
            hdCurveParams = null
        )

        /**
         * Builds a [FilmProfile] from a canonical [FilmStock].
         *
         * [pushPullStops] is applied as a small exposure/compensation shift.
         */
        fun fromStock(
            stock: FilmStock,
            pushPullStops: Float = 0f
        ): FilmProfile {
            return FilmProfile(
                id = stock.id,
                name = stock.name,
                description = stock.description,
                baseIso = stock.baseIso,
                exposureCompensation = stock.exposureCompensation + pushPullStops * 0.5f,
                whiteBalanceKelvin = stock.whiteBalanceKelvin,
                contrast = stock.colorResponse.contrast,
                saturation = stock.colorResponse.saturation,
                warmth = stock.colorResponse.warmth,
                tint = stock.colorResponse.tint,
                shadowLift = stock.colorResponse.shadowLift,
                highlightCompression = stock.colorResponse.highlightCompression,
                blackPoint = stock.colorResponse.blackPoint,
                grainAmount = stock.grainBaseAmount,
                grainSize = stock.grainSize,
                halationAmount = stock.halationAmount,
                bloomAmount = stock.bloomAmount,
                vignetteAmount = stock.vignetteAmount,
                softnessAmount = stock.softnessAmount,
            blackAndWhite = stock.blackAndWhite,
            filmType = stock.filmType,
            filmStockId = stock.id,
            hdCurveParams = stock.curveParams,
            halationColor = stock.halationColor,
            halationThreshold = stock.halationThreshold,
            bloomThreshold = stock.bloomThreshold
        )
        }
    }
}
