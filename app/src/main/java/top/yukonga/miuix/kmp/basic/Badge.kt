package top.yukonga.miuix.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirst
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.LocalTextStyles
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier.layoutId("anchor"),
                contentAlignment = Alignment.Center,
                content = content,
            )
            Box(modifier = Modifier.layoutId("badge"), content = badge)
        },
    ) { measurables, constraints ->
        val badgePlaceable =
            measurables
                .fastFirst { it.layoutId == "badge" }
                .measure(

                    constraints.copy(minHeight = 0),
                )

        val anchorPlaceable = measurables.fastFirst { it.layoutId == "anchor" }.measure(constraints)

        val firstBaseline = anchorPlaceable[FirstBaseline]
        val lastBaseline = anchorPlaceable[LastBaseline]
        val totalWidth = anchorPlaceable.width
        val totalHeight = anchorPlaceable.height

        layout(
            width = totalWidth,
            height = totalHeight,

            alignmentLines = mapOf(FirstBaseline to firstBaseline, LastBaseline to lastBaseline),
        ) {

            val hasContent = badgePlaceable.width > (BadgeDefaults.Size.roundToPx())
            val badgeHorizontalOffset =
                if (hasContent) BadgeWithContentHorizontalOffset else BadgeOffset
            val badgeVerticalOffset =
                if (hasContent) BadgeWithContentVerticalOffset else BadgeOffset

            anchorPlaceable.placeRelative(0, 0)

            val badgeX =
                minOf(
                    anchorPlaceable.width - badgeHorizontalOffset.roundToPx(),
                    BadgeEndRuler.current(Float.POSITIVE_INFINITY).toInt() - badgePlaceable.width,
                )

            val badgeY =
                maxOf(
                    -badgePlaceable.height + badgeVerticalOffset.roundToPx(),
                    BadgeTopRuler.current(Float.NEGATIVE_INFINITY).toInt(),
                )

            badgePlaceable.placeRelative(badgeX, badgeY)
        }
    }
}

@Composable
fun Badge(
    modifier: Modifier = Modifier,
    containerColor: Color = BadgeDefaults.containerColor,
    contentColor: Color = BadgeDefaults.contentColor,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    val size = if (content != null) BadgeDefaults.LargeSize else BadgeDefaults.Size
    val shape = CircleShape

    Row(
        modifier =
        modifier
            .defaultMinSize(minWidth = size, minHeight = size)
            .background(color = containerColor, shape = shape)
            .then(
                if (content != null) {
                    Modifier.padding(horizontal = BadgeWithContentHorizontalPadding)
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (content != null) {

            val style = MiuixTheme.textStyles.footnote2.copy(
                lineHeight = 16.sp,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
            )
            ProvideContentColorTextStyle(
                contentColor = contentColor,
                textStyle = style,
                content = { content() },
            )
        }
    }
}

object BadgeDefaults {

    val Size = 6.dp

    val LargeSize = 16.dp

    val containerColor: Color
        @Composable get() = MiuixTheme.colorScheme.error

    val contentColor: Color
        @Composable get() = MiuixTheme.colorScheme.onError
}

@Composable
private fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit,
) {
    val textStyles = LocalTextStyles.current
    val badgeTextStyles = remember(textStyles, textStyle) {
        textStyles.copy(main = textStyles.main.merge(textStyle))
    }
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyles provides badgeTextStyles,
        content = content,
    )
}

internal val BadgeWithContentHorizontalPadding = 4.dp

internal val BadgeWithContentHorizontalOffset = 12.dp
internal val BadgeWithContentVerticalOffset = 14.dp

internal val BadgeOffset = 6.dp

internal val BadgeTopRuler = HorizontalRuler()
internal val BadgeEndRuler = VerticalRuler()

internal fun Modifier.badgeBounds() = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(
        width = placeable.width,
        height = placeable.height,
        rulers = {

            BadgeEndRuler provides coordinates.size.width.toFloat()
            BadgeTopRuler provides 0f
        },
    ) {
        placeable.place(0, 0)
    }
}
