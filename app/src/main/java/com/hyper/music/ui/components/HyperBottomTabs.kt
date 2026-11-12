package com.hyper.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.hyper.music.ui.effects.edgelight.edgeLight
import com.hyper.music.ui.effects.edgelight.rememberDefaultEdgeLight
import com.hyper.music.ui.effects.hyper.DampedDragAnimation
import com.hyper.music.ui.effects.hyper.InteractiveHighlight
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.isAppDarkTheme
import top.yukonga.miuix.kmp.glass.GlassCapsule
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size

internal val LocalHyperBottomTabScale =
    staticCompositionLocalOf { { 1f } }

@Composable
internal fun hyperSurfaceColor(): Color {
    val isLightTheme = !isAppDarkTheme()
    return if (isLightTheme) Color(0xFFFFFFFF).copy(0.88f) else Color(0xFF121212).copy(0.84f)
}

private val FloatingShadowBleed = 6.dp
private const val FloatingShadowLayers = 7

@Composable
internal fun Modifier.floatingBarShadow(alphaScale: Float = 1f): Modifier {
    val peakAlpha = (if (isAppDarkTheme()) 0.08f else 0.04f) * alphaScale
    return this.drawBehind {
        val maxBleed = FloatingShadowBleed.toPx()
        val w = size.width
        val h = size.height
        val corner = h / 2f
        val layerAlpha = peakAlpha / FloatingShadowLayers
        for (layer in FloatingShadowLayers downTo 1) {
            val bleed = maxBleed * layer / FloatingShadowLayers
            drawRoundRect(
                color = Color.Black.copy(alpha = layerAlpha),
                topLeft = Offset(-bleed, -bleed),
                size = Size(w + bleed * 2f, h + bleed * 2f),
                cornerRadius = CornerRadius(corner + bleed),
            )
        }
    }
}

@Composable
fun RowScope.HyperBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalHyperBottomTabScale.current
    Column(
        modifier
            .clip(GlassCapsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val scale = scale()
                scaleX = scale
                scaleY = scale
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun HyperBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    containerHeight: Dp = 56.dp,
    highlightHeight: Dp = 48.dp,
    selectorHeight: Dp = 48.dp,
    contentPadding: Dp = 4.dp,
    content: @Composable RowScope.() -> Unit
) {
    val insetPx = with(LocalDensity.current) { contentPadding.toPx() }
    val isLightTheme = !isAppDarkTheme()
    val accentColor =
        if (isLightTheme) Color.Black
        else Color.White
    val solidColor = hyperSurfaceColor().copy(alpha = 1f)
    val frostedSurface = MiuixTheme.colorScheme.surfaceContainer.copy(
        alpha = if (isLightTheme) 0.72f else 0.8f
    )
    val defaultEdgeLight = rememberDefaultEdgeLight()

    BoxWithConstraints(
        modifier.floatingBarShadow(),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - insetPx * 2f) / tabsCount
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()

        val left = remember { Animatable(0f) }
        val right = remember { Animatable(0f) }
        var dragging by remember { mutableStateOf(false) }
        var lastSettledIndex by remember { mutableIntStateOf(selectedTabIndex().coerceIn(0, tabsCount - 1)) }
        var initialized by remember { mutableStateOf(false) }

        fun navBounds(l: Float, r: Float, w: Float, inset: Float): Offset {
            val min = inset.coerceIn(0f, (w.coerceAtLeast(0f)) / 2f)
            val max = (w - min).coerceAtLeast(min)
            val start = l.coerceIn(min, max)
            return Offset(start, r.coerceIn(start, max))
        }
        fun targetLeft(index: Int) = insetPx + index * tabWidth
        fun targetRight(index: Int) = targetLeft(index) + tabWidth
        fun animateSliderTo(index: Int, movingRight: Boolean) {
            val tl = targetLeft(index)
            val tr = targetRight(index)
            animationScope.launch {
                left.animateTo(
                    tl,
                    if (movingRight) GlassMotion.navIndicatorTrail() else GlassMotion.navIndicator(),
                )
            }
            animationScope.launch {
                right.animateTo(
                    tr,
                    if (movingRight) GlassMotion.navIndicator() else GlassMotion.navIndicatorTrail(),
                )
            }
            lastSettledIndex = index
        }

        LaunchedEffect(Unit) {
            val idx = selectedTabIndex().coerceIn(0, tabsCount - 1)
            val init = targetLeft(idx)
            left.snapTo(init)
            right.snapTo(init + tabWidth)
            lastSettledIndex = idx
            initialized = true
        }

        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = GlassMotion.PRESS_SCALE_MIN,
                onDragStarted = { dragging = true },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    onTabSelected(targetIndex)
                    dragging = false
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    val chipLeft = targetValue
                        .coerceIn(0f, (tabsCount - 1).toFloat()) * tabWidth
                    animationScope.launch {
                        left.snapTo(chipLeft)
                        right.snapTo(chipLeft + tabWidth)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                }
        }

        var firstSettle by remember { mutableStateOf(true) }
        LaunchedEffect(currentIndex, dragging, initialized) {
            if (!initialized || dragging || tabWidth <= 0f) return@LaunchedEffect
            if (firstSettle) {
                firstSettle = false
                return@LaunchedEffect
            }
            val movingRight = currentIndex >= lastSettledIndex
            animateSliderTo(currentIndex, movingRight)
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth,
                        size.height / 2f
                    )
                }
            )
        }

    val advancedMaterial = LocalAdvancedMaterialEnabled.current

    Row(
            Modifier
                .then(
                    if (advancedMaterial) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { GlassCapsule() },
                            effects = {
                                blur(24f.dp.toPx())
                            },
                            layerBlock = {},
                            onDrawSurface = { drawRect(frostedSurface) }
                        )
                    } else {
                        Modifier.background(color = solidColor, shape = GlassCapsule())
                    }
                )
                .edgeLight(shape = GlassCapsule(), edgeLight = defaultEdgeLight)
                .then(interactiveHighlight.modifier)
                .height(containerHeight)
                .fillMaxWidth()
                .padding(horizontal = contentPadding, vertical = contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        CompositionLocalProvider(
            LocalHyperBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .height(highlightHeight)
                    .fillMaxWidth()
                    .padding(horizontal = contentPadding)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .clip(GlassCapsule()),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(selectorHeight)
                    .layout { measurable, constraints ->
                        val bounds = navBounds(
                            left.value,
                            right.value,
                            constraints.maxWidth.toFloat(),
                            insetPx,
                        )
                        val start = bounds.x.roundToInt()
                        val width = (bounds.y.roundToInt() - start).coerceAtLeast(0)
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = width, maxWidth = width),
                        )
                        val top = ((constraints.maxHeight - placeable.height) / 2f).roundToInt()
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.placeRelative(IntOffset(start, top))
                        }
                    }
                    .then(dampedDragAnimation.modifier)
                    .graphicsLayer {
                        val rawVelocity = dampedDragAnimation.velocity
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val speed = abs(rawVelocity) / 10f
                        val stretch = (speed * 0.75f).fastCoerceIn(0f, 0.2f)
                        scaleX /= 1f - stretch
                        val normalized = stretch / 0.2f
                        transformOrigin = TransformOrigin(
                            0.5f + normalized * 0.5f * (if (rawVelocity >= 0f) 1f else -1f),
                            0.5f
                        )
                    }
                    .clip(RoundedCornerShape(percent = 50))
                    .drawBehind {
                        val selectorColor = if (isLightTheme) Color.Black.copy(0.07f) else Color.White.copy(0.11f)
                        drawRect(selectorColor)
                    }
            )
        }
    }
}
