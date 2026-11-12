


package top.yukonga.miuix.kmp.blur.highlight

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Immutable
data class Highlight(
    val width: Dp = 0.8.dp,
    @param:FloatRange(from = 0.0, to = 1.0) val alpha: Float = 1f,
    val style: HighlightStyle = HighlightStyle.Default,
) {

    companion object {


        @Stable
        val GlassStrokeBigLight: Highlight = Highlight(style = BloomStroke.GlassStrokeBigLight)


        @Stable
        val GlassStrokeMiddleLight: Highlight = Highlight(style = BloomStroke.GlassStrokeMiddleLight)


        @Stable
        val GlassStrokeSmallLight: Highlight = Highlight(style = BloomStroke.GlassStrokeSmallLight)


        @Stable
        val GlassStrokeBigDark: Highlight = Highlight(style = BloomStroke.GlassStrokeBigDark)


        @Stable
        val GlassStrokeMiddleDark: Highlight = Highlight(style = BloomStroke.GlassStrokeMiddleDark)


        @Stable
        val GlassStrokeSmallDark: Highlight = Highlight(style = BloomStroke.GlassStrokeSmallDark)


        @Stable
        val Default: Highlight = GlassStrokeMiddleLight
    }
}
