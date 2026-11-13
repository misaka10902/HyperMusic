package com.hyper.music.ui.effects.edgelight

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class EdgeLightConfig(
    val width: Dp = 1.dp,
    val blurRadius: Dp = 2.dp,
    val intensity: Float = 1f,
    val color: Color = Color.White.copy(alpha = 0.5f)
)

@Stable
fun Modifier.edgeLight(
    shape: Shape,
    config: EdgeLightConfig = EdgeLightConfig()
): Modifier {
    return this.edgeLight(
        shape = shape,
        edgeLight = EdgeLight(
            width = config.width,
            blurRadius = config.blurRadius,
            intensity = config.intensity,
            style = EdgeLightStyle.Uniform(color = config.color)
        )
    )
}

@Stable
fun Modifier.edgeLight(
    shape: Shape,
    color: Color = Color.White.copy(alpha = 0.5f),
    width: Dp = 1.dp,
    blurRadius: Dp = 2.dp,
    intensity: Float = 1f
): Modifier {
    return this.edgeLight(
        shape = shape,
        edgeLight = EdgeLight.Uniform(
            color = color,
            width = width,
            blurRadius = blurRadius,
            intensity = intensity
        )
    )
}

@Stable
fun Modifier.edgeLightIf(
    shape: Shape,
    condition: Boolean,
    color: Color = Color.White.copy(alpha = 0.5f),
    width: Dp = 1.dp,
    blurRadius: Dp = 2.dp,
    intensity: Float = 1f
): Modifier {
    return this.edgeLightIf(
        shape = shape,
        condition = condition,
        edgeLight = EdgeLight.Uniform(
            color = color,
            width = width,
            blurRadius = blurRadius,
            intensity = intensity
        )
    )
}
