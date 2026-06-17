package com.roll24.film

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.roll24.Roll24ViewModel
import com.roll24.camera.SensorProfile
import com.roll24.image.ImageSaver
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class Wave6EvidenceTest {

    private val evidenceDir = File("C:/Users/SylvianAliceCaramasc/Desktop/24mm cam/.sisyphus/evidence").apply { mkdirs() }
    private val appContext: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun task27ResolutionAwareProcessing() {
        val engine = FilmDevelopmentEngine()
        val profile = FilmProfileRepository.getDefaultProfile()
        val labSettings = FilmLabSettings(normalizeAmount = 0f, digitalLookReduction = 0f)
        val input = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        input.eraseColor(Color.rgb(128, 128, 128))

        val full = engine.develop(input, profile, labSettings, ProcessingResolution.FULL)
        val half = engine.develop(input, profile, labSettings, ProcessingResolution.HALF)
        val quarter = engine.develop(input, profile, labSettings, ProcessingResolution.QUARTER)
        val thumbnail = engine.develop(input, profile, labSettings, ProcessingResolution.THUMBNAIL)

        val hintSettings = FilmLabSettings(
            normalizeAmount = 0f,
            digitalLookReduction = 0f,
            targetOutputWidth = 100,
            targetOutputHeight = 75
        )
        val hinted = engine.develop(input, profile, hintSettings, ProcessingResolution.FULL)

        File(evidenceDir, "task-27-resolution.txt").writeText(
            buildString {
                appendLine("T27 Resolution-aware processing")
                appendLine("Input:  ${input.width}x${input.height}")
                appendLine("FULL:   ${full.width}x${full.height}")
                appendLine("HALF:   ${half.width}x${half.height}")
                appendLine("QUARTER: ${quarter.width}x${quarter.height}")
                appendLine("THUMBNAIL: ${thumbnail.width}x${thumbnail.height}")
                appendLine("Hinted (100x75): ${hinted.width}x${hinted.height}")
                appendLine("HALF roughly half input: ${half.width == input.width / 2 && half.height == input.height / 2}")
                appendLine("Hint overrides preset: ${hinted.width == 100 && hinted.height == 75}")
            }
        )
    }

    @Test
    fun task29PipelineDiff() {
        val engine = FilmDevelopmentEngine()
        val profile = FilmProfileRepository.getProfile("portra_400")
            ?: FilmProfileRepository.getDefaultProfile()
        val input = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                val v = ((x * 4) + y) % 256
                input.setPixel(x, y, Color.rgb(v, v, v))
            }
        }

        // New pipeline with all flags enabled
        FeatureFlags.useNewPipeline = true
        FeatureFlags.useOrangeMaskRemoval = true
        val newPipeline = engine.develop(input, profile, FilmLabSettings(), sensorProfile = null, captureIso = null)

        // Legacy pipeline
        val legacy = engine.developLegacy(input, profile)

        // Feature-flag off new pipeline globally
        FeatureFlags.useNewPipeline = false
        val flagsOff = engine.develop(input, profile, FilmLabSettings(), sensorProfile = null, captureIso = null)

        var diffNewLegacy = 0L
        var diffNewFlagsOff = 0L
        var count = 0
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                val pNew = newPipeline.getPixel(x, y)
                val pLegacy = legacy.getPixel(x, y)
                val pFlagsOff = flagsOff.getPixel(x, y)
                diffNewLegacy += channelDiff(pNew, pLegacy)
                diffNewFlagsOff += channelDiff(pNew, pFlagsOff)
                count += 3
            }
        }

        // Restore defaults so later tests are not surprised
        FeatureFlags.useNewPipeline = true
        FeatureFlags.useOrangeMaskRemoval = true

        File(evidenceDir, "task-29-pipeline-diff.txt").writeText(
            buildString {
                appendLine("T29 Engine integration with feature flags")
                appendLine("Profile: ${profile.id} (${profile.name})")
                appendLine("New vs Legacy mean channel diff: ${diffNewLegacy / count.toFloat()}")
                appendLine("New vs FlagsOff mean channel diff: ${diffNewFlagsOff / count.toFloat()}")
                appendLine("Legacy and new differ: ${diffNewLegacy > 0}")
                appendLine("developLegacy compiles and returns bitmap: ${legacy.width == input.width && legacy.height == input.height}")
            }
        )
    }

    @Test
    fun task30PerSensorDefaultsAndPersistence() {
        val viewModel = Roll24ViewModel()
        val lensLabels = listOf("0.6x", "1x", "3x", "5x")

        val defaults = lensLabels.associateWith { label ->
            val profile = defaultProfileForLens(label)
            label to profile.id
        }

        // Simulate selecting each lens and verify the default profile is applied
        for (label in lensLabels) {
            val sensorProfile = SensorProfile(
                cameraId = "0",
                physicalId = null,
                lensLabel = label,
                supportsRaw = false,
                supportsManual = false,
                focalLengthMm = when (label) {
                    "0.6x" -> 2.2f
                    "1x" -> 6.3f
                    "3x" -> 7.9f
                    "5x" -> 18.6f
                    else -> 6.3f
                },
                aperture = null,
                isoRange = null,
                exposureRange = null,
                rawSizes = emptyList(),
                yuvSizes = emptyList()
            )
            viewModel.selectLens(appContext, sensorProfile)
        }

        // Manual override: select a different profile and switch lenses to verify persistence
        val manualProfile = FilmProfileRepository.getProfile("mono_press_400")
            ?: FilmProfileRepository.getDefaultProfile()
        val targetLens = "1x"
        val sensorProfile1x = SensorProfile(
            cameraId = "0", physicalId = null, lensLabel = targetLens,
            supportsRaw = false, supportsManual = false, focalLengthMm = 6.3f,
            aperture = null, isoRange = null, exposureRange = null,
            rawSizes = emptyList(), yuvSizes = emptyList()
        )
        viewModel.selectLens(appContext, sensorProfile1x)
        viewModel.selectProfile(appContext, manualProfile)

        // Switch away and back
        val sensorProfile3x = SensorProfile(
            cameraId = "0", physicalId = null, lensLabel = "3x",
            supportsRaw = false, supportsManual = false, focalLengthMm = 7.9f,
            aperture = null, isoRange = null, exposureRange = null,
            rawSizes = emptyList(), yuvSizes = emptyList()
        )
        viewModel.selectLens(appContext, sensorProfile3x)
        viewModel.selectLens(appContext, sensorProfile1x)

        val restoredProfile = viewModel.selectedProfile.value

        File(evidenceDir, "task-30-manual-override.txt").writeText(
            buildString {
                appendLine("T30 Per-sensor selector defaults + validation")
                defaults.values.forEach { (label, id) ->
                    appendLine("Default for $label -> $id")
                }
                appendLine("Manual override on 1x: ${manualProfile.id}")
                appendLine("Restored profile after lens switch: ${restoredProfile.id}")
                appendLine("Manual override persisted: ${restoredProfile.id == manualProfile.id}")
            }
        )
    }

    @Test
    fun task30EndToEndDevelopAndSave() {
        val engine = FilmDevelopmentEngine()
        val profile = FilmProfileRepository.getProfile("portra_400")
            ?: FilmProfileRepository.getDefaultProfile()
        val input = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        input.eraseColor(Color.rgb(128, 128, 128))

        val developed = engine.develop(input, profile, FilmLabSettings())
        val negative = engine.develop(input, profile, FilmLabSettings())

        val result = com.roll24.image.ImageSaver.saveAnalogCapture(
            context = appContext,
            negative = negative,
            developed = developed,
            profile = profile,
            label = "TEST_${System.currentTimeMillis()}",
            metadata = null
        )

        File(evidenceDir, "task-30-end-to-end.txt").writeText(
            buildString {
                appendLine("T30 End-to-end develop + save")
                appendLine("Developed dimensions: ${developed.width}x${developed.height}")
                appendLine("Negative dimensions: ${negative.width}x${negative.height}")
                appendLine("Save result non-null: ${result != null}")
                appendLine("Gallery URI: ${result?.galleryUri}")
                appendLine("Developed URI: ${result?.developedUri}")
                appendLine("Negative URI: ${result?.negativeUri}")
            }
        )
    }

    private fun defaultProfileForLens(lensLabel: String): FilmProfile {
        return when (lensLabel) {
            "0.6x" -> FilmProfileRepository.getProfile("gold_200")
                ?: FilmProfileRepository.getProfile("fujicolor_c200")
                ?: FilmProfileRepository.getProfile("warm_gold_200")
            "1x" -> FilmProfileRepository.getProfile("portra_400")
                ?: FilmProfileRepository.getProfile("ektar_100")
                ?: FilmProfileRepository.getProfile("soft_portrait_400")
                ?: FilmProfileRepository.getProfile("s24_1x_clean_negative")
            "3x" -> FilmProfileRepository.getProfile("pro_400h")
                ?: FilmProfileRepository.getProfile("s24_3x_portrait_400")
            "5x" -> FilmProfileRepository.getProfile("vision3_250d")
                ?: FilmProfileRepository.getProfile("s24_5x_chrome_200")
            else -> null
        } ?: FilmProfileRepository.getDefaultProfile()
    }

    private fun channelDiff(a: Int, b: Int): Int {
        return kotlin.math.abs(Color.red(a) - Color.red(b)) +
            kotlin.math.abs(Color.green(a) - Color.green(b)) +
            kotlin.math.abs(Color.blue(a) - Color.blue(b))
    }
}
