package com.roll24.film

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
    val blackAndWhite: Boolean
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
            blackAndWhite = false
        )
    }
}
