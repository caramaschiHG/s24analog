package com.roll24.camera

import android.hardware.camera2.*
import android.util.Log
import com.roll24.film.FilmProfile

class CaptureEngine(
    private val capabilities: CameraCapabilities?
) {
    
    companion object {
        private const val TAG = "CaptureEngine"
    }
    
    /**
     * Applies film profile settings to capture request builder
     */
    fun applyProfile(
        builder: CaptureRequest.Builder,
        profile: FilmProfile
    ) {
        Log.d(TAG, "Applying profile: ${profile.name}")
        
        // Try to disable automatic processing for cleaner base
        applyManualControlMode(builder)
        
        // Apply ISO
        applyIso(builder, profile.baseIso)
        
        // Apply exposure compensation
        applyExposureCompensation(builder, profile.exposureCompensation)
        
        // Apply white balance
        applyWhiteBalance(builder, profile.whiteBalanceKelvin)
        
        // Disable/reduce automatic processing
        applyNoiseReduction(builder)
        applyEdgeMode(builder)
        applyTonemapMode(builder)
        
        // Disable HDR and scene modes
        applySceneMode(builder)
    }
    
    private fun applyManualControlMode(builder: CaptureRequest.Builder) {
        if (capabilities?.supportsManualSensor == true) {
            try {
                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE)
                Log.d(TAG, "Set CONTROL_MODE_USE_SCENE_MODE for manual control")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set manual control mode", e)
            }
        }
    }
    
    private fun applyIso(builder: CaptureRequest.Builder, iso: Int) {
        val isoRange = capabilities?.isoRange
        
        if (isoRange != null) {
            val clampedIso = iso.coerceIn(isoRange.lower, isoRange.upper)
            try {
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)
                Log.d(TAG, "Set ISO: $clampedIso")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set ISO", e)
            }
        } else {
            Log.d(TAG, "ISO control not available, using auto")
        }
    }
    
    private fun applyExposureCompensation(builder: CaptureRequest.Builder, compensation: Float) {
        try {
            // Convert float compensation to camera steps
            // This is simplified - real implementation would use AE_COMPENSATION_STEP
            val steps = (compensation * 6).toInt() // Approximate
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, steps)
            Log.d(TAG, "Set exposure compensation: $compensation EV")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set exposure compensation", e)
        }
    }
    
    private fun applyWhiteBalance(builder: CaptureRequest.Builder, kelvin: Int) {
        if (capabilities?.supportsManualWhiteBalance == true) {
            try {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
                
                // Set color temperature via color correction transform
                // This is simplified - real implementation would calculate proper matrix
                val temperature = kelvinToColorTemperature(kelvin)
                builder.set(CaptureRequest.COLOR_CORRECTION_MODE, 
                    CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
                
                Log.d(TAG, "Set white balance: ${kelvin}K")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set manual white balance", e)
                // Fallback to auto WB with warm/cool mode
                applyAutoWhiteBalance(builder, kelvin)
            }
        } else {
            applyAutoWhiteBalance(builder, kelvin)
        }
    }
    
    private fun applyAutoWhiteBalance(builder: CaptureRequest.Builder, kelvin: Int) {
        try {
            val wbMode = when {
                kelvin < 4500 -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                kelvin < 5500 -> CaptureRequest.CONTROL_AWB_MODE_WARM_FLUORESCENT
                kelvin < 6500 -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
                else -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
            }
            builder.set(CaptureRequest.CONTROL_AWB_MODE, wbMode)
            Log.d(TAG, "Set auto WB mode: $wbMode for ${kelvin}K")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set auto white balance", e)
        }
    }
    
    private fun applyNoiseReduction(builder: CaptureRequest.Builder) {
        val availableModes = capabilities?.noiseReductionModes ?: emptyList()
        
        val preferredMode = when {
            CaptureRequest.NOISE_REDUCTION_MODE_OFF in availableModes -> 
                CaptureRequest.NOISE_REDUCTION_MODE_OFF
            CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL in availableModes -> 
                CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL
            CaptureRequest.NOISE_REDUCTION_MODE_FAST in availableModes -> 
                CaptureRequest.NOISE_REDUCTION_MODE_FAST
            else -> CaptureRequest.NOISE_REDUCTION_MODE_FAST
        }
        
        try {
            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, preferredMode)
            Log.d(TAG, "Set noise reduction mode: $preferredMode")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set noise reduction mode", e)
        }
    }
    
    private fun applyEdgeMode(builder: CaptureRequest.Builder) {
        val availableModes = capabilities?.edgeModes ?: emptyList()
        
        // Prefer OFF or FAST to avoid aggressive sharpening
        val preferredMode = when {
            CaptureRequest.EDGE_MODE_OFF in availableModes -> 
                CaptureRequest.EDGE_MODE_OFF
            CaptureRequest.EDGE_MODE_FAST in availableModes -> 
                CaptureRequest.EDGE_MODE_FAST
            else -> CaptureRequest.EDGE_MODE_FAST
        }
        
        try {
            builder.set(CaptureRequest.EDGE_MODE, preferredMode)
            Log.d(TAG, "Set edge mode: $preferredMode")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set edge mode", e)
        }
    }
    
    private fun applyTonemapMode(builder: CaptureRequest.Builder) {
        val availableModes = capabilities?.tonemapModes ?: emptyList()
        
        // Prefer CONTRAST_CURVE for custom tone mapping, or FAST for minimal processing
        val preferredMode = when {
            CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE in availableModes -> 
                CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE
            CaptureRequest.TONEMAP_MODE_FAST in availableModes -> 
                CaptureRequest.TONEMAP_MODE_FAST
            else -> CaptureRequest.TONEMAP_MODE_FAST
        }
        
        try {
            builder.set(CaptureRequest.TONEMAP_MODE, preferredMode)
            Log.d(TAG, "Set tonemap mode: $preferredMode")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set tonemap mode", e)
        }
    }
    
    private fun applySceneMode(builder: CaptureRequest.Builder) {
        try {
            // Disable HDR and scene modes
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            Log.d(TAG, "Set CONTROL_MODE_AUTO to disable HDR/scene modes")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set control mode", e)
        }
    }
    
    private fun kelvinToColorTemperature(kelvin: Int): Float {
        // Simplified conversion - real implementation would be more sophisticated
        return kelvin.toFloat()
    }
}
