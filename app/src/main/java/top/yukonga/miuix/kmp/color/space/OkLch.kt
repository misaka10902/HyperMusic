package top.yukonga.miuix.kmp.color.space

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.color.core.Transforms

data class OkLch(val l: Float, val c: Float, val h: Float) {
    fun toColor(alpha: Float = 1f): Color {
        val lN = (l / 100f).coerceIn(0f, 1f)
        val cN = ((c / 100f) * 0.4f).coerceIn(0f, 0.4f)
        val hDeg = (((h % 360f) + 360f) % 360f)
        return Transforms.oklchToColor(lN, cN, hDeg, alpha)
    }
}
