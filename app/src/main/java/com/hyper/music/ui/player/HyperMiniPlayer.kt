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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.PlaybackController
import com.hyper.music.music.NetCover
import com.hyper.music.ui.components.floatingBarGlass
import com.hyper.music.ui.effects.edgelight.edgeLight
import com.hyper.music.ui.effects.edgelight.rememberDefaultEdgeLight
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.BottomFloatingHeight
import com.hyper.music.ui.utils.BottomFloatingWidthFraction
import com.hyper.music.ui.GlassCapsule
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.glass.OverseasMusic
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
    val settings by MusicLibrary.settings.collectAsState()
    val barLyric by PlaybackController.playBarLyric.collectAsState()

    val showBarLyric = settings.playBarLyrics && track.hasSong && barLyric.isNotBlank()

    val miniSize = MiniPlayerHeight - 16.dp

    val offsetX = remember { Animatable(0f) }
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
            .floatingBarGlass(
                backdrop = backdrop,
                shape = GlassCapsule(),
                enabled = advancedMaterial,
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
                if (track.hasSong) {
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
                } else {

                    Icon(
                        imageVector = MiuixIcons.Glass.OverseasMusic,
                        contentDescription = null,
                        modifier = Modifier.size(miniSize * 0.56f),
                        tint = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.6f),
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
                val corridorDp = with(density) { corridorWpx.toDp() }
                Column(
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .onGloballyPositioned { coords -> textWpx = coords.size.width.toFloat() }
                        .graphicsLayer { translationX = offsetX.value },
                ) {

                    AnimatedContent(
                        targetState = if (showBarLyric) barLyric else track.title,
                        transitionSpec = {
                            (
                                slideInVertically(
                                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                                ) { h -> h } + fadeIn(tween(170))
                                ).togetherWith(
                                slideOutVertically(
                                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                                ) { h -> -h } + fadeOut(tween(140)),
                            )
                        },
                        label = "miniBarTitle",
                    ) { text ->
                        Text(
                            text = text,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (corridorWpx > 0f) Modifier.widthIn(max = corridorDp) else Modifier,
                        )
                    }
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
    .background(MiuixTheme.colorScheme.secondaryContainer)
