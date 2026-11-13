package com.hyper.music.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

fun Modifier.horizontalEdgeFade(
    edgeWidth: Dp,
    edgeAlpha: Float = 0f,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fadePx = edgeWidth.toPx().coerceAtMost(size.width / 2f)
        if (fadePx <= 0f) return@drawWithContent
        val fraction = (fadePx / size.width).coerceIn(0f, 0.49f)
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.Black.copy(alpha = edgeAlpha),
                    fraction * 0.35f to Color.Black.copy(alpha = 0.35f + edgeAlpha * 0.65f),
                    fraction * 0.7f to Color.Black.copy(alpha = 0.82f + edgeAlpha * 0.18f),
                    fraction to Color.Black,
                    1f - fraction to Color.Black,
                    1f - fraction * 0.7f to Color.Black.copy(alpha = 0.82f + edgeAlpha * 0.18f),
                    1f - fraction * 0.35f to Color.Black.copy(alpha = 0.35f + edgeAlpha * 0.65f),
                    1f to Color.Black.copy(alpha = edgeAlpha),
                ),
            ),
            blendMode = BlendMode.DstIn,
        )
    }

fun Modifier.bottomContentFade(
    startFromBottom: Dp,
    bottomAlpha: Float = 0.12f,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fadePx = startFromBottom.toPx().coerceAtMost(size.height)
        if (fadePx <= 0f) return@drawWithContent
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(

                    0.00f to Color.Black,
                    0.25f to Color.Black.copy(alpha = 0.88f),
                    0.50f to Color.Black.copy(alpha = 0.62f),
                    0.75f to Color.Black.copy(alpha = 0.34f),
                    1.00f to Color.Black.copy(alpha = bottomAlpha),
                ),
                startY = size.height - fadePx,
                endY = size.height,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
