package com.roll24.film

import com.roll24.film.processors.HdChannelParams
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
    val halationAmount: Float = 0.1f,
    val bloomAmount: Float = 0.05f,
    val vignetteAmount: Float = 0.1f,
    val softnessAmount: Float = 0.05f,
    val blackAndWhite: Boolean = false,
    val pushPullStopsMin: Float = -2f,
    val pushPullStopsMax: Float = +2f
) {
    companion object {
        // T17: Kodak Portra 400 — warm pastel skin tones, low contrast, fine grain
        val PORTRA_400 = FilmStock(
            id = "portra_400",
            name = "Kodak Portra 400",
            description = "Warm pastel color negative film with natural skin tones and wide latitude",
            filmType = FilmType.C41,
            baseIso = 400,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.10f, shoulder = 0.20f, gamma = 1.02f, dMin = 0.01f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.05f, saturation = -0.05f, warmth = 0.12f, tint = 0.02f,
                shadowLift = 0.18f, highlightCompression = 0.30f, blackPoint = 0.01f
            ),
            halationColor = android.graphics.Color.parseColor("#E8A66E"),
            grainBaseAmount = 0.10f,
            grainSize = 0.85f,
            halationAmount = 0.12f,
            bloomAmount = 0.08f,
            vignetteAmount = 0.08f,
            softnessAmount = 0.10f
        )

        // T18: Kodak Ektar 100 — saturated colors, strong reds, high contrast
        val EKTAR_100 = FilmStock(
            id = "ektar_100",
            name = "Kodak Ektar 100",
            description = "Ultra-saturated color negative film with strong reds and high contrast",
            filmType = FilmType.C41,
            baseIso = 100,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.06f, shoulder = 0.22f, gamma = 1.12f, dMin = 0.02f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.25f, saturation = 0.25f, warmth = 0.08f, tint = 0.04f,
                shadowLift = 0.06f, highlightCompression = 0.22f, blackPoint = 0.02f
            ),
            halationColor = android.graphics.Color.parseColor("#C23B22"),
            grainBaseAmount = 0.06f,
            grainSize = 0.70f,
            halationAmount = 0.08f,
            bloomAmount = 0.04f,
            vignetteAmount = 0.06f,
            softnessAmount = 0.04f
        )

        // T19: Fuji Pro 400H — cool pastel tones, natural skin, soft highlights
        val PRO_400H = FilmStock(
            id = "pro_400h",
            name = "Fuji Pro 400H",
            description = "Cool pastel color negative film with soft highlights and natural skin",
            filmType = FilmType.C41,
            baseIso = 400,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.10f, shoulder = 0.22f, gamma = 1.02f, dMin = 0.01f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.04f, saturation = -0.10f, warmth = -0.02f, tint = -0.03f,
                shadowLift = 0.16f, highlightCompression = 0.32f, blackPoint = 0.01f
            ),
            halationColor = android.graphics.Color.parseColor("#A8C8D8"),
            grainBaseAmount = 0.11f,
            grainSize = 0.90f,
            halationAmount = 0.10f,
            bloomAmount = 0.07f,
            vignetteAmount = 0.07f,
            softnessAmount = 0.12f
        )

        // T20: Fuji Velvia 50 — vivid E6 slide film
        val VELVIA_50 = FilmStock(
            id = "velvia_50",
            name = "Fuji Velvia 50",
            description = "Extremely vivid E6 slide film with high contrast and narrow latitude",
            filmType = FilmType.E6,
            baseIso = 50,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.04f, shoulder = 0.15f, gamma = 1.18f, dMin = 0.03f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.35f, saturation = 0.40f, warmth = 0.0f, tint = 0.0f,
                shadowLift = 0.0f, highlightCompression = 0.15f, blackPoint = 0.03f
            ),
            halationColor = android.graphics.Color.parseColor("#B03060"),
            grainBaseAmount = 0.05f,
            grainSize = 0.65f,
            halationAmount = 0.0f,
            bloomAmount = 0.0f,
            vignetteAmount = 0.04f,
            softnessAmount = 0.0f
        )

        // T21: CineStill 800T — tungsten C41 with strong red halation
        val CINESTILL_800T = FilmStock(
            id = "cinestill_800t",
            name = "CineStill 800T",
            description = "Tungsten-balanced color negative film with strong red halation",
            filmType = FilmType.C41,
            baseIso = 800,
            whiteBalanceKelvin = 3200,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.08f, shoulder = 0.28f, gamma = 1.09f, dMin = 0.02f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.18f, saturation = 0.05f, warmth = -0.10f, tint = 0.05f,
                shadowLift = 0.04f, highlightCompression = 0.28f, blackPoint = 0.02f
            ),
            halationColor = android.graphics.Color.parseColor("#FF1A1A"),
            halationThreshold = 0.65f,
            grainBaseAmount = 0.28f,
            grainSize = 1.10f,
            halationAmount = 0.45f,
            bloomAmount = 0.18f,
            vignetteAmount = 0.12f,
            softnessAmount = 0.06f
        )

        // T22: Kodak Vision3 250D — motion-picture color negative, warm cinematic
        val VISION3_250D = FilmStock(
            id = "vision3_250d",
            name = "Kodak Vision3 250D",
            description = "Motion picture color negative film with warm cinematic skintones",
            filmType = FilmType.VISION3,
            baseIso = 250,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.08f, shoulder = 0.25f, gamma = 1.06f, dMin = 0.015f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.12f, saturation = 0.08f, warmth = 0.10f, tint = 0.02f,
                shadowLift = 0.10f, highlightCompression = 0.35f, blackPoint = 0.015f
            ),
            halationColor = android.graphics.Color.parseColor("#C9A227"),
            grainBaseAmount = 0.14f,
            grainSize = 0.95f,
            halationAmount = 0.12f,
            bloomAmount = 0.06f,
            vignetteAmount = 0.08f,
            softnessAmount = 0.05f
        )

        // T23: Kodak Gold 200 — warm nostalgic consumer film
        val GOLD_200 = FilmStock(
            id = "gold_200",
            name = "Kodak Gold 200",
            description = "Warm consumer color negative film with yellow-orange cast",
            filmType = FilmType.C41,
            baseIso = 200,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.08f, shoulder = 0.20f, gamma = 1.07f, dMin = 0.02f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.14f, saturation = 0.10f, warmth = 0.28f, tint = 0.04f,
                shadowLift = 0.10f, highlightCompression = 0.20f, blackPoint = 0.02f
            ),
            halationColor = android.graphics.Color.parseColor("#E6A817"),
            grainBaseAmount = 0.18f,
            grainSize = 1.0f,
            halationAmount = 0.08f,
            bloomAmount = 0.05f,
            vignetteAmount = 0.10f,
            softnessAmount = 0.05f
        )

        // T24: Fujicolor C200 — cool green/cyan casual film
        val FUJICOLOR_C200 = FilmStock(
            id = "fujicolor_c200",
            name = "Fujicolor C200",
            description = "Cool consumer color negative film with green-cyan cast",
            filmType = FilmType.C41,
            baseIso = 200,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.08f, shoulder = 0.18f, gamma = 1.05f, dMin = 0.02f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.10f, saturation = 0.05f, warmth = -0.08f, tint = -0.06f,
                shadowLift = 0.08f, highlightCompression = 0.18f, blackPoint = 0.02f
            ),
            halationColor = android.graphics.Color.parseColor("#4A9B9B"),
            grainBaseAmount = 0.20f,
            grainSize = 1.05f,
            halationAmount = 0.06f,
            bloomAmount = 0.04f,
            vignetteAmount = 0.08f,
            softnessAmount = 0.04f
        )

        // T25: Ilford HP5 Plus 400 — soft B&W negative
        val HP5_PLUS_400 = FilmStock(
            id = "hp5_plus_400",
            name = "Ilford HP5 Plus 400",
            description = "Soft black and white negative film with wide latitude",
            filmType = FilmType.BLACK_AND_WHITE,
            baseIso = 400,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.10f, shoulder = 0.22f, gamma = 1.07f, dMin = 0.025f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.15f, saturation = 0f, warmth = 0f, tint = 0f,
                shadowLift = 0.08f, highlightCompression = 0.22f, blackPoint = 0.025f
            ),
            halationColor = android.graphics.Color.parseColor("#8A8A8A"),
            grainBaseAmount = 0.25f,
            grainSize = 1.10f,
            halationAmount = 0.10f,
            bloomAmount = 0.05f,
            vignetteAmount = 0.08f,
            softnessAmount = 0.08f,
            blackAndWhite = true
        )

        // T26: Kodak Tri-X 400 — high contrast B&W negative
        val TRI_X_400 = FilmStock(
            id = "tri_x_400",
            name = "Kodak Tri-X 400",
            description = "High contrast black and white film with punchy grain",
            filmType = FilmType.BLACK_AND_WHITE,
            baseIso = 400,
            curveParams = HdCurveParams(
                base = HdChannelParams(toe = 0.06f, shoulder = 0.15f, gamma = 1.16f, dMin = 0.05f, dMax = 1f)
            ),
            colorResponse = ColorResponseParams(
                contrast = 0.32f, saturation = 0f, warmth = 0f, tint = 0f,
                shadowLift = 0.02f, highlightCompression = 0.15f, blackPoint = 0.05f
            ),
            halationColor = android.graphics.Color.parseColor("#4A4A4A"),
            grainBaseAmount = 0.35f,
            grainSize = 1.25f,
            halationAmount = 0.12f,
            bloomAmount = 0.06f,
            vignetteAmount = 0.10f,
            softnessAmount = 0.06f,
            blackAndWhite = true
        )

        val CANONICAL_STOCKS = listOf(
            PORTRA_400, EKTAR_100, PRO_400H, VELVIA_50, CINESTILL_800T,
            VISION3_250D, GOLD_200, FUJICOLOR_C200, HP5_PLUS_400, TRI_X_400
        )
    }
}
