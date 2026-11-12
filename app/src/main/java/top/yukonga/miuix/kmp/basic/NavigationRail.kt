


package top.yukonga.miuix.kmp.basic

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Sidebar
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    color: Color = MiuixTheme.colorScheme.surface,
    showDivider: Boolean = true,
    defaultWindowInsetsPadding: Boolean = true,
    minWidth: Dp = NavigationRailDefaults.MinWidth,
    expandedWidth: Dp = NavigationRailDefaults.ExpandedWidth,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    NavigationRailImpl(
        modifier = modifier,
        state = null,
        expanded = expanded,
        header = header,
        color = color,
        showDivider = showDivider,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        minWidth = minWidth,
        expandedWidth = expandedWidth,
        expandContentDescription = NavigationRailDefaults.ExpandContentDescription,
        collapseContentDescription = NavigationRailDefaults.CollapseContentDescription,
        scrollState = scrollState,
        content = content,
    )
}


@Composable
fun NavigationRail(
    state: NavigationRailState,
    modifier: Modifier = Modifier,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    color: Color = MiuixTheme.colorScheme.surface,
    showDivider: Boolean = true,
    defaultWindowInsetsPadding: Boolean = true,
    minWidth: Dp = NavigationRailDefaults.MinWidth,
    expandedWidth: Dp = NavigationRailDefaults.ExpandedWidth,
    expandContentDescription: String = NavigationRailDefaults.ExpandContentDescription,
    collapseContentDescription: String = NavigationRailDefaults.CollapseContentDescription,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    NavigationRailImpl(
        modifier = modifier,
        state = state,
        expanded = false,
        header = header,
        color = color,
        showDivider = showDivider,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        minWidth = minWidth,
        expandedWidth = expandedWidth,
        expandContentDescription = expandContentDescription,
        collapseContentDescription = collapseContentDescription,
        scrollState = scrollState,
        content = content,
    )
}

@Composable
private fun NavigationRailImpl(
    state: NavigationRailState?,
    expanded: Boolean,
    header: @Composable (ColumnScope.() -> Unit)?,
    color: Color,
    showDivider: Boolean,
    defaultWindowInsetsPadding: Boolean,
    minWidth: Dp,
    expandedWidth: Dp,
    expandContentDescription: String,
    collapseContentDescription: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isExpandable = state != null
    val isExpanded = state?.isExpanded ?: expanded

    val effectiveExpandedWidth = expandedWidth.coerceAtLeast(minWidth)



    val expandProgress = if (isExpandable) {
        animateFloatAsState(
            targetValue = if (isExpanded) 1f else 0f,
            animationSpec = RailExpandSpring,
            label = "navigationRailExpandProgress",
        )
    } else {
        remember(expanded) { mutableStateOf(if (expanded) 1f else 0f) }
    }
    val expandInfo = remember(expandProgress, minWidth) {
        NavigationRailExpandInfo(progress = expandProgress, collapsedWidth = minWidth)
    }
    Row(


        modifier = modifier
            .fillMaxHeight()
            .background(color)
            .then(
                if (defaultWindowInsetsPadding) {
                    Modifier
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Start))
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val targetWidth = if (isExpandable) {
                        lerp(minWidth, effectiveExpandedWidth, expandProgress.value.coerceIn(0f, 1f))
                    } else if (expanded) {
                        effectiveExpandedWidth
                    } else {
                        minWidth
                    }
                    val width = targetWidth.roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(width, placeable.height) { placeable.placeRelative(0, 0) }
                }
                .fillMaxHeight()
                .then(
                    if (defaultWindowInsetsPadding) {
                        Modifier
                            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                            .verticalScroll(scrollState)
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    } else {
                        Modifier.verticalScroll(scrollState)
                    },
                )
                .selectableGroup()
                .padding(vertical = NavigationRailDefaults.VerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            if (state != null) {
                NavigationRailExpandToggle(
                    state = state,
                    info = expandInfo,
                    expandContentDescription = expandContentDescription,
                    collapseContentDescription = collapseContentDescription,
                )
                Spacer(modifier = Modifier.height(NavigationRailDefaults.HeaderSpacing))
            }
            if (header != null) {
                header()
                Spacer(modifier = Modifier.height(NavigationRailDefaults.HeaderSpacing))
            }
            CompositionLocalProvider(
                LocalNavigationRailExpandInfo provides (if (isExpandable || expanded) expandInfo else null),
            ) {
                content()
            }
        }
        if (showDivider) {
            VerticalDivider()
        }
    }
}


@Composable
private fun NavigationRailExpandToggle(
    state: NavigationRailState,
    info: NavigationRailExpandInfo,
    expandContentDescription: String,
    collapseContentDescription: String,
) {
    IconButton(
        onClick = { state.toggle() },
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            val fraction = info.progress.value.coerceIn(0f, 1f)
            val collapsedX = (info.collapsedWidth.toPx() - placeable.width) / 2f
            val expandedX = NavigationRailDefaults.ExpandedItemHorizontalMargin.toPx()
            val x = lerp(collapsedX, expandedX, fraction).fastRoundToInt()
            layout(constraints.maxWidth, placeable.height) {
                placeable.placeRelative(x, 0)
            }
        },
        minWidth = NavigationRailDefaults.IconSize + NavigationRailDefaults.ExpandedItemContentHorizontalPadding * 2,
        minHeight = NavigationRailDefaults.IconSize + NavigationRailDefaults.ExpandedItemContentVerticalPadding * 2,
    ) {
        Icon(
            modifier = Modifier.size(NavigationRailDefaults.IconSize),
            imageVector = MiuixIcons.Basic.Sidebar,
            contentDescription = if (state.isExpanded) collapseContentDescription else expandContentDescription,
            tint = MiuixTheme.colorScheme.onSurfaceContainer,
        )
    }
}


@Composable
fun NavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val tint = MiuixTheme.colorScheme.onSurfaceContainer
    val indicatorColor = MiuixTheme.colorScheme.surfaceContainerHigh
    val fontWeight = FontWeight.Medium
    val iconColorFilter = remember(tint) { ColorFilter.tint(tint) }

    val expandInfo = LocalNavigationRailExpandInfo.current
    if (expandInfo != null) {
        NavigationRailItemExpandable(
            info = expandInfo,
            selected = selected,
            onClick = onClick,
            icon = icon,
            label = label,
            enabled = enabled,
            tint = tint,
            fontWeight = fontWeight,
            iconColorFilter = iconColorFilter,
            interactionSource = interactionSource,
            badge = badge,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
            )
            .padding(vertical = NavigationRailDefaults.ItemVerticalPadding)
            .then(if (badge != null) Modifier.badgeBounds() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (selected) {
                        Modifier.squircleBackground(
                            color = indicatorColor,
                            cornerRadius = NavigationRailDefaults.ExpandedItemCornerRadius,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(
                    horizontal = NavigationRailDefaults.ExpandedItemContentHorizontalPadding,
                    vertical = NavigationRailDefaults.CollapsedIndicatorVerticalPadding,
                ),
        ) {
            NavigationItemIcon(badge = badge, modifier = Modifier) { iconModifier ->
                Image(
                    modifier = iconModifier.size(NavigationRailDefaults.IconSize),
                    imageVector = icon,

                    contentDescription = null,
                    colorFilter = iconColorFilter,
                )
            }
        }
        Spacer(modifier = Modifier.height(NavigationRailDefaults.IconTextSpacing))
        Text(
            text = label,
            color = tint,
            textAlign = TextAlign.Center,
            fontSize = NavigationRailDefaults.LabelFontSize,
            fontWeight = fontWeight,
        )
    }
}


@Composable
private fun NavigationRailItemExpandable(
    info: NavigationRailExpandInfo,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color,
    fontWeight: FontWeight,
    iconColorFilter: ColorFilter,
    interactionSource: MutableInteractionSource,
    badge: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val indicatorColor = MiuixTheme.colorScheme.surfaceContainerHigh
    val indication = LocalIndication.current
    val progress = info.progress
    Box(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .then(if (badge != null) Modifier.badgeBounds() else Modifier),
    ) {
        Layout(
            modifier = Modifier.fillMaxWidth(),
            content = {


                Box(
                    modifier = Modifier
                        .layoutId("indicator")
                        .clip(ExpandedItemShape)
                        .then(
                            if (selected) {
                                Modifier.squircleBackground(
                                    color = indicatorColor,
                                    cornerRadius = NavigationRailDefaults.ExpandedItemCornerRadius,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .indication(interactionSource, indication),
                )
                NavigationItemIcon(badge = badge, modifier = Modifier.layoutId("icon")) { iconModifier ->
                    Image(
                        modifier = iconModifier.size(NavigationRailDefaults.IconSize),
                        imageVector = icon,

                        contentDescription = null,
                        colorFilter = iconColorFilter,
                    )
                }
                NavigationRailItemLabel(
                    info = info,
                    label = label,
                    tint = tint,
                    fontWeight = fontWeight,
                    modifier = Modifier.layoutId("label"),
                )
            },
        ) { measurables, constraints ->

            val fraction = progress.value.coerceIn(0f, 1f)
            val width = constraints.maxWidth
            val marginPx = NavigationRailDefaults.ExpandedItemHorizontalMargin.roundToPx()
            val leadingInset = NavigationRailDefaults.ExpandedItemContentHorizontalPadding.roundToPx()
            val expandedSpacing = NavigationRailDefaults.ExpandedItemIconTextSpacing.roundToPx()

            val iconPlaceable = measurables.fastFirst { it.layoutId == "icon" }
                .measure(constraints.copy(minWidth = 0, minHeight = 0))
            val iconW = iconPlaceable.width
            val iconH = iconPlaceable.height




            val expandedLabelMax = (width - 2 * marginPx - 2 * leadingInset - iconW - expandedSpacing).coerceAtLeast(0)
            val labelPlaceable = measurables.fastFirst { it.layoutId == "label" }.measure(
                Constraints(maxWidth = lerp(width, expandedLabelMax, fraction), maxHeight = constraints.maxHeight),
            )
            val labelW = labelPlaceable.width
            val labelH = labelPlaceable.height

            val topPad = NavigationRailDefaults.ItemVerticalPadding.roundToPx()
            val collapsedSpacing = NavigationRailDefaults.IconTextSpacing.roundToPx()
            val collapsedIndVPad = NavigationRailDefaults.CollapsedIndicatorVerticalPadding.roundToPx()
            val expandedVPad = NavigationRailDefaults.ExpandedItemContentVerticalPadding.roundToPx()



            val collapsedHeight = topPad + iconH + collapsedIndVPad * 2 + collapsedSpacing + labelH + topPad
            val expandedHeight = expandedVPad * 2 + maxOf(iconH, labelH)
            val height = lerp(collapsedHeight.toFloat(), expandedHeight.toFloat(), fraction).fastRoundToInt()



            val collapsedIconX = (info.collapsedWidth.toPx() - iconW) / 2f
            val expandedIconX = (marginPx + leadingInset).toFloat()
            val iconX = lerp(collapsedIconX, expandedIconX, fraction).fastRoundToInt()
            val iconY = lerp((topPad + collapsedIndVPad).toFloat(), (height - iconH) / 2f, fraction).fastRoundToInt()




            val indicatorVPad = lerp(collapsedIndVPad.toFloat(), expandedVPad.toFloat(), fraction).fastRoundToInt()
            val indicatorH = iconH + indicatorVPad * 2
            val indicatorY = iconY - indicatorVPad
            val indicatorX = lerp(collapsedIconX - leadingInset, marginPx.toFloat(), fraction).fastRoundToInt()
            val indicatorW = lerp((iconW + leadingInset * 2).toFloat(), (width - marginPx * 2).toFloat(), fraction)
                .fastRoundToInt()
                .coerceAtLeast(0)
            val indicatorPlaceable = measurables.fastFirst { it.layoutId == "indicator" }
                .measure(Constraints.fixed(indicatorW, indicatorH))


            val collapsedLabelY = topPad + iconH + collapsedIndVPad * 2 + collapsedSpacing
            val collapsedLabelX = collapsedIconX + iconW / 2f - labelW / 2f
            val expandedLabelX = (marginPx + leadingInset + iconW + expandedSpacing).toFloat()
            val labelX = lerp(collapsedLabelX, expandedLabelX, fraction).fastRoundToInt().coerceAtLeast(0)
            val labelY = lerp(collapsedLabelY.toFloat(), (height - labelH) / 2f, fraction).fastRoundToInt()

            layout(width, height) {
                indicatorPlaceable.placeRelative(indicatorX, indicatorY)
                iconPlaceable.placeRelative(iconX, iconY)
                labelPlaceable.placeRelative(labelX, labelY)
            }
        }
    }
}


@Composable
private fun NavigationRailItemLabel(
    info: NavigationRailExpandInfo,
    label: String,
    tint: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val fraction = info.progress.value.coerceIn(0f, 1f)
    val collapsedSp = NavigationRailDefaults.LabelFontSize.value
    val expandedSp = NavigationRailDefaults.ExpandedLabelFontSize.value
    Text(
        text = label,
        modifier = modifier,
        color = tint,
        fontSize = lerp(collapsedSp, expandedSp, fraction).sp,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}


private val RailExpandSpring: SpringSpec<Float> =
    folmeSpring(damping = 1f, response = 0.35f, visibilityThreshold = 0.001f)


private val ExpandedItemShape = RoundedCornerShape(NavigationRailDefaults.ExpandedItemCornerRadius)


object NavigationRailDefaults {

    val MinWidth = 80.dp


    val ExpandedWidth = 240.dp


    val VerticalPadding = 24.dp


    val HeaderSpacing = 24.dp


    val IconSize = 28.dp


    val IconTextSpacing = 4.dp


    val ItemVerticalPadding = 12.dp


    val LabelFontSize = 12.sp


    val ExpandedLabelFontSize = 16.sp


    val ExpandedItemHorizontalMargin = 12.dp


    val ExpandedItemCornerRadius = 16.dp


    val CollapsedIndicatorVerticalPadding = 4.dp


    val ExpandedItemContentHorizontalPadding = 14.dp


    val ExpandedItemContentVerticalPadding = 14.dp


    val ExpandedItemIconTextSpacing = 16.dp


    val ExpandContentDescription = "Expand navigation rail"


    val CollapseContentDescription = "Collapse navigation rail"
}


@Stable
internal class NavigationRailExpandInfo(
    val progress: State<Float>,
    val collapsedWidth: Dp,
)


internal val LocalNavigationRailExpandInfo = compositionLocalOf<NavigationRailExpandInfo?> { null }


enum class NavigationRailValue {

    Collapsed,


    Expanded,
}


@Stable
class NavigationRailState(initialValue: NavigationRailValue = NavigationRailValue.Collapsed) {

    var currentValue: NavigationRailValue by mutableStateOf(initialValue)
        private set


    val isExpanded: Boolean get() = currentValue == NavigationRailValue.Expanded


    fun expand() {
        currentValue = NavigationRailValue.Expanded
    }


    fun collapse() {
        currentValue = NavigationRailValue.Collapsed
    }


    fun toggle() {
        currentValue = if (isExpanded) NavigationRailValue.Collapsed else NavigationRailValue.Expanded
    }

    companion object {

        val Saver: Saver<NavigationRailState, Int> = Saver(
            save = { it.currentValue.ordinal },
            restore = { NavigationRailState(NavigationRailValue.entries.getOrNull(it) ?: NavigationRailValue.Collapsed) },
        )
    }
}


@Composable
fun rememberNavigationRailState(
    initialValue: NavigationRailValue = NavigationRailValue.Collapsed,
): NavigationRailState = rememberSaveable(saver = NavigationRailState.Saver) {
    NavigationRailState(initialValue)
}
