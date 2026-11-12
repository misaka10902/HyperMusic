package com.hyper.music.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

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
