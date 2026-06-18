package com.roll24.camera.capture

import com.roll24.camera.CameraSettings
import com.roll24.camera.zoom.S24UltraZoomController
import com.roll24.film.FilmLabSettings
import com.roll24.film.FilmProfile
import com.roll24.film.FilmType

/**
 * Maps a film profile + camera settings into a concrete [S24UltraCaptureRecipe].
 *
 * Design philosophy:
 * - Negative (C-41) films → preserve latitude, minimal ISP, allow noise
 * - Slide (E-6) films → protect highlights, lower ISO preferred
 * - B&W films → accept higher ISO/noise (becomes grain), disable color processing
 * - Portrait films → soften edge enhancement, preserve skin tones
 * - Night films → limit ISO ceiling, preserve highlights
 */
object S24UltraCaptureRecipeMapper {

    fun from(
        filmProfile: FilmProfile,
        cameraSettings: CameraSettings,
        labSettings: FilmLabSettings,
        activeLensLabel: String = "1x"
    ): S24UltraCaptureRecipe {
        val format = resolveFormat(filmProfile)
        val noiseReduction = resolveNoiseReduction(filmProfile)
        val edge = resolveEdge(filmProfile)
        val tonemap = resolveTonemap(filmProfile)

        return S24UltraCaptureRecipe(
            lensLabel = activeLensLabel,
            preferredFormat = format,
            targetIso = null, // Auto for now — future: film ISO mapping
            targetExposureNanos = null, // Auto
            exposureBiasEv = cameraSettings.exposureCompensation,
            whiteBalanceKelvin = null, // Auto — future: film WB mapping
            noiseReductionPreference = noiseReduction,
            edgePreference = edge,
            tonemapPreference = tonemap,
            maxCreativeZoom = S24UltraZoomController.ROLL24_MAX_CREATIVE_ZOOM
        )
    }

    private fun resolveFormat(profile: FilmProfile): PreferredCaptureFormat {
        // All Roll24 profiles benefit from RAW when available
        return PreferredCaptureFormat.RAW_THEN_YUV_THEN_JPEG
    }

    private fun resolveNoiseReduction(profile: FilmProfile): ProcessingPreference {
        return when {
            // B&W films: noise becomes grain, keep it
            profile.blackAndWhite -> ProcessingPreference.OFF
            // Night/high-ISO films: minimal denoise to preserve texture
            profile.baseIso >= 800 -> ProcessingPreference.MINIMAL
            // Portrait: gentle denoise for skin
            isPortraitProfile(profile) -> ProcessingPreference.MINIMAL
            // Default: off for maximum Roll24 pipeline control
            else -> ProcessingPreference.OFF
        }
    }

    private fun resolveEdge(profile: FilmProfile): ProcessingPreference {
        return when {
            // Portrait: no digital sharpening
            isPortraitProfile(profile) -> ProcessingPreference.OFF
            // High ISO: sharpening amplifies noise
            profile.baseIso >= 800 -> ProcessingPreference.OFF
            // Default: off — Roll24 handles its own softness/sharpness
            else -> ProcessingPreference.OFF
        }
    }

    private fun resolveTonemap(profile: FilmProfile): TonemapPreference {
        // Roll24 always wants neutral/linear data — the H&D curve is applied in software
        return TonemapPreference.RAW_NEUTRAL
    }

    /** Heuristic: a portrait profile has "portrait" in name or high softness. */
    private fun isPortraitProfile(profile: FilmProfile): Boolean {
        return profile.name.contains("portrait", ignoreCase = true) ||
            profile.softnessAmount > 0.4f
    }
}
