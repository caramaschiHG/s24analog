package com.roll24.film

object FilmProfileRepository {
    
    val profiles: List<FilmProfile> = listOf(
        // Warm Gold 200 - Warm color negative film, golden hour light
        FilmProfile(
            id = "warm_gold_200",
            name = "Warm Gold 200",
            description = "Warm color negative film with golden tones",
            baseIso = 200,
            exposureCompensation = 0.3f,
            whiteBalanceKelvin = 6500,
            contrast = 0.15f,
            saturation = 0.2f,
            warmth = 0.3f,
            tint = 0.05f,
            shadowLift = 0.1f,
            highlightCompression = 0.2f,
            blackPoint = 0.02f,
            grainAmount = 0.15f,
            grainSize = 0.8f,
            halationAmount = 0.1f,
            bloomAmount = 0.05f,
            vignetteAmount = 0.08f,
            softnessAmount = 0.05f,
            blackAndWhite = false
        ),
        
        // Soft Portrait 400 - Soft, elegant colors for portraits
        FilmProfile(
            id = "soft_portrait_400",
            name = "Soft Portrait 400",
            description = "Soft portrait film with natural skin tones",
            baseIso = 400,
            exposureCompensation = 0f,
            whiteBalanceKelvin = 5800,
            contrast = 0.05f,
            saturation = -0.1f,
            warmth = 0.15f,
            tint = 0f,
            shadowLift = 0.15f,
            highlightCompression = 0.25f,
            blackPoint = 0.01f,
            grainAmount = 0.12f,
            grainSize = 0.9f,
            halationAmount = 0.15f,
            bloomAmount = 0.1f,
            vignetteAmount = 0.1f,
            softnessAmount = 0.15f,
            blackAndWhite = false
        ),
        
        // Night Tungsten 800 - High speed film for low light
        FilmProfile(
            id = "night_tungsten_800",
            name = "Night Tungsten 800",
            description = "High speed film for night and tungsten lighting",
            baseIso = 800,
            exposureCompensation = -0.3f,
            whiteBalanceKelvin = 4200,
            contrast = 0.25f,
            saturation = 0.1f,
            warmth = -0.1f,
            tint = 0f,
            shadowLift = 0.05f,
            highlightCompression = 0.3f,
            blackPoint = 0.03f,
            grainAmount = 0.35f,
            grainSize = 1.2f,
            halationAmount = 0.3f,
            bloomAmount = 0.2f,
            vignetteAmount = 0.15f,
            softnessAmount = 0.08f,
            blackAndWhite = false
        ),
        
        // Green Street 400 - Urban street photography film
        FilmProfile(
            id = "green_street_400",
            name = "Green Street 400",
            description = "Urban street film with cool green tones",
            baseIso = 400,
            exposureCompensation = 0f,
            whiteBalanceKelvin = 5200,
            contrast = 0.18f,
            saturation = 0.15f,
            warmth = -0.15f,
            tint = -0.1f,
            shadowLift = 0.08f,
            highlightCompression = 0.2f,
            blackPoint = 0.02f,
            grainAmount = 0.2f,
            grainSize = 1.0f,
            halationAmount = 0.12f,
            bloomAmount = 0.08f,
            vignetteAmount = 0.12f,
            softnessAmount = 0.06f,
            blackAndWhite = false
        ),
        
        // Mono Press 400 - High contrast black and white
        FilmProfile(
            id = "mono_press_400",
            name = "Mono Press 400",
            description = "High contrast black and white documentary film",
            baseIso = 400,
            exposureCompensation = 0f,
            whiteBalanceKelvin = 5500,
            contrast = 0.35f,
            saturation = 0f,
            warmth = 0f,
            tint = 0f,
            shadowLift = 0.02f,
            highlightCompression = 0.15f,
            blackPoint = 0.05f,
            grainAmount = 0.28f,
            grainSize = 1.1f,
            halationAmount = 0.18f,
            bloomAmount = 0.12f,
            vignetteAmount = 0.1f,
            softnessAmount = 0.1f,
            blackAndWhite = true
        )
    )
    
    fun getProfile(id: String): FilmProfile? = profiles.find { it.id == id }
    
    fun getDefaultProfile(): FilmProfile = profiles.first()
}
