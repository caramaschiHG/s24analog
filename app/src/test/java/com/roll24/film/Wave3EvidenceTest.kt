package com.roll24.film

import android.graphics.Bitmap
import android.graphics.Color
import com.roll24.camera.SensorProfile
import com.roll24.film.processors.GrainProcessor
import com.roll24.film.processors.HalationProcessor
import com.roll24.film.processors.HdChannelParams
import com.roll24.film.processors.HdCurveParams
import com.roll24.film.processors.HdCurveProcessor
import com.roll24.film.processors.OrangeMaskProcessor
import com.roll24.sensor.S24UltraSensorDossier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class Wave3EvidenceTest {

    private val evidenceDir = File("C:/Users/SylvianAliceCaramasc/Desktop/24mm cam/.sisyphus/evidence").apply { mkdirs() }

    @Test
    fun task10Normalize() {
        val inBitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        inBitmap.eraseColor(Color.rgb(128, 128, 128))

        val spec = S24UltraSensorDossier.byLensLabel("1x")!!
        val sensorProfile = SensorProfile(
            cameraId = "0", physicalId = null, lensLabel = "1x", supportsRaw = false,
            supportsManual = false, focalLengthMm = 6.3f, aperture = 1.7f,
            isoRange = null, exposureRange = null, rawSizes = emptyList(), yuvSizes = emptyList(),
            manufacturer = "Samsung", modelName = "ISOCELL HP2", resolutionMp = 200f,
            pixelSizeUm = 0.6f, opticalFormat = "1/1.3\"", cfaPattern = "RGGB",
            binningModes = emptyList(), hasOis = true, technologies = emptyList(),
            nativeFocalLengthMm = 6.3f, equivalentFocalLengthMm = 23f,
            matchedDossierSpec = spec
        )

        val engine = FilmDevelopmentEngine()
        val settings = FilmLabSettings(normalizeAmount = 1f, digitalLookReduction = 0f)
        val out = engine.develop(inBitmap, FilmProfile.NEUTRAL, settings, sensorProfile, null)

        File(evidenceDir, "task-10-normalize.txt").writeText(
            buildString {
                appendLine("T10 Sensor-aware normalize")
                appendLine("Input center pixel:  R=${Color.red(inBitmap.getPixel(0,0))} G=${Color.green(inBitmap.getPixel(0,0))} B=${Color.blue(inBitmap.getPixel(0,0))}")
                appendLine("Output center pixel: R=${Color.red(out.getPixel(0,0))} G=${Color.green(out.getPixel(0,0))} B=${Color.blue(out.getPixel(0,0))}")
                appendLine("Changed: ${inBitmap.getPixel(0,0) != out.getPixel(0,0)}")
            }
        )
    }

    @Test
    fun task11DigitalLook() {
        val inBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val v = ((x * 16) + y) % 256
                inBitmap.setPixel(x, y, Color.rgb(v, v, v))
            }
        }

        val engine = FilmDevelopmentEngine()
        val off = FilmLabSettings(normalizeAmount = 0f, digitalLookReduction = 0f)
        val on = FilmLabSettings(normalizeAmount = 0f, digitalLookReduction = 0.8f)

        val outOff = engine.develop(inBitmap, FilmProfile.NEUTRAL, off, sensorProfile = null, captureIso = null)
        val outOn = engine.develop(inBitmap, FilmProfile.NEUTRAL, on, sensorProfile = null, captureIso = null)

        var diff = 0L
        var count = 0
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val p1 = outOff.getPixel(x, y)
                val p2 = outOn.getPixel(x, y)
                diff += kotlin.math.abs(Color.red(p1) - Color.red(p2))
                diff += kotlin.math.abs(Color.green(p1) - Color.green(p2))
                diff += kotlin.math.abs(Color.blue(p1) - Color.blue(p2))
                count += 3
            }
        }
        val meanDiff = diff / count.toFloat()

        File(evidenceDir, "task-11-digital-look.txt").writeText(
            buildString {
                appendLine("T11 reduceDigitalLook")
                appendLine("digitalLookReduction=0 center:  ${formatPixel(outOff.getPixel(4, 4))}")
                appendLine("digitalLookReduction=0.8 center: ${formatPixel(outOn.getPixel(4, 4))}")
                appendLine("Mean absolute channel difference: $meanDiff")
            }
        )
    }

    @Test
    fun task12HdCurve() {
        val w = 256
        val h = 64
        val inBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (x in 0 until w) {
            for (y in 0 until h) {
                inBitmap.setPixel(x, y, Color.rgb(x, x, x))
            }
        }

        val proc = HdCurveProcessor()
        val red = HdChannelParams(0.12f, 0.25f, 1.05f, 0.02f, 1f)
        val green = HdChannelParams(0.10f, 0.20f, 1.0f, 0.02f, 1f)
        val blue = HdChannelParams(0.08f, 0.18f, 0.98f, 0.02f, 1f)
        val params = HdCurveParams(base = red, red = red, green = green, blue = blue)
        val out = proc.process(inBitmap.copy(Bitmap.Config.ARGB_8888, true), params)

        inBitmap.savePng(File(evidenceDir, "task-12-hd-curve-input.png"))
        out.savePng(File(evidenceDir, "task-12-hd-curve.png"))

        File(evidenceDir, "task-12-hd-curve.txt").writeText(
            buildString {
                appendLine("T12 H&D curve")
                appendLine("Saved gradient before/after PNGs.")
                appendLine("Input  (x=64):  ${formatPixel(inBitmap.getPixel(64, 32))}")
                appendLine("Output (x=64):  ${formatPixel(out.getPixel(64, 32))}")
                appendLine("Output (x=192): ${formatPixel(out.getPixel(192, 32))}")
            }
        )
    }

    @Test
    fun task13GrainIso() {
        val base = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        base.eraseColor(Color.rgb(128, 128, 128))

        val proc = GrainProcessor()
        val spec = S24UltraSensorDossier.byLensLabel("1x")!!

        val iso100 = proc.apply(base.copy(Bitmap.Config.ARGB_8888, true), 0.5f, 1f, captureIso = 100, sensorSpec = spec)
        val iso3200 = proc.apply(base.copy(Bitmap.Config.ARGB_8888, true), 0.5f, 1f, captureIso = 3200, sensorSpec = spec)

        val std100 = stdDev(iso100)
        val std3200 = stdDev(iso3200)

        File(evidenceDir, "task-13-grain-iso.txt").writeText(
            buildString {
                appendLine("T13 ISO-aware grain")
                appendLine("Base ISO (HP2): ${com.roll24.sensor.SensorNoiseModel.forSensor(spec)?.baseIso}")
                appendLine("ISO 100 stddev:  $std100")
                appendLine("ISO 3200 stddev: $std3200")
                appendLine("Higher ISO has stronger grain: ${std3200 > std100}")
            }
        )
    }

    @Test
    fun task14Halation() {
        val w = 64
        val h = 64
        val inBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        inBitmap.eraseColor(Color.BLACK)
        for (y in 28 until 36) {
            for (x in 28 until 36) {
                inBitmap.setPixel(x, y, Color.WHITE)
            }
        }

        val proc = HalationProcessor()
        val out = proc.apply(inBitmap.copy(Bitmap.Config.ARGB_8888, true), 0.8f)
        inBitmap.savePng(File(evidenceDir, "task-14-halation-input.png"))
        out.savePng(File(evidenceDir, "task-14-halation.png"))

        val sample = out.getPixel(24, 32)
        File(evidenceDir, "task-14-halation.txt").writeText(
            buildString {
                appendLine("T14 Halation")
                appendLine("Sample near white square (x=24,y=32): ${formatPixel(sample)}")
                appendLine("Red shift visible: ${Color.red(sample) > Color.green(sample) && Color.red(sample) > Color.blue(sample)}")
            }
        )
    }

    @Test
    fun task15OrangeMask() {
        val inBitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        inBitmap.eraseColor(Color.rgb(180, 120, 40))

        val proc = OrangeMaskProcessor()
        val c41 = proc.process(inBitmap.copy(Bitmap.Config.ARGB_8888, true), FilmType.C41)
        val e6 = proc.process(inBitmap.copy(Bitmap.Config.ARGB_8888, true), FilmType.E6)
        val bw = proc.process(inBitmap.copy(Bitmap.Config.ARGB_8888, true), FilmType.BLACK_AND_WHITE)

        File(evidenceDir, "task-15-orange-mask.txt").writeText(
            buildString {
                appendLine("T15 Orange mask removal")
                appendLine("Input:                 ${formatPixel(inBitmap.getPixel(0, 0))}")
                appendLine("C41 output:            ${formatPixel(c41.getPixel(0, 0))}")
                appendLine("E6 output (unchanged): ${formatPixel(e6.getPixel(0, 0))}")
                appendLine("B&W output (unchanged): ${formatPixel(bw.getPixel(0, 0))}")
                appendLine("C41 orange reduced: ${Color.red(c41.getPixel(0, 0)) < Color.red(inBitmap.getPixel(0, 0))}")
            }
        )
    }

    @Test
    fun task16FilmStock() {
        val curve = HdCurveParams(
            base = HdChannelParams(0.10f, 0.20f, 1.0f, 0.02f, 1f)
        )
        val stock = FilmStock(
            id = "kodak_portra_400",
            name = "Kodak Portra 400",
            description = "Natural color negative film",
            filmType = FilmType.C41,
            baseIso = 400,
            curveParams = curve,
            colorResponse = ColorResponseParams(
                saturation = 0f, warmth = 0.1f, tint = 0f,
                shadowLift = 0.1f, highlightCompression = 0.25f,
                blackPoint = 0.02f, contrast = 0.05f
            )
        )

        val profile = FilmProfile.fromStock(stock)

        File(evidenceDir, "task-16-film-stock.txt").writeText(
            buildString {
                appendLine("T16 FilmProfile.fromStock")
                appendLine("Stock id/name: ${stock.id} / ${stock.name}")
                appendLine("Profile id/name: ${profile.id} / ${profile.name}")
                appendLine("FilmType: ${profile.filmType}")
                appendLine("FilmStockId: ${profile.filmStockId}")
                appendLine("BaseISO: ${profile.baseIso}")
                appendLine("HalationThreshold: ${profile.halationThreshold}")
                appendLine("BloomThreshold: ${profile.bloomThreshold}")
                appendLine("Has hdCurveParams: ${profile.hdCurveParams != null}")
            }
        )
    }

    private fun formatPixel(p: Int): String {
        return "R=${Color.red(p)} G=${Color.green(p)} B=${Color.blue(p)}"
    }

    private fun stdDev(bmp: Bitmap): Double {
        val n = bmp.width * bmp.height
        var sum = 0.0
        var sumSq = 0.0
        for (y in 0 until bmp.height) {
            for (x in 0 until bmp.width) {
                val p = bmp.getPixel(x, y)
                val lum = 0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)
                sum += lum
                sumSq += lum * lum
            }
        }
        val mean = sum / n
        return sqrt(sumSq / n - mean * mean)
    }

    private fun Bitmap.savePng(file: File) {
        FileOutputStream(file).use { compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
