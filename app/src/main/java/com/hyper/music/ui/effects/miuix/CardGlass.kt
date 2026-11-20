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
    // 只订阅 hasImage：拖动壁纸模糊滑块时卡片不参与重组。
    val hasImage by WallpaperManager.hasImageFlow.collectAsState(
        initial = WallpaperManager.state.value.hasImage,
    )
    return hasImage
}

/** 卡片背景不透明度：只由「卡片不透明度」设置项决定，与模糊度无关。 */
@Composable
fun cardGlassAlpha(): Float {
    val opacity by WallpaperManager.cardOpacityFlow.collectAsState(
        initial = WallpaperManager.state.value.cardOpacity,
    )
    if (!cardGlassEnabled()) return 1f
    return opacity.coerceIn(WallpaperManager.MIN_CARD_OPACITY, 1f)
}

/** 卡片模糊半径（px）：由「卡片模糊」设置项决定。 */
@Composable
private fun cardBlurPx(): Float {
    val blur by WallpaperManager.cardBlurFlow.collectAsState(
        initial = WallpaperManager.state.value.cardBlur,
    )
    if (!cardGlassEnabled()) return 0f
    return blur.coerceIn(0f, WallpaperManager.MAX_CARD_BLUR)
}

/**
 * 与 miuix 的 [squircleClip]/[squircleSurface] 完全同一条轮廓的 [Shape]。
 *
 * 卡片本身的底色是 `squircleSurface` 画的，而毛玻璃层（`drawBackdrop` / `background`）
 * 只接受 [Shape]。若这里传 `RoundedCornerShape`，两种曲率不同的轮廓会叠在一起，
 * 圆角处就会出现「外圈圆弧、内圈超椭圆」的双弧线。用同一个 squircle 路径后，
 * 玻璃层与卡片底完全重合，圆角是真正的连续曲率。
 */
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

/**
 * 取一个与 miuix squircle 组件同轮廓的 [Shape]；设备不支持 RuntimeShader 时自动退化为
 * 同半径的圆角矩形，与 [squircleSurface] 的回退保持一致。
 */
@Composable
fun rememberSquircleShape(cornerRadius: Dp): Shape {
    val enabled = isSquircleEnabled()
    return remember(cornerRadius, enabled) { SquircleOutlineShape(cornerRadius, enabled) }
}

/**
 * 卡片毛玻璃：壁纸启用、高级材质开启且模糊度 > 0 时，对卡片区域做真实 backdrop 模糊；
 * 否则回退为纯色半透明背景。模糊度由「卡片模糊」设置项调节。
 */
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
