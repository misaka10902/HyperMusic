package top.yukonga.miuix.kmp.basic

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.Platform
import top.yukonga.miuix.kmp.utils.platform

@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    showDivider: Boolean = true,
    defaultWindowInsetsPadding: Boolean = true,
    mode: NavigationBarDisplayMode = NavigationBarDisplayMode.IconAndText,
    content: @Composable RowScope.() -> Unit,
) {
    val captionBarPaddings = WindowInsets.captionBar.only(WindowInsetsSides.Bottom).asPaddingValues()
    val captionBarBottomPaddingValue = captionBarPaddings.calculateBottomPadding()

    val animatedCaptionBarHeight by animateDpAsState(
        targetValue = if (captionBarBottomPaddingValue > 0.dp) captionBarBottomPaddingValue else 0.dp,
        animationSpec = tween(durationMillis = 300),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color),
    ) {
        if (showDivider) {
            HorizontalDivider()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalNavigationBarDisplayMode provides mode) {
                content()
            }
        }
        if (defaultWindowInsetsPadding) {
            val navigationBarsPadding = if (platform() != Platform.IOS) {
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            } else {
                20.dp
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val totalHeight = (navigationBarsPadding + animatedCaptionBarHeight).roundToPx()
                        val fixedConstraints = constraints.copy(minHeight = totalHeight, maxHeight = totalHeight)
                        val placeable = measurable.measure(fixedConstraints)
                        layout(placeable.width, totalHeight) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures {  } },
            )
        }
    }
}

@Composable
fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarDefaults.navigationBarItemColors(),
    badge: (@Composable () -> Unit)? = null,
) {
    val itemHeight = NavigationBarDefaults.ItemHeight
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val tint = colors.contentColor(selected, isPressed)
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    val mode = LocalNavigationBarDisplayMode.current

    Column(
        modifier = modifier
            .height(itemHeight)
            .weight(1f)
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .then(if (badge != null) Modifier.badgeBounds() else Modifier),
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = if (mode == NavigationBarDisplayMode.IconAndText || mode == NavigationBarDisplayMode.IconWithSelectedLabel) Arrangement.Top else Arrangement.Center,
    ) {
        when (mode) {
            NavigationBarDisplayMode.IconAndText -> {
                NavigationItemIcon(
                    badge = badge,
                    modifier = Modifier.padding(top = NavigationBarDefaults.IconTopPadding),
                ) { iconModifier ->
                    Image(
                        modifier = iconModifier.size(NavigationBarDefaults.IconSize),
                        imageVector = icon,

                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tint),
                    )
                }
                Text(
                    modifier = Modifier.padding(bottom = NavigationBarDefaults.BottomPadding),
                    text = label,
                    color = tint,
                    textAlign = TextAlign.Center,
                    fontSize = NavigationBarDefaults.LabelFontSize,
                    fontWeight = fontWeight,
                )
            }

            NavigationBarDisplayMode.IconWithSelectedLabel -> {
                val defaultPadding = (itemHeight - NavigationBarDefaults.IconSize) / 2
                val iconTopPadding by animateDpAsState(
                    targetValue = if (selected) NavigationBarDefaults.IconTopPadding else defaultPadding,
                    animationSpec = tween(durationMillis = 300),
                    label = "iconTopPadding",
                )
                val textAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "textAlpha",
                )

                NavigationItemIcon(
                    badge = badge,
                    modifier = Modifier.layout { measurable, constraints ->
                        val topPaddingPx = iconTopPadding.roundToPx()
                        val placeable = measurable.measure(constraints.offset(vertical = -topPaddingPx))
                        layout(placeable.width, placeable.height + topPaddingPx) {
                            placeable.placeRelative(0, topPaddingPx)
                        }
                    },
                ) { iconModifier ->
                    Image(
                        modifier = iconModifier.size(NavigationBarDefaults.IconSize),
                        imageVector = icon,

                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tint),
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(bottom = NavigationBarDefaults.BottomPadding)
                        .graphicsLayer { alpha = textAlpha },
                    text = label,
                    color = tint,
                    textAlign = TextAlign.Center,
                    fontSize = NavigationBarDefaults.LabelFontSize,
                    fontWeight = fontWeight,
                )
            }

            else -> {
                NavigationItemIcon(badge = badge, modifier = Modifier) { iconModifier ->
                    Image(
                        modifier = iconModifier.size(NavigationBarDefaults.IconSize),
                        imageVector = icon,
                        contentDescription = label,
                        colorFilter = ColorFilter.tint(tint),
                    )
                }
            }
        }
    }
}

@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
fun FloatingNavigationBar(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    cornerRadius: Dp = FloatingToolbarDefaults.CornerRadius,
    horizontalAlignment: Alignment.Horizontal = CenterHorizontally,
    horizontalOutSidePadding: Dp = FloatingNavigationBarDefaults.HorizontalOutSidePadding,
    shadowElevation: Dp = FloatingNavigationBarDefaults.ShadowElevation,
    showDivider: Boolean = false,
    defaultWindowInsetsPadding: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)

    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = when (platform()) {
        Platform.IOS -> 36.dp

        else -> {
            if (navBarBottomPadding != 0.dp) 16.dp + navBarBottomPadding else 32.dp
        }
    }

    val captionBarBottomPaddingValue = WindowInsets.captionBar.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val animatedCaptionBarHeight by animateDpAsState(
        targetValue = if (captionBarBottomPaddingValue > 0.dp) captionBarBottomPaddingValue else 0.dp,
        animationSpec = tween(durationMillis = 300),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (horizontalAlignment == Alignment.Start) horizontalOutSidePadding else 0.dp,
                end = if (horizontalAlignment == Alignment.End) horizontalOutSidePadding else 0.dp,
            ),
    ) {
        Row(
            modifier = Modifier
                .selectableGroup()
                .padding(bottom = bottomPaddingValue)
                .defaultMinSize(minHeight = 52.dp)
                .then(
                    if (defaultWindowInsetsPadding) {
                        Modifier.padding(bottom = animatedCaptionBarHeight)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (showDivider) {
                        Modifier
                            .squircleBackground(
                                color = MiuixTheme.colorScheme.dividerLine,
                                cornerRadius = cornerRadius,
                            )
                            .padding(0.75.dp)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (shadowElevation > 0.dp) {
                        Modifier.dropShadow(
                            shape = shape,
                            shadow = Shadow(
                                radius = 10.dp,
                                color = Color.Black,
                                alpha = 0.2f,
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .squircleBackground(color = color, cornerRadius = cornerRadius)
                .then(modifier)
                .padding(horizontal = FloatingNavigationBarDefaults.HorizontalPadding)
                .align(horizontalAlignment)
                .pointerInput(Unit) {
                    detectTapGestures {  }
                },
            horizontalArrangement = Arrangement.spacedBy(FloatingNavigationBarDefaults.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
fun FloatingNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarDefaults.navigationBarItemColors(),
    badge: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val tint = colors.contentColor(selected, isPressed)

    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .then(if (badge != null) Modifier.badgeBounds() else Modifier),
        horizontalAlignment = CenterHorizontally,
    ) {
        NavigationItemIcon(
            badge = badge,
            modifier = Modifier.padding(
                vertical = FloatingNavigationBarDefaults.IconPadding,
                horizontal = FloatingNavigationBarDefaults.IconPadding,
            ),
        ) { iconModifier ->
            Image(
                modifier = iconModifier.size(FloatingNavigationBarDefaults.IconSize),
                imageVector = icon,
                contentDescription = label,
                colorFilter = ColorFilter.tint(tint),
            )
        }
    }
}

@Composable
internal fun NavigationItemIcon(
    badge: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    BadgedBox(modifier = modifier, badge = { badge?.invoke() }) { content(Modifier) }
}

object NavigationBarDefaults {

    val ItemHeight = 64.dp

    val IconSize = 26.dp

    val LabelFontSize = 12.sp

    val IconTopPadding = 8.dp

    val BottomPadding = 8.dp

    val SelectedPressedAlpha = 0.5f

    val UnselectedPressedAlpha = 0.6f

    val UnselectedAlpha = 0.4f

    @Composable
    fun navigationBarItemColors(
        unselectedContentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
        selectedContentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    ): NavigationBarItemColors = remember(unselectedContentColor, selectedContentColor) {
        NavigationBarItemColors(
            unselectedContentColor = unselectedContentColor,
            selectedContentColor = selectedContentColor,
        )
    }
}

@Immutable
data class NavigationBarItemColors(
    private val unselectedContentColor: Color,
    private val selectedContentColor: Color,
) {
    @Stable
    internal fun contentColor(selected: Boolean, isPressed: Boolean): Color = when {
        isPressed -> if (selected) {
            selectedContentColor.copy(alpha = selectedContentColor.alpha * NavigationBarDefaults.SelectedPressedAlpha)
        } else {
            unselectedContentColor.copy(alpha = unselectedContentColor.alpha * NavigationBarDefaults.UnselectedPressedAlpha)
        }

        selected -> selectedContentColor

        else -> unselectedContentColor.copy(alpha = unselectedContentColor.alpha * NavigationBarDefaults.UnselectedAlpha)
    }
}

object FloatingNavigationBarDefaults {

    val HorizontalOutSidePadding = 36.dp

    val ShadowElevation = 1.dp

    val HorizontalPadding = 12.dp

    val ItemSpacing = 12.dp

    val IconSize = 28.dp

    val IconPadding = 10.dp

    val UnselectedAlpha = 0.4f
}

enum class NavigationBarDisplayMode {

    IconAndText,

    IconOnly,

    IconWithSelectedLabel,
}

val LocalNavigationBarDisplayMode = compositionLocalOf { NavigationBarDisplayMode.IconAndText }

@Immutable
data class NavigationItem(
    val label: String,
    val icon: ImageVector,
)
