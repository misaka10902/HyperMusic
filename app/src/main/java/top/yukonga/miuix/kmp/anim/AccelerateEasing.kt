


package top.yukonga.miuix.kmp.anim

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import kotlin.math.pow


@Immutable
class AccelerateEasing(
    private val factor: Float = 1.0f,
) : Easing {
    private val doubleFactor: Float = 2 * factor

    override fun transform(fraction: Float): Float = if (factor == 1.0f) {
        fraction * fraction
    } else {
        fraction.pow(doubleFactor)
    }
}
