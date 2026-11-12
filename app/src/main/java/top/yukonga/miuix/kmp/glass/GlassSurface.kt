


package top.yukonga.miuix.kmp.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.blur.Backdrop


fun glassSurface(
    backdrop: Backdrop?,
    visuals: GlassPopupVisuals,
    cornerRadius: Dp = GlassPopupDefaults.CornerRadius,
): @Composable (Shape) -> Modifier = { providedShape ->
    Modifier.glassPanel(
        backdrop = backdrop,
        shape = resolveGlassSurfaceShape(providedShape, cornerRadius),
        style = visuals.style,
        alpha = visuals.alpha,
        material = visuals.material,
        stroke = visuals.stroke,
        shadow = visuals.shadow,
        shading = false,
        fallback = Modifier.clip(providedShape).background(visuals.containerColor),
    )
}


internal fun resolveGlassSurfaceShape(providedShape: Shape, fallbackCornerRadius: Dp): GlassShape = when (providedShape) {
    is GlassShape -> providedShape

    is RoundedCornerShape -> GlassShape(
        topStart = providedShape.topStart,
        topEnd = providedShape.topEnd,
        bottomEnd = providedShape.bottomEnd,
        bottomStart = providedShape.bottomStart,
    )

    else -> GlassShape(fallbackCornerRadius)
}
