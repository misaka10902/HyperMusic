package com.hyper.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyper.music.ui.utils.isAppDarkTheme
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShape


@Composable
internal fun hyperSurfaceColor(): Color {
    val isLightTheme = !isAppDarkTheme()
    return if (isLightTheme) Color(0xFFFFFFFF).copy(0.88f) else Color(0xFF121212).copy(0.84f)
}

@Composable
internal fun Modifier.floatingBarGlass(
    backdrop: Backdrop?,
    shape: GlassShape,
    enabled: Boolean,
): Modifier {
    val material: GlassMaterial = GlassMaterials.puredThinGlass(isAppDarkTheme())
    return this.glassPanel(
        backdrop = if (enabled) backdrop else null,
        shape = shape,
        alpha = 1f,
        material = material,
        stroke = null,
        shadow = null,
        shading = false,
        fallback = Modifier.clip(shape).background(hyperSurfaceColor().copy(alpha = 1f)),
    )
}


@Composable
internal fun Modifier.floatingBarShadow(alphaScale: Float = 1f): Modifier {
    val shadowAlpha = (if (isAppDarkTheme()) 0.30f else 0.13f) * alphaScale
    return this.dropShadow(
        shape = RoundedCornerShape(percent = 50),
        block = {
            radius = 14.dp.toPx()
            offset = Offset(0f, 3.dp.toPx())
            color = Color.Black
            alpha = shadowAlpha
        },
    )
}
