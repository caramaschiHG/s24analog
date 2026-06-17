package com.roll24.film

data class FilmLabSettings(
    val filmIntensity: Float = 1f,
    val pushPull: Float = 0f,
    val grainAmount: Float = 1f,
    val halationAmount: Float = 1f,
    val bloomAmount: Float = 1f,
    val vignetteAmount: Float = 1f,
    val warmth: Float = 0f,
    val contrast: Float = 0f,
    val normalizeAmount: Float = 1f,
    val digitalLookReduction: Float = 0.3f
)

