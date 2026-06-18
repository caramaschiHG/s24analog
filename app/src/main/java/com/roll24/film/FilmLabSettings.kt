package com.roll24.film

import com.roll24.camera.CaptureSource
import com.roll24.camera.InputEncoding

data class FilmLabSettings(
    val captureSource: CaptureSource = CaptureSource.JPEG,
    val inputEncoding: InputEncoding = InputEncoding.SRGB,
    val filmIntensity: Float = 1f,
    val pushPull: Float = 0f,
    val grainAmount: Float = 1f,
    val halationAmount: Float = 1f,
    val bloomAmount: Float = 1f,
    val vignetteAmount: Float = 1f,
    val warmth: Float = 0f,
    val contrast: Float = 0f,
    val normalizeAmount: Float = 1f,
    val digitalLookReduction: Float = 0.3f,
    val targetOutputWidth: Int = 0,
    val targetOutputHeight: Int = 0
)

