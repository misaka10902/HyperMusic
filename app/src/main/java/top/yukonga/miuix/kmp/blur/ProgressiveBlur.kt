


package top.yukonga.miuix.kmp.blur

import androidx.compose.runtime.Immutable


@Immutable
data class ProgressiveBlur(
    val angle: Float = 90f,
    val startFraction: Float = 0f,
    val endFraction: Float = 1f,
    val curve: Float = 1f,
) {
    companion object {

        val Top: ProgressiveBlur = ProgressiveBlur(angle = 90f, startFraction = 0f, endFraction = 1f)


        val Bottom: ProgressiveBlur = ProgressiveBlur(angle = 270f, startFraction = 0f, endFraction = 1f)


        val Left: ProgressiveBlur = ProgressiveBlur(angle = 0f, startFraction = 0f, endFraction = 1f)


        val Right: ProgressiveBlur = ProgressiveBlur(angle = 180f, startFraction = 0f, endFraction = 1f)
    }
}
