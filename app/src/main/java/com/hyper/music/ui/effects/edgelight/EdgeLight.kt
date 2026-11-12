package com.hyper.music.ui.effects.edgelight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyper.music.ui.utils.isAppDarkTheme

@Immutable
data class EdgeLight(
    val width: Dp = 1.dp,
    val blurRadius: Dp = 2.dp,
    val intensity: Float = 1f,
    val style: EdgeLightStyle = EdgeLightStyle.Default
) {
    companion object {

        @Stable
        val Default: EdgeLight = EdgeLight()

        @Stable
        fun Uniform(
            color: Color = Color.White.copy(alpha = 0.5f),
            width: Dp = 0.28.dp,
            blurRadius: Dp = 0.8.dp,
            intensity: Float = 1f
        ): EdgeLight = EdgeLight(
            width = width,
            blurRadius = blurRadius,
            intensity = intensity,
            style = EdgeLightStyle.Uniform(color = color)
        )

        @Stable
        fun Directional(
            color: Color = Color.White.copy(alpha = 0.5f),
            width: Dp = 1.dp,
            blurRadius: Dp = 2.dp,
            intensity: Float = 1f,
            angle: Float = 45f,
            falloff: Float = 1f
        ): EdgeLight = EdgeLight(
            width = width,
            blurRadius = blurRadius,
            intensity = intensity,
            style = EdgeLightStyle.Directional(color = color, angle = angle, falloff = falloff)
        )

        @Stable
        fun Glow(
            color: Color = Color.White.copy(alpha = 0.5f),
            width: Dp = 1.dp,
            blurRadius: Dp = 2.dp,
            intensity: Float = 1f,
            glowSize: Float = 10f
        ): EdgeLight = EdgeLight(
            width = width,
            blurRadius = blurRadius,
            intensity = intensity,
            style = EdgeLightStyle.Glow(color = color, glowSize = glowSize)
        )

    }
}

@Composable
fun rememberDefaultEdgeLight(): EdgeLight {
    val isLightTheme = !isAppDarkTheme()
    val color = if (isLightTheme) Color.White.copy(alpha = 0.7f)
                else Color.White.copy(alpha = 0.4f)
    return remember(isLightTheme) { EdgeLight.Uniform(color = color) }
}
