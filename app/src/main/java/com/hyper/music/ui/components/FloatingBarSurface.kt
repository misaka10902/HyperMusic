package com.hyper.music.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyper.music.ui.utils.isAppDarkTheme

/** 悬浮条（迷你播放器、多选栏）的纯色兜底背景。 */
@Composable
internal fun hyperSurfaceColor(): Color {
    val isLightTheme = !isAppDarkTheme()
    return if (isLightTheme) Color(0xFFFFFFFF).copy(0.88f) else Color(0xFF121212).copy(0.84f)
}

/**
 * 悬浮条的投影。
 *
 * 之前用 `drawBehind` 叠了 7 层圆角矩形，绘制范围刻意超出节点边界；
 * 一旦父级被提升为独立图层（透明度 / 缩放动画），超出部分就会被裁成直角，
 * 表现为「阴影出现方形切边」。改用 Compose 的 [dropShadow]：
 * 阴影由框架沿形状绘制，并自行预留外扩区域，不会再被裁掉。
 */
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
