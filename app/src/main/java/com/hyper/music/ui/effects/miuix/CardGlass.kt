package com.hyper.music.ui.effects.miuix

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.hyper.music.music.WallpaperManager
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.squircle.addSquircleRect
import top.yukonga.miuix.kmp.squircle.isSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun cardGlassEnabled(): Boolean {

    val hasImage by WallpaperManager.hasImageFlow.collectAsState(
        initial = WallpaperManager.state.value.hasImage,
    )
    return hasImage
}


@Composable
fun cardGlassAlpha(): Float {
    val opacity by WallpaperManager.cardOpacityFlow.collectAsState(
        initial = WallpaperManager.state.value.cardOpacity,
    )
    if (!cardGlassEnabled()) return 1f
    return opacity.coerceIn(WallpaperManager.MIN_CARD_OPACITY, 1f)
}


@Composable
private fun cardBlurPx(): Float {
    val blur by WallpaperManager.cardBlurFlow.collectAsState(
        initial = WallpaperManager.state.value.cardBlur,
    )
    if (!cardGlassEnabled()) return 0f
    return blur.coerceIn(0f, WallpaperManager.MAX_CARD_BLUR)
}


private class SquircleOutlineShape(
    private val cornerRadius: Dp,
    private val squircleEnabled: Boolean,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val path = Path()
        path.addSquircleRect(
            width = size.width,
            height = size.height,
            cornerRadius = with(density) { cornerRadius.toPx() },
            squircleEnabled = squircleEnabled,
        )
        return Outline.Generic(path)
    }
}


@Composable
fun rememberSquircleShape(cornerRadius: Dp): Shape {
    val enabled = isSquircleEnabled()
    return remember(cornerRadius, enabled) { SquircleOutlineShape(cornerRadius, enabled) }
}


@Composable
fun Modifier.cardGlass(shape: Shape): Modifier {
    val alpha = cardGlassAlpha()
    val blurPx = cardBlurPx()
    val scheme = MiuixTheme.colorScheme
    val backdrop = LocalActiveBackdrop.current
    val advanced = LocalAdvancedMaterialEnabled.current
    val tint = scheme.surfaceContainer.copy(alpha = alpha)
    return if (advanced && backdrop != null && blurPx > 0.01f) {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = { blur(blurPx) },
            onDrawSurface = { drawRect(color = tint) },
        )
    } else {
        this.background(color = tint, shape = shape)
    }
}

@Composable
fun cardGlassColors(): CardColors = if (cardGlassEnabled()) {
    CardDefaults.defaultColors(
        color = Color.Transparent,
        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
    )
} else {
    CardDefaults.defaultColors()
}
