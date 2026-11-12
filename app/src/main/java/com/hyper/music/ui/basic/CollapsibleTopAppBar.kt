package com.hyper.music.ui.basic

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.hyper.music.music.WallpaperManager
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.isAppDarkTheme
import com.hyper.music.ui.utils.rememberWindowSizeDp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Stable
class SharedScrollBehavior(
    private val delegate: ScrollBehavior,
) : ScrollBehavior by delegate {
    var currentHeightPx by mutableFloatStateOf(0f)
}

@Composable
fun rememberSharedScrollBehavior(): SharedScrollBehavior {
    val delegate = MiuixScrollBehavior()
    return remember(delegate) { SharedScrollBehavior(delegate) }
}

object CollapsibleTopAppBarDefaults {
    val CollapsedHeight = 52.dp
    val TitlePadding = 28.dp
    val TitleWidthFraction = 0.9f
    val MaxLargeTitleBlur = 6.dp
    val MaxSmallTitleBlur = 6.dp
}

@Composable
fun CollapsibleTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: String = title,
    subtitle: String? = null,
    showLargeTitle: Boolean? = null,
    showSmallTitle: Boolean? = null,
    showShadow: Boolean? = null,
    showGradientOverlay: Boolean = true,
    scrollBehavior: SharedScrollBehavior? = null,
    contentPadding: (Dp) -> Unit = {},
    startAction: @Composable ((backdropAlpha: Float, shadowAlpha: Float) -> Unit)? = null,
    endAction: @Composable ((backdropAlpha: Float, shadowAlpha: Float) -> Unit)? = null,
    gradientMaskHeight: Dp = CollapsibleTopAppBarDefaults.CollapsedHeight + 70.dp,
    onAlphaChanged: (backdropAlpha: Float, shadowAlpha: Float) -> Unit = { _, _ -> },
) {
    val state = scrollBehavior?.state

    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val wallpaperActive = WallpaperManager.state.collectAsState().value.hasImage

    val isWideWindow = rememberWindowSizeDp().width >= 600.dp
    val effectiveShowLargeTitle = showLargeTitle ?: !isWideWindow

    val scrolledOffset = remember(scrollBehavior) {
        { scrollBehavior?.state?.heightOffset ?: 0f }
    }
    val largeTitleAlpha = remember(scrollBehavior) {
        {
            val frac = scrollBehavior?.state?.collapsedFraction ?: 0f
            1f - (frac * 3f).coerceIn(0f, 1f)
        }
    }
    val largeTitleBlur: () -> Dp = remember(scrollBehavior) {
        {
            val frac = scrollBehavior?.state?.collapsedFraction ?: 0f
            Dp((frac * 4f).coerceIn(0f, 1f) * CollapsibleTopAppBarDefaults.MaxLargeTitleBlur.value)
        }
    }
    val updateHeightOffsetLimit = remember(scrollBehavior) {
        { height: Int ->
            scrollBehavior?.state?.let { s ->
                val limit = -height.toFloat()
                if (s.heightOffsetLimit != limit) s.heightOffsetLimit = limit
            }
            Unit
        }
    }

    LaunchedEffect(effectiveShowLargeTitle, scrollBehavior) {
        if (!effectiveShowLargeTitle) {
            scrollBehavior?.state?.heightOffsetLimit = -1f
        }
    }

    val smallTitleVisible by remember(state, effectiveShowLargeTitle, showSmallTitle) {
        derivedStateOf {
            showSmallTitle
                ?: if (!effectiveShowLargeTitle) true
                else (state?.collapsedFraction ?: 0f) * 3f >= 1f
        }
    }
    val density = LocalDensity.current

    val showButtonShadow = remember(scrollBehavior, showShadow, effectiveShowLargeTitle) {
        derivedStateOf {
            if (showShadow != null) return@derivedStateOf showShadow
            val st = scrollBehavior?.state ?: return@derivedStateOf false
            st.heightOffset < -2f || st.collapsedFraction > 0.01f
        }
    }
    val shadowAlpha = remember { Animatable(if (showButtonShadow.value) 1f else 0f) }
    val backdropAlpha = remember { Animatable(if (showButtonShadow.value) 1f else 0f) }
    LaunchedEffect(showButtonShadow.value) {
        val target = if (showButtonShadow.value) 1f else 0f
        val spec = if (showButtonShadow.value) {
            folmeSpring(damping = 1.0f, response = 0.32f)
        } else {
            folmeSpring<Float>(damping = 1.0f, response = 0.28f)
        }
        launch { shadowAlpha.animateTo(target, spec) }
        launch { backdropAlpha.animateTo(target, spec) }
    }
    val smallTitleAlpha = remember { Animatable(if (smallTitleVisible) 1f else 0f) }
    val smallTitleTranslationY = remember { Animatable(if (smallTitleVisible) 0f else 20f) }
    val smallTitleBlur =
        remember { Animatable(if (smallTitleVisible) 0f else CollapsibleTopAppBarDefaults.MaxSmallTitleBlur.value) }

    LaunchedEffect(smallTitleVisible) {
        if (smallTitleVisible) {
            val showSpec = folmeSpring<Float>(damping = 1.0f, response = 0.3f)
            launch { smallTitleAlpha.animateTo(1f, showSpec) }
            launch { smallTitleTranslationY.animateTo(0f, showSpec) }
            launch { smallTitleBlur.animateTo(0f, showSpec) }
        } else {
            val hideSpec = folmeSpring<Float>(damping = 1.0f, response = 0.15f)
            launch { smallTitleAlpha.animateTo(0f, hideSpec) }
            launch { smallTitleTranslationY.animateTo(20f, hideSpec) }
            launch {
                smallTitleBlur.animateTo(
                    CollapsibleTopAppBarDefaults.MaxSmallTitleBlur.value,
                    hideSpec
                )
            }
        }
    }

    val gradientAlpha = remember { Animatable(0f) }
    LaunchedEffect(showButtonShadow.value) {
        val target = if (showButtonShadow.value) 1f else 0f
        val spec = if (showButtonShadow.value) {
            folmeSpring(damping = 1.0f, response = 0.32f)
        } else {
            folmeSpring<Float>(damping = 1.0f, response = 0.28f)
        }
        gradientAlpha.animateTo(target, spec)
    }
    val gradientColor = if (isAppDarkTheme()) Color.Black else Color.White

    LaunchedEffect(backdropAlpha.value, shadowAlpha.value) {
        onAlphaChanged(backdropAlpha.value, shadowAlpha.value)
    }

    Box {
        if (showGradientOverlay && !wallpaperActive) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(gradientMaskHeight)
                    .graphicsLayer { alpha = gradientAlpha.value }
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to gradientColor.copy(alpha = 0.8f),
                                0.7f to gradientColor.copy(alpha = 0.4f),
                                0.9f to gradientColor.copy(alpha = 0.15f),
                                1f to Color.Transparent
                            )
                        )
                    }
            )
        }

    val statusBarInset = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topInset = if (statusBarInset > 0.dp) statusBarInset else 64.dp

    Layout(
            {
                if (startAction != null) {
                    Box(
                        Modifier
                            .layoutId("startAction")
                            .zIndex(2f),
                    ) {
                        startAction(backdropAlpha.value, shadowAlpha.value)
                    }
                }
                Box(
                    Modifier
                        .layoutId("title")
                        .padding(horizontal = CollapsibleTopAppBarDefaults.TitlePadding)
                        .graphicsLayer {
                            alpha = smallTitleAlpha.value
                            translationY = smallTitleTranslationY.value
                        }
                        .then(

                            if (advancedMaterial && !wallpaperActive) {
                                Modifier.blur(Dp(smallTitleBlur.value))
                            } else Modifier
                        ),
                ) {
                    Text(
                        text = title,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
                if (endAction != null) {
                    Box(
                        Modifier
                            .layoutId("endAction")
                            .zIndex(2f),
                    ) {
                        endAction(backdropAlpha.value, shadowAlpha.value)
                    }
                }
                if (effectiveShowLargeTitle) {
                    Box(
                        Modifier
                            .layoutId("largeTitle")
                            .padding(top = CollapsibleTopAppBarDefaults.CollapsedHeight)
                            .padding(horizontal = CollapsibleTopAppBarDefaults.TitlePadding)
                            .graphicsLayer { alpha = largeTitleAlpha() }
                            .then(
                                if (advancedMaterial && !wallpaperActive) {
                                    Modifier.blur(largeTitleBlur())
                                } else Modifier
                            ),
                    ) {
                        Column(
                            modifier = Modifier
                                .onSizeChanged { updateHeightOffsetLimit(it.height) }
                                .animateContentSize(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                ),
                        ) {
                            Text(
                                text = largeTitle,
                                color = MiuixTheme.colorScheme.onSurface,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Normal,
                            )
                            val subtitleVisible by animateFloatAsState(
                                targetValue = if (subtitle != null) 1f else 0f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                label = "subtitleAlpha",
                            )
                            if (subtitle != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = subtitle.orEmpty(),
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.graphicsLayer { alpha = subtitleVisible },
                                )
                            }
                        }
                    }
                }
            },
            modifier = modifier
                .onGloballyPositioned { coords ->
                    val heightDp = with(density) { coords.size.height.toDp() }
                    scrollBehavior?.currentHeightPx = coords.size.height.toFloat()
                    contentPadding(heightDp)
                }
                .padding(top = topInset),
        ) { measurables, constraints ->
            val backButtonPlaceable = measurables
                .firstOrNull { it.layoutId == "backButton" }
                ?.measure(constraints.copy(minWidth = 0, minHeight = 0))

            val startActionPlaceable = measurables
                .firstOrNull { it.layoutId == "startAction" }
                ?.measure(constraints.copy(minWidth = 0, minHeight = 0))

            val endActionPlaceable = measurables
                .firstOrNull { it.layoutId == "endAction" }
                ?.measure(constraints.copy(minWidth = 0, minHeight = 0))

            val maxTitleWidth = if (constraints.maxWidth == Constraints.Infinity) {
                constraints.maxWidth
            } else {
                constraints.maxWidth
            }
            val titleMaxWidth = if (maxTitleWidth == Constraints.Infinity) {
                maxTitleWidth
            } else {
                (maxTitleWidth * CollapsibleTopAppBarDefaults.TitleWidthFraction).roundToInt()
            }

            val titlePlaceable = measurables
                .fastFirst { it.layoutId == "title" }
                .measure(constraints.copy(minWidth = 0, maxWidth = titleMaxWidth, minHeight = 0))

            val largeTitlePlaceable = if (effectiveShowLargeTitle) {
                measurables
                    .firstOrNull { it.layoutId == "largeTitle" }
                    ?.measure(
                        constraints.copy(
                            minWidth = 0,
                            minHeight = 0,
                            maxHeight = Constraints.Infinity,
                        ),
                    )
            } else null

            val collapsedHeightPx = CollapsibleTopAppBarDefaults.CollapsedHeight.roundToPx()
            val expansion =
                ((largeTitlePlaceable?.height ?: 0) - collapsedHeightPx).coerceAtLeast(0)

            val offset = scrolledOffset()
            val collapseFraction = if (expansion > 0 && !offset.isNaN()) {
                (abs(offset) / expansion.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val barHeight = lerp(
                start = collapsedHeightPx.toFloat(),
                stop = (collapsedHeightPx + expansion).toFloat(),
                fraction = 1f - collapseFraction,
            ).roundToInt()

            val verticalCenter = collapsedHeightPx / 2

            layout(constraints.maxWidth, barHeight) {
                backButtonPlaceable?.placeRelative(
                    x = with(density) { 16.dp.roundToPx() },
                    y = verticalCenter - backButtonPlaceable.height / 2,
                )

                startActionPlaceable?.placeRelative(
                    x = with(density) { 16.dp.roundToPx() },
                    y = verticalCenter - startActionPlaceable.height / 2,
                )

                titlePlaceable.placeRelative(
                    x = (constraints.maxWidth - titlePlaceable.width) / 2,
                    y = verticalCenter - titlePlaceable.height / 2,
                )

                endActionPlaceable?.placeRelative(
                    x = constraints.maxWidth - with(density) { 16.dp.roundToPx() } - endActionPlaceable.width,
                    y = verticalCenter - endActionPlaceable.height / 2,
                )

                val largeTitleY = if (offset.isNaN()) 0 else offset.roundToInt()
                largeTitlePlaceable?.placeRelative(x = 0, y = largeTitleY)
            }
        }
    }
}
