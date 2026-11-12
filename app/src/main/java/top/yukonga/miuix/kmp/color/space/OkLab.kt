


package top.yukonga.miuix.kmp.color.space

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.color.core.Transforms


data class OkLab(val l: Float, val a: Float, val b: Float) {
    fun toColor(alpha: Float = 1f): Color {
        val lN = (l / 100f).coerceIn(0f, 1f)
        val aN = ((a / 100f) * 0.4f).coerceIn(-0.4f, 0.4f)
        val bN = ((b / 100f) * 0.4f).coerceIn(-0.4f, 0.4f)
        val ok = floatArrayOf(lN, aN, bN)
        return Transforms.okLabToColor(ok, alpha)
    }
}
