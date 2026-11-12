package com.hyper.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.CoverSize
import com.hyper.music.music.NetCover
import com.hyper.music.ui.components.hyperSurfaceColor
import com.hyper.music.ui.effects.edgelight.edgeLight
import com.hyper.music.ui.effects.edgelight.rememberDefaultEdgeLight
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.BottomFloatingHeight
import com.hyper.music.ui.utils.BottomFloatingWidthFraction
import com.hyper.music.ui.utils.isAppDarkTheme
import top.yukonga.miuix.kmp.glass.GlassCapsule
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.hyper.music.ui.components.floatingBarShadow

val MiniPlayerHeight = BottomFloatingHeight
const val MiniPlayerWidthFraction = BottomFloatingWidthFraction
val MiniPlayerToNavBarSpacing = 8.dp
val BottomBarAreaHeight = 52.dp + 21.dp

private const val SwipeCommitFraction = 0.20f
private val SwipeRebound = spring<Float>(
    dampingRatio = 0.72f,
    stiffness = 520f,
    visibilityThreshold = 0.5f,
)

@Composable
fun HyperMiniPlayer(
    state: HyperPlayerState,
    backdrop: Backdrop,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenQueue: () -> Unit = {},
    widthFraction: Float = MiniPlayerWidthFraction,
) {
    val track = state.track
    val scope = rememberCoroutineScope()

    val miniSize = MiniPlayerHeight - 16.dp

    val offsetX = remember { Animatable(0f) }
    val solidColor = hyperSurfaceColor().copy(alpha = 1f)
    val frostedSurface = MiuixTheme.colorScheme.surfaceContainer.copy(
        alpha = if (isAppDarkTheme()) 0.8f else 0.72f
    )
    val edgeLight = rememberDefaultEdgeLight()
    val advancedMaterial = LocalAdvancedMaterialEnabled.current

    var corridorWpx by remember { mutableFloatStateOf(0f) }
    var textWpx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val textAlpha = remember { Animatable(1f) }
    var dragTotal by remember { mutableFloatStateOf(0f) }
    fun scrollLo(): Float = -(textWpx - corridorWpx).coerceAtLeast(0f)
    fun slideExitPx(sizeWidth: Float): Float =
        (if (corridorWpx > 0f) corridorWpx else sizeWidth) + with(density) { 40.dp.toPx() }

    Box(
        modifier = modifier.fillMaxWidth(widthFraction)
            .floatingBarShadow(),
        contentAlignment = Alignment.Center,
    ) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .graphicsLayer {
                val p = state.expandProgress.coerceIn(0f, 1f)
                alpha = (1f - p).coerceIn(0f, 1f)
            }
            .then(
                if (advancedMaterial) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { GlassCapsule() },
                        effects = { blur(24f.dp.toPx()) },
                        layerBlock = {},
                        onDrawSurface = { drawRect(frostedSurface) },
                    )
                } else {
                    Modifier.background(
                        color = solidColor,
                        shape = GlassCapsule(),
                    )
                }
            )
            .edgeLight(shape = GlassCapsule(), edgeLight = edgeLight)
            .clip(GlassCapsule())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand,
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragTotal = 0f
                        scope.launch {
                            offsetX.stop()
                            textAlpha.stop()
                        }
                    },
                    onDragCancel = {
                        dragTotal = 0f
                        scope.launch {
                            offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 520f))
                            textAlpha.animateTo(1f, tween(160))
                        }
                    },
                    onDragEnd = {
                        val commitPx = size.width * SwipeCommitFraction
                        val canChange = state.track.hasSong
                        scope.launch {
                            when {
                                canChange && dragTotal < -commitPx -> {
                                    val exit = slideExitPx(size.width.toFloat())
                                    val a = launch {
                                        offsetX.animateTo(-exit, tween(240, easing = FastOutSlowInEasing))
                                    }
                                    val b = launch { textAlpha.animateTo(0f, tween(180)) }
                                    a.join(); b.join()
                                    state.next()
                                    offsetX.snapTo(exit)
                                    textAlpha.snapTo(1f)
                                    offsetX.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
                                }

                                canChange && dragTotal > commitPx -> {
                                    val exit = slideExitPx(size.width.toFloat())
                                    val a = launch {
                                        offsetX.animateTo(exit, tween(240, easing = FastOutSlowInEasing))
                                    }
                                    val b = launch { textAlpha.animateTo(0f, tween(180)) }
                                    a.join(); b.join()
                                    state.previous()
                                    offsetX.snapTo(-exit)
                                    textAlpha.snapTo(1f)
                                    offsetX.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
                                }

                                else -> {
                                    offsetX.animateTo(0f, SwipeRebound)
                                    textAlpha.animateTo(1f, tween(160))
                                }
                            }
                            dragTotal = 0f
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    dragTotal += dragAmount
                    scope.launch {
                        val raw = offsetX.value + dragAmount
                        val next = when {
                            raw < scrollLo() -> scrollLo() + (raw - scrollLo()) * 0.3f
                            raw > 0f -> raw * 0.3f
                            else -> raw
                        }
                        offsetX.snapTo(next)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(miniSize)
                    .coverFrame(circular = true)
                    .coverPlaceholder(),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = track.coverUrl,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    label = "miniCover",
                ) { url ->
                    NetCover(
                        url = url,
                        modifier = Modifier.fillMaxSize(),
                        corner = CoverCircleCorner,
                        contentDescription = track.title,
                        maxSize = CoverSize.LARGE.decode,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
                    .graphicsLayer { alpha = textAlpha.value }
                    .onGloballyPositioned { coords -> corridorWpx = coords.size.width.toFloat() }
                    .padding(horizontal = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .onGloballyPositioned { coords -> textWpx = coords.size.width.toFloat() }
                        .graphicsLayer { translationX = offsetX.value },
                ) {
                    Text(
                        text = track.title,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            MiniPlayPauseControl(
                isPlaying = state.isPlaying,
                onClick = { state.togglePlayPause() },
            )
            MiniPlayerControl(
                imageVector = MiuixIcons.Playlist,
                description = "播放列表",
                iconSize = 23.dp,
                onClick = { onOpenQueue() },
            )
        }
    }
    }
}

@Composable
private fun MiniPlayPauseControl(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "miniPlayPause",
    )
    val tint = MiuixTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = MiuixIcons.Pause,
            contentDescription = "暂停",
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    alpha = progress
                    val s = 0.76f + 0.24f * progress
                    scaleX = s
                    scaleY = s
                    rotationZ = -32f * (1f - progress)
                },
            colorFilter = ColorFilter.tint(tint),
        )
        Image(
            imageVector = MiuixIcons.Play,
            contentDescription = "播放",
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    alpha = 1f - progress
                    val s = 0.76f + 0.24f * (1f - progress)
                    scaleX = s
                    scaleY = s
                    rotationZ = 32f * progress
                },
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

@Composable
private fun MiniPlayerControl(
    res: Int? = null,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    description: String,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = Pair(res, description),
            transitionSpec = {
                (fadeIn(tween(120)) + scaleIn(tween(180, delayMillis = 60), initialScale = 0.6f))
                    .togetherWith(fadeOut(tween(110)) + scaleOut(tween(140), targetScale = 0.6f))
            },
            label = "miniPlayerControl",
        ) { (targetRes, _) ->
            if (targetRes != null) {
                Image(
                    painter = painterResource(targetRes),
                    contentDescription = description,
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                )
            } else {
                Image(
                    imageVector = requireNotNull(imageVector),
                    contentDescription = description,
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                )
            }
        }
    }
}

@Composable
private fun Modifier.coverPlaceholder(): Modifier = this
    .background(
        brush = Brush.linearGradient(
            listOf(
                MiuixTheme.colorScheme.primary.copy(alpha = 0.85f),
                MiuixTheme.colorScheme.primaryContainer,
            )
        ),
    )
