package com.roll24.film

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class Wave4EvidenceTest {

    private val evidenceDir = File("C:/Users/SylvianAliceCaramasc/Desktop/24mm cam/.sisyphus/evidence").apply { mkdirs() }
    private val engine = FilmDevelopmentEngine()

    private val stocks = listOf(
        FilmStock.PORTRA_400 to "portra",
        FilmStock.EKTAR_100 to "ektar",
        FilmStock.PRO_400H to "pro400h",
        FilmStock.VELVIA_50 to "velvia",
        FilmStock.CINESTILL_800T to "cinestill",
        FilmStock.VISION3_250D to "vision3",
        FilmStock.GOLD_200 to "gold",
        FilmStock.FUJICOLOR_C200 to "fujicolor",
        FilmStock.HP5_PLUS_400 to "hp5",
        FilmStock.TRI_X_400 to "trix"
    )

    private val taskNumbers = mapOf(
        "portra" to 17,
        "ektar" to 18,
        "pro400h" to 19,
        "velvia" to 20,
        "cinestill" to 21,
        "vision3" to 22,
        "gold" to 23,
        "fujicolor" to 24,
        "hp5" to 25,
        "trix" to 26
    )

    @Test
    fun wave4CanonicalStocks() {
        val input = createTestBitmap()
        val report = StringBuilder()
        report.appendLine("Wave 4 - Canonical Film Stocks (T17-T26)")
        report.appendLine("=".repeat(50))

        for ((stock, slug) in stocks) {
            val taskNum = taskNumbers.getValue(slug)
            val profile = FilmProfileRepository.getProfile(stock.id)
                ?: throw AssertionError("Missing profile for ${stock.id}")

            require(profile.filmType == stock.filmType) {
                "${stock.id} filmType mismatch: ${profile.filmType} != ${stock.filmType}"
            }

            val output = engine.develop(input.copy(Bitmap.Config.ARGB_8888, true), profile)
            val png = File(evidenceDir, "task-${taskNum}-${slug}.png")
            output.savePng(png)

            val sample = output.getPixel(output.width / 2, output.height / 2)
            report.appendLine()
            report.appendLine("T$taskNum ${stock.name} (${stock.id})")
            report.appendLine("  filmType=${profile.filmType}, baseIso=${profile.baseIso}")
            report.appendLine("  contrast=${profile.contrast}, saturation=${profile.saturation}, warmth=${profile.warmth}, tint=${profile.tint}")
            report.appendLine("  halation=${profile.halationAmount}, bloom=${profile.bloomAmount}, grain=${profile.grainAmount}")
            report.appendLine("  blackAndWhite=${profile.blackAndWhite}")
            report.appendLine("  center sample: ${formatPixel(sample)}")

            if (profile.blackAndWhite) {
                require(isGrayscale(sample)) { "${stock.id} B&W output is not grayscale" }
            }
        }

        // Specific acceptance checks
        val cinestill = FilmProfileRepository.getProfile("cinestill_800t")!!
        require(cinestill.halationAmount >= 0.40f) {
            "CineStill 800T halationAmount ${cinestill.halationAmount} < 0.40f"
        }

        val velvia = FilmProfileRepository.getProfile("velvia_50")!!
        require(velvia.filmType == FilmType.E6) { "Velvia 50 must be E6" }

        val hp5 = FilmProfileRepository.getProfile("hp5_plus_400")!!
        require(hp5.blackAndWhite) { "HP5 Plus must be blackAndWhite" }

        val triX = FilmProfileRepository.getProfile("tri_x_400")!!
        require(triX.blackAndWhite && triX.contrast >= 0.30f) {
            "Tri-X 400 must be B&W with contrast >= 0.30f"
        }

        File(evidenceDir, "task-17-26-wave4-summary.txt").writeText(report.toString())
    }

    private fun createTestBitmap(): Bitmap {
        val w = 128
        val h = 128
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) {
            for (x in 0 until w) {
                // Gradient background plus a bright "light" region for halation/bloom
                val baseR = (x * 255 / w).coerceAtMost(255)
                val baseG = (y * 255 / h).coerceAtMost(255)
                val baseB = 128
                val r = if (x in 54..74 && y in 54..74) 255 else baseR
                val g = if (x in 54..74 && y in 54..74) 255 else baseG
                val b = if (x in 54..74 && y in 54..74) 255 else baseB
                bmp.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        return bmp
    }

    private fun isGrayscale(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return abs(r - g) <= 2 && abs(g - b) <= 2
    }

    private fun formatPixel(p: Int): String {
        return "R=${Color.red(p)} G=${Color.green(p)} B=${Color.blue(p)}"
    }

    private fun Bitmap.savePng(file: File) {
        FileOutputStream(file).use { compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
