package com.hyper.music.ui.basic

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

class RightAlignedTooltipPositionProvider(
    private val spacing: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val margin = (windowSize.width * 0.04f).roundToInt().coerceAtLeast(10)

        var x = anchorBounds.right - popupContentSize.width
        var y = anchorBounds.bottom + spacing

        if (y + popupContentSize.height > windowSize.height) {
            y = anchorBounds.top - popupContentSize.height - spacing
        }
        x = x.coerceIn(
            margin,
            maxOf(margin, windowSize.width - popupContentSize.width - margin),
        )
        y = y.coerceIn(0, maxOf(0, windowSize.height - popupContentSize.height))
        return IntOffset(x, y)
    }
}
