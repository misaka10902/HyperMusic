


package top.yukonga.miuix.kmp.anim

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import kotlin.math.pow


@Immutable
class DecelerateEasing(
    private val factor: Float = 1.0f,
) : Easing {
    override fun transform(fraction: Float): Float = if (factor == 1.0f) {
        1.0f - (1.0f - fraction) * (1.0f - fraction)
    } else {
        1.0f - (1.0f - fraction).pow(2 * factor)
    }
}
