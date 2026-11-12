


package top.yukonga.miuix.kmp.basic

import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollHorizontal


@Stable
data class BreadcrumbItem(
    val path: String,
    val text: String? = null,
)


@Composable
fun BreadcrumbBar(
    items: List<BreadcrumbItem>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightIndex: Int = items.lastIndex,
    enabled: Boolean = true,
    colors: BreadcrumbBarColors = BreadcrumbBarDefaults.breadcrumbBarColors(),
    insideMargin: PaddingValues = BreadcrumbBarDefaults.InsideMargin,
    itemMaxWidth: Dp = BreadcrumbBarDefaults.ItemMaxWidth,
    listState: LazyListState? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
) {
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    val resolvedListState = listState ?: rememberLazyListState()
    val hasHighlight = highlightIndex in items.indices
    var lastSettledHighlightIndex by remember(resolvedListState) { mutableIntStateOf(-1) }

    LaunchedEffect(highlightIndex, items.size, resolvedListState) {
        if (!hasHighlight) return@LaunchedEffect
        val animate = lastSettledHighlightIndex >= 0 && lastSettledHighlightIndex != highlightIndex
        resolvedListState.centerItem(highlightIndex, animate)
        lastSettledHighlightIndex = highlightIndex
    }

    val hasScrollRange by remember(resolvedListState) {
        derivedStateOf {
            resolvedListState.canScrollBackward || resolvedListState.canScrollForward
        }
    }
    val reverseScrolling by remember(resolvedListState) {
        derivedStateOf { resolvedListState.layoutInfo.reverseLayout }
    }
    val reverseDirection = ScrollableDefaults.reverseDirection(
        layoutDirection = LocalLayoutDirection.current,
        orientation = Orientation.Horizontal,
        reverseScrolling = reverseScrolling,
    )
    val nestedScrollConnection = remember(resolvedListState, reverseDirection) {
        BreadcrumbBarNestedScrollConnection(resolvedListState, reverseDirection)
    }

    Box(
        modifier = modifier
            .padding(insideMargin)
            .nestedScroll(nestedScrollConnection)
            .overScrollHorizontal(
                nestedScrollToParent = false,
                isEnabled = { hasScrollRange },
            ),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(BreadcrumbBarDefaults.ItemHeight),
            state = resolvedListState,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
            overscrollEffect = null,
            userScrollEnabled = hasScrollRange,
        ) {
            itemsIndexed(items) { index, item ->
                BreadcrumbItemContent(
                    item = item,
                    index = index,
                    itemCount = items.size,
                    highlighted = hasHighlight && index == highlightIndex,
                    enabled = enabled,
                    colors = colors,
                    itemMaxWidth = itemMaxWidth,
                    interactionSource = interactionSource,
                    indication = indication,
                    onClick = { currentOnItemClick(index) },
                )
            }
        }
    }
}

private suspend fun LazyListState.centerItem(index: Int, animate: Boolean) {
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) {
        scrollToItem(index)
    }
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val itemCenter = item.offset + item.size / 2f
    val delta = itemCenter - viewportCenter
    if (animate) {
        animateScrollBy(delta)
    } else {
        scrollBy(delta)
    }
}

private class BreadcrumbBarNestedScrollConnection(
    private val listState: LazyListState,
    private val reverseDirection: Boolean,
) : NestedScrollConnection {
    private fun canConsume(delta: Float): Boolean {
        val logicalDelta = if (reverseDirection) -delta else delta
        return when {
            logicalDelta > 0f -> listState.canScrollForward
            logicalDelta < 0f -> listState.canScrollBackward
            else -> false
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = if (canConsume(available.x)) Offset(available.x, 0f) else Offset.Zero

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity = if (canConsume(available.x)) Velocity(available.x, 0f) else Velocity.Zero
}

@Composable
private fun BreadcrumbItemContent(
    item: BreadcrumbItem,
    index: Int,
    itemCount: Int,
    highlighted: Boolean,
    enabled: Boolean,
    colors: BreadcrumbBarColors,
    itemMaxWidth: Dp,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BreadcrumbSegment(
            text = item.text ?: item.path,
            highlighted = highlighted,
            enabled = enabled,
            colors = colors,
            itemMaxWidth = itemMaxWidth,
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick,
        )
        if (index < itemCount - 1) {
            BreadcrumbSeparator(
                color = if (enabled) colors.separatorColor else colors.disabledColor,
            )
        }
    }
}

@Composable
private fun BreadcrumbSegment(
    text: String,
    highlighted: Boolean,
    enabled: Boolean,
    colors: BreadcrumbBarColors,
    itemMaxWidth: Dp,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    onClick: () -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val backgroundColor: Color
    val textColor: Color
    if (!enabled) {
        backgroundColor = colors.disabledBackgroundColor
        textColor = colors.disabledColor
    } else if (highlighted) {
        backgroundColor = colors.highlightBackgroundColor
        textColor = colors.highlightColor
    } else {
        backgroundColor = colors.backgroundColor
        textColor = colors.color
    }
    Box(
        modifier = Modifier
            .height(BreadcrumbBarDefaults.ItemHeight)
            .widthIn(max = itemMaxWidth)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onClickLabel = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = BreadcrumbBarDefaults.ItemHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BreadcrumbSeparator(
    color: Color,
) {
    val layoutDirection = LocalLayoutDirection.current
    Image(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(width = 10.dp, height = 16.dp)
            .graphicsLayer {
                scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
            },
        imageVector = MiuixIcons.Basic.ArrowRight,
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
    )
}

object BreadcrumbBarDefaults {


    val InsideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)


    val ItemHeight = 32.dp


    val ItemHorizontalPadding = 10.dp


    val ItemMaxWidth = 160.dp


    @Composable
    fun breadcrumbBarColors(
        color: Color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        highlightColor: Color = MiuixTheme.colorScheme.primary,
        disabledColor: Color = MiuixTheme.colorScheme.disabledOnSecondaryVariant,
        separatorColor: Color = MiuixTheme.colorScheme.onSurfaceVariantActions,
        backgroundColor: Color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.1f),
        highlightBackgroundColor: Color = MiuixTheme.colorScheme.primary.copy(alpha = 0.2f),
        disabledBackgroundColor: Color = MiuixTheme.colorScheme.disabledSecondaryVariant,
    ): BreadcrumbBarColors = remember(
        color,
        highlightColor,
        disabledColor,
        separatorColor,
        backgroundColor,
        highlightBackgroundColor,
        disabledBackgroundColor,
    ) {
        BreadcrumbBarColors(
            color = color,
            highlightColor = highlightColor,
            disabledColor = disabledColor,
            separatorColor = separatorColor,
            backgroundColor = backgroundColor,
            highlightBackgroundColor = highlightBackgroundColor,
            disabledBackgroundColor = disabledBackgroundColor,
        )
    }
}

@Immutable
data class BreadcrumbBarColors(
    val color: Color,
    val highlightColor: Color,
    val disabledColor: Color,
    val separatorColor: Color,
    val backgroundColor: Color,
    val highlightBackgroundColor: Color,
    val disabledBackgroundColor: Color,
)


fun List<BreadcrumbItem>.joinToPath(separator: String = "/"): String = joinToString(separator = separator) { it.path }
