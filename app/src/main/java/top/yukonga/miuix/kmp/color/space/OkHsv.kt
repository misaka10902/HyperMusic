package top.yukonga.miuix.kmp.color.space

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.color.core.Transforms

data class OkHsv(val h: Float, val s: Float, val v: Float) {
    fun toColor(alpha: Float = 1f): Color = Transforms.okhsvToColor(h, s, v, alpha)
}
