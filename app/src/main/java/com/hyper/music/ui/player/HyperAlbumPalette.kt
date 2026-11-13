package com.hyper.music.ui.player

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class AlbumPalette(
    val dominant: androidx.compose.ui.graphics.Color,
    val secondary: androidx.compose.ui.graphics.Color,
    val top: androidx.compose.ui.graphics.Color,
    val middle: androidx.compose.ui.graphics.Color,
    val bottom: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
)

object AlbumPaletteExtractor {

    fun paletteOf(bitmap: Bitmap): AlbumPalette? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val buckets = HashMap<Int, LongArray>(256)
        val step = max(1, pixels.size / 4096)
        var i = 0
        while (i < pixels.size) {
            val pixel = pixels[i]
            i += step
            if (pixel ushr 24 < 0x7F) continue
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            if (max(r, max(g, b)) < 24) continue
            val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            val slot = buckets.getOrPut(key) { LongArray(4) }
            slot[0]++
            slot[1] += r.toLong()
            slot[2] += g.toLong()
            slot[3] += b.toLong()
        }
        if (buckets.isEmpty()) return null

        val dominantArgb = bestColor(buckets, null)
        val secondaryArgb = bestColor(buckets, dominantArgb)
        return buildPalette(dominantArgb, secondaryArgb)
    }

    private fun bestColor(buckets: HashMap<Int, LongArray>, exclude: Int?): Int {
        val excludeHsl = if (exclude != null) hslOf(exclude) else FloatArray(3)

        var bestScore = -1f
        var best = exclude ?: 0
        for ((_, slot) in buckets) {
            val count = slot[0]
            if (count <= 0L) continue
            val r = (slot[1] / count).toInt()
            val g = (slot[2] / count).toInt()
            val b = (slot[3] / count).toInt()

            val hsl = hslOf(Color.rgb(r, g, b))
            val saturation = hsl[1]
            val lightness = hsl[2]

            val lightnessWeight = when {
                lightness < 0.08f -> 0.15f
                lightness > 0.92f -> 0.25f
                lightness < 0.20f -> 0.55f
                lightness > 0.80f -> 0.60f
                else -> 1f
            }
            var score = count * (0.28f + saturation * 1.6f) * lightnessWeight

            if (exclude != null) {
                val hueDelta = abs(hsl[0] - excludeHsl[0]).let { min(it, 360f - it) }
                score *= when {
                    hueDelta < 18f -> 0.02f
                    hueDelta < 40f -> 0.35f
                    else -> 1f
                }
            }
            if (score > bestScore) {
                bestScore = score
                best = Color.rgb(r, g, b)
            }
        }
        return best
    }

    private fun buildPalette(dominantArgb: Int, secondaryArgb: Int): AlbumPalette {
        val hsl = hslOf(dominantArgb)
        val hue = hsl[0]
        val saturation = hsl[1].coerceIn(0.18f, 0.92f)

        val secHsl = hslOf(secondaryArgb)
        val secondaryHue = if (abs(secHsl[0] - hue) < 1f) (hue + 24f) % 360f else secHsl[0]

        return AlbumPalette(
            dominant = androidx.compose.ui.graphics.Color(dominantArgb),
            secondary = androidx.compose.ui.graphics.Color(
                hslToColor(secondaryHue, secHsl[1].coerceIn(0.20f, 0.90f), 0.34f)
            ),
            top = androidx.compose.ui.graphics.Color(
                hslToColor(hue, (saturation * 0.78f).coerceIn(0.16f, 0.80f), 0.30f)
            ),
            middle = androidx.compose.ui.graphics.Color(
                hslToColor(secondaryHue, (secHsl[1] * 0.72f).coerceIn(0.14f, 0.74f), 0.17f)
            ),
            bottom = androidx.compose.ui.graphics.Color(
                hslToColor(hue, (saturation * 0.55f).coerceIn(0.10f, 0.60f), 0.075f)
            ),
            accent = androidx.compose.ui.graphics.Color(
                hslToColor(hue, max(saturation, 0.55f).coerceAtMost(0.95f), 0.68f)
            ),
        )
    }

    private fun hslOf(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val l = (max + min) / 2f
        if (max == min) return floatArrayOf(0f, 0f, l)
        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        return floatArrayOf(h, s, l)
    }

    private fun hslToColor(hue: Float, saturation: Float, lightness: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val sector = (h / 60f).toInt() % 6
        val (r1, g1, b1) = when (sector) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color.rgb(
            ((r1 + m) * 255f).roundToInt().coerceIn(0, 255),
            ((g1 + m) * 255f).roundToInt().coerceIn(0, 255),
            ((b1 + m) * 255f).roundToInt().coerceIn(0, 255),
        )
    }
}
