package com.hyper.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.CoverLoader
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.LocalLyrics
import com.hyper.music.music.LyricLine
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.NetCover
import com.hyper.music.ui.utils.saveCoverOnLongPress
import com.hyper.music.music.PlayMode
import com.hyper.music.music.buildLyrics
import com.hyper.music.music.currentLineIndex
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.isCompactWindow
import com.hyper.music.ui.utils.rememberWindowSizeDp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.layout.fillMaxHeight
import kotlinx.coroutines.withContext

private val PageTextPrimary = Color.White
private val PageTextSecondary = Color.White.copy(alpha = 0.66f)
private val ControlTint = Color.White
private val ControlTintDim = Color.White.copy(alpha = 0.78f)

private object PlayerSpacing {
    val ProgressSideInset = 20.dp
    val ProgressToTime = 4.dp
    val TimeToControls = 20.dp
    val ControlsToFunctions = 16.dp
    val MainControlGap = 20.dp
    val FunctionRowInset = 24.dp
}

data class PlayerReveal(
    val header: Float = 1f,
    val cover: Float = 1f,
    val controls: Float = 1f,
)

@Composable
fun HyperPlayerPageContent(
    state: HyperPlayerState,
    modifier: Modifier = Modifier,
    reveal: PlayerReveal = PlayerReveal(),
    onOpenQueue: () -> Unit = {},
    onMore: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val track = state.track
    val favorites by MusicLibrary.favorites.collectAsState()
    val isFavorite = state.playback.song?.let { song -> favorites.any { it.id == song.id } } ?: false
    fun toggleFavorite() {
        val song = state.playback.song ?: return

        MusicLibrary.toggleFavorite(song)
    }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        .coerceAtLeast(26.dp)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        .coerceAtLeast(16.dp)
    val contentTopPadding = statusBarPadding + 26.dp

    val coverScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.8f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "coverScale",
    )

    val settings by MusicLibrary.settings.collectAsState()
    val circular = settings.coverCircular
    val pageCorner = if (circular) CoverCircleCorner else PlayPageCoverCorner
    var showLyrics by remember { mutableStateOf(false) }
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    val lyricSong = state.playback.song
    var lyricLoading by remember { mutableStateOf(false) }
    val lyrics by produceState<List<LyricLine>>(
        initialValue = emptyList(),
        key1 = lyricSong?.id.orEmpty(),
    ) {
        value = emptyList()
        lyricLoading = true
        val song = lyricSong
        val data = if (song == null) {
            null
        } else if (song.isLocal) {

            runCatching {
                withContext(kotlinx.coroutines.Dispatchers.IO) { LocalLyrics.read(context, song) }
            }.getOrNull()
        } else if (track.lyricId.isNotBlank()) {
            runCatching { GdApi.fullLyric(song) }.getOrNull()
        } else {
            null
        }
        value = if (data != null) buildLyrics(data.lyric, data.translation) else emptyList()
        lyricLoading = false
    }
    val lyricIndex = currentLineIndex(lyrics, state.positionMs)

    var cachedLyrics by remember { mutableStateOf(emptyList<LyricLine>()) }
    LaunchedEffect(lyrics) { if (lyrics.isNotEmpty()) cachedLyrics = lyrics }
    val shownLyrics = if (lyrics.isNotEmpty()) lyrics
        else if (lyricLoading) cachedLyrics
        else emptyList()

    val coverBackdrop = rememberLayerBackdrop {
        drawRect(Color.Transparent)
        drawContent()
    }
    val artwork by produceState<Pair<ImageBitmap?, AlbumPalette?>>(
        initialValue = null to null,
        key1 = track.coverUrl,
    ) {
        val url = track.coverUrl
        if (url.isNullOrBlank()) return@produceState
        val bmp = withContext(Dispatchers.Default) {
            runCatching { CoverLoader.load(url, maxSize = CoverSize.LARGE.decode) }.getOrNull()
        }
        val pal = bmp?.let {
            withContext(Dispatchers.Default) {
                runCatching { AlbumPaletteExtractor.paletteOf(it.asAndroidBitmap()) }.getOrNull()
            }
        }
        value = bmp to pal
    }
    val coverBitmap = artwork.first
    val palette = artwork.second

    val fallback = Color(0xFF6E6E73)
    val topColor by animateColorAsState(
        targetValue = palette?.top ?: fallback.copy(alpha = 0.92f),
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "bgTop",
    )
    val middleColor by animateColorAsState(
        targetValue = palette?.middle ?: fallback.copy(alpha = 0.55f),
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "bgMiddle",
    )
    val bottomColor by animateColorAsState(
        targetValue = palette?.bottom ?: Color.Black,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "bgBottom",
    )
    val accentColor by animateColorAsState(
        targetValue = palette?.accent ?: fallback,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "bgAccent",
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSize = rememberWindowSizeDp()
        val isLandscape = maxWidth > maxHeight
        val compact = !isLandscape && (maxHeight < 600.dp || isCompactWindow(windowSize))
        val scale = if (compact) {
            minOf(maxWidth / 400.dp, maxHeight / 620.dp).coerceIn(0.68f, 1f)
        } else {
            1f
        }

        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = state.expandProgress.coerceIn(0f, 1f) }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .layerBackdrop(coverBackdrop),
        ) {
            Crossfade(
                targetState = coverBitmap,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                label = "playerBgCover",
            ) { bmp ->
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().scale(1.2f),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color(0xFF6E6E73),
                                    1f to Color(0xFF1C1C1E),
                                )
                            ),
                    )
                }
            }
        }

        val advancedMaterial = LocalAdvancedMaterialEnabled.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (advancedMaterial) {
                        Modifier.drawBackdrop(
                            backdrop = coverBackdrop,
                            shape = { RectangleShape },
                            effects = { blur(48f.dp.toPx()) },
                        )
                    } else {
                        Modifier
                    }
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to topColor.copy(alpha = if (advancedMaterial && coverBitmap != null) 0.82f else 1f),
                        0.42f to middleColor.copy(alpha = if (advancedMaterial && coverBitmap != null) 0.78f else 1f),
                        1f to bottomColor.copy(alpha = if (advancedMaterial && coverBitmap != null) 0.86f else 1f),
                    )
                )
    ,
        )

        }

        val isTablet = maxWidth >= 600.dp && maxHeight >= 600.dp && isLandscape
        if (isTablet) {
            TabletPlayerUi(
                state = state,
                track = track,
                reveal = reveal,
                isFavorite = isFavorite,
                circular = circular,
                pageCorner = pageCorner,
                onToggleFavorite = { toggleFavorite() },
                onOpenQueue = onOpenQueue,
                onMore = onMore,
                onOpenSleepTimer = onOpenSleepTimer,
                lyrics = lyrics,
                lyricIndex = lyricIndex,
                haptic = haptic,
                coverScale = coverScale,
                showLyricsTranslation = settings.showLyricsTranslation,
                lyricCentered = settings.lyricCentered,
                titleCentered = settings.titleCentered,
            )
        } else if (isLandscape) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
            LandscapePlayerUi(
                state = state,
                track = track,
                reveal = reveal,
                isFavorite = isFavorite,
                circular = circular,
                pageCorner = pageCorner,
                onToggleFavorite = { toggleFavorite() },
                onOpenQueue = onOpenQueue,
                onMore = onMore,
                onOpenSleepTimer = onOpenSleepTimer,
                lyrics = lyrics,
                lyricIndex = lyricIndex,
                showLyrics = showLyrics,
                onShowLyricsChange = { showLyrics = it },
                haptic = haptic,
                coverScale = coverScale,
                showLyricsTranslation = settings.showLyricsTranslation,
                lyricCentered = settings.lyricCentered,
            )
            }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = contentTopPadding,
                    bottom = navBarPadding + 12.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerTitle(
                track = track,
                revealHeader = reveal.header,
                centered = settings.titleCentered,
            )

            val lyricsReady = lyrics.isNotEmpty()
            val coverZoneTopBias = if (compact) 12.dp else 0.dp
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = coverZoneTopBias)
                    .then(
                        if (track.hasSong) {
                            Modifier.pointerInput(showLyrics) {
                                val threshold = size.width * 0.14f
                                detectHorizontalDragGestures(
                                    onDragStart = { swipeProgress = 0f },
                                    onDragEnd = { swipeProgress = 0f },
                                    onDragCancel = { swipeProgress = 0f },
                                ) { change, dragAmount ->
                                    change.consume()
                                    swipeProgress += -dragAmount
                                    if (!showLyrics && swipeProgress > threshold) {
                                        swipeProgress = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showLyrics = true
                                    } else if (showLyrics && swipeProgress < -threshold) {
                                        swipeProgress = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showLyrics = false
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = if (compact) Alignment.BottomCenter else Alignment.Center,
            ) {
            val coverSize = (minOf(maxWidth, maxHeight) * CoverSizeFraction)
                .coerceAtLeast(0.dp)

            val expandLyricsArea = !compact
            Crossfade(
                targetState = showLyrics,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                label = "coverOrLyrics",
            ) { isLyrics ->

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = if (compact) Alignment.BottomCenter else Alignment.Center,
                ) {
                    if (isLyrics && expandLyricsArea) {

                        Crossfade(
                            targetState = shownLyrics,
                            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                            label = "lyricsSwitch",
                        ) { lines ->
                            if (lines.isNotEmpty()) {
                                HyperLyricsView(
                                    lines = lines,
                                    currentIndex = currentLineIndex(lines, state.positionMs),
                                    showTranslation = settings.showLyricsTranslation,
                                    centered = settings.lyricCentered,
                                    onSeek = { state.seekTo(it) },
                                    modifier = Modifier.fillMaxSize(),
                                    onDismiss = { showLyrics = false },
                                )
                            } else {
                                NoLyricsHint()
                            }
                        }
                    } else {

                        Box(
                            modifier = Modifier
                                .size(coverSize)
                                .graphicsLayer { alpha = state.expandProgress.coerceIn(0f, 1f) }
                                .coverFrame(circular, pageCorner)
                                .then(
                                    if (track.hasSong) {
                                        Modifier
                                    } else {
                                        Modifier.background(
                                            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLyrics) {

                                if (lyricsReady) {
                                    HyperLyricsView(
                                        lines = lyrics,
                                        currentIndex = lyricIndex,
                                        showTranslation = settings.showLyricsTranslation,
                                        centered = settings.lyricCentered,
                                        onSeek = { state.seekTo(it) },
                                        modifier = Modifier.fillMaxSize(),
                                        onDismiss = { showLyrics = false },
                                    )
                                } else {
                                    NoLyricsHint()
                                }
                            } else {
                                when {
                                    track.hasSong -> Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .scale(coverScale)
                                            .saveCoverOnLongPress(track.coverUrl, track.title),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Crossfade(
                                            targetState = track.coverUrl ?: "",
                                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                                            label = "albumCrossfade",
                                        ) { url ->
                                            if (url.isBlank()) {
                                                Image(
                                                    imageVector = MiuixIcons.Music,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(88.dp),
                                                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f)),
                                                )
                                            } else {
                                                NetCover(
                                                    url = url,
                                                    modifier = Modifier.fillMaxSize(),
                                                    corner = pageCorner,
                                                    contentDescription = track.title,
                                                    maxSize = CoverSize.LARGE.decode,
                                                )
                                            }
                                        }
                                    }
                                    else -> Image(
                                        imageVector = MiuixIcons.Music,
                                        contentDescription = null,
                                        modifier = Modifier.size(88.dp),
                                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            Spacer(Modifier.height(if (compact) 12.dp else 0.dp))

            val legacyTimeToControls = if (compact) 20.dp else 30.dp
            val legacyControlsToFunctions = if (compact) 16.dp else 26.dp
            val progressDownShift =
                (legacyTimeToControls - PlayerSpacing.TimeToControls) +
                    (legacyControlsToFunctions - PlayerSpacing.ControlsToFunctions)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val e = reveal.controls.coerceIn(0f, 1f)
                        alpha = e
                        translationY = (1f - e) * 28.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            if (progressDownShift > 0.dp) {
                Spacer(Modifier.height(progressDownShift))
            }
            HyperProgressBar(
                progress = safeProgress(state.positionMs, track.durationMs),
                onSeek = { state.seekTo((it * track.durationMs).toLong()) },
                onSeekFinished = { state.seekTo((it * track.durationMs).toLong()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerSpacing.ProgressSideInset),
            )
            Spacer(Modifier.height(PlayerSpacing.ProgressToTime))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerSpacing.ProgressSideInset),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(state.positionMs),
                    color = PageTextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    text = formatDuration(track.durationMs),
                    color = PageTextSecondary,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(PlayerSpacing.TimeToControls))

            val ctrlIcon = 30.dp
            val ctrlBox = 56.dp * scale
            val ctrlGap = PlayerSpacing.MainControlGap * scale
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                PageIconButton(
                    description = "上一首",
                    iconSize = ctrlIcon,
                    tint = ControlTint,
                    boxSize = ctrlBox,
                    content = { MiuixSkipIcon(isNext = false, tint = ControlTint, size = ctrlIcon) },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state.previous()
                    },
                )
                Spacer(Modifier.size(ctrlGap))
                PlayPauseButton(
                    isPlaying = state.isPlaying,
                    buttonSize = 64.dp * scale,
                    iconSize = 44.dp,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        state.togglePlayPause()
                    },
                )
                Spacer(Modifier.size(ctrlGap))
                PageIconButton(
                    description = "下一首",
                    iconSize = ctrlIcon,
                    tint = ControlTint,
                    boxSize = ctrlBox,
                    content = { MiuixSkipIcon(isNext = true, tint = ControlTint, size = ctrlIcon) },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state.next()
                    },
                )
            }

            Spacer(Modifier.height(PlayerSpacing.ControlsToFunctions))

            val fnIcon = 23.dp
            val fnBox = 44.dp * scale
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerSpacing.FunctionRowInset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayModeButton(
                    mode = state.playMode,
                    boxSize = fnBox,
                    iconScale = 1f,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        state.cyclePlayMode()
                    },
                )
                PageIconButton(
                    imageVector = MiuixIcons.Timer,
                    description = "定时关闭",
                    iconSize = fnIcon,
                    tint = ControlTintDim,
                    boxSize = fnBox,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onOpenSleepTimer()
                    },
                )
                FavoriteButton(
                    isFavorite = isFavorite,
                    boxSize = fnBox,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        toggleFavorite()
                    },
                )
                PageIconButton(
                    imageVector = MiuixIcons.Playlist,
                    description = "播放列表",
                    iconSize = 24.dp,
                    tint = ControlTintDim,
                    boxSize = fnBox,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onOpenQueue()
                    },
                )
                PageIconButton(
                    imageVector = MiuixIcons.More,
                    description = "更多",
                    iconSize = 22.dp,
                    tint = ControlTintDim,
                    boxSize = fnBox,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onMore()
                    },
                )
            }
            }
        }
        }
    }
}

@Composable
private fun PlayerTitle(
    track: PlayerTrack,
    revealHeader: Float,
    modifier: Modifier = Modifier,
    centered: Boolean = true,
) {
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val e = revealHeader.coerceIn(0f, 1f)
                alpha = e
                translationY = (1f - e) * -14.dp.toPx() - 8.dp.toPx()
            },
    ) {
        val sideInset = if (centered) 0.dp else PlayerSpacing.ProgressSideInset
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sideInset),
            horizontalAlignment = align,
        ) {
            Text(
                text = track.title,
                color = PageTextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = track.artist,
                color = PageTextSecondary,
                fontSize = 14.sp,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlayerRightControls(
    state: HyperPlayerState,
    track: PlayerTrack,
    reveal: PlayerReveal,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    haptic: HapticFeedback,
    titleCentered: Boolean = true,
    modifier: Modifier = Modifier,
    cover: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
            PlayerTitle(
                track = track,
                revealHeader = reveal.header,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                centered = titleCentered,
            )
            cover?.invoke()

            HyperProgressBar(
                progress = safeProgress(state.positionMs, track.durationMs),
                onSeek = { state.seekTo((it * track.durationMs).toLong()) },
                onSeekFinished = { state.seekTo((it * track.durationMs).toLong()) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerSpacing.ProgressSideInset),
            )
            Spacer(Modifier.height(PlayerSpacing.ProgressToTime))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerSpacing.ProgressSideInset),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = formatDuration(state.positionMs), color = PageTextSecondary, fontSize = 12.sp)
                Text(text = formatDuration(track.durationMs), color = PageTextSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.height(PlayerSpacing.TimeToControls))

            val ctrlIcon = 30.dp
            val ctrlBox = 56.dp
            val ctrlGap = PlayerSpacing.MainControlGap
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                PageIconButton(
                    description = "上一首",
                    iconSize = ctrlIcon,
                    tint = ControlTint,
                    boxSize = ctrlBox,
                    content = { MiuixSkipIcon(isNext = false, tint = ControlTint, size = ctrlIcon) },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state.previous()
                    },
                )
                Spacer(Modifier.size(ctrlGap))
                PlayPauseButton(
                    isPlaying = state.isPlaying,
                    buttonSize = 64.dp,
                    iconSize = 44.dp,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        state.togglePlayPause()
                    },
                )
                Spacer(Modifier.size(ctrlGap))
                PageIconButton(
                    description = "下一首",
                    iconSize = ctrlIcon,
                    tint = ControlTint,
                    boxSize = ctrlBox,
                    content = { MiuixSkipIcon(isNext = true, tint = ControlTint, size = ctrlIcon) },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state.next()
                    },
                )
            }

            Spacer(Modifier.height(PlayerSpacing.ControlsToFunctions))

            val fnIcon = 23.dp
            val fnBox = 44.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerSpacing.FunctionRowInset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayModeButton(
                    mode = state.playMode,
                    boxSize = fnBox,
                    iconScale = 1f,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        state.cyclePlayMode()
                    },
                )
                PageIconButton(
                    imageVector = MiuixIcons.Timer,
                    description = "定时关闭",
                    iconSize = fnIcon,
                    tint = ControlTintDim,
                    boxSize = fnBox,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onOpenSleepTimer()
                    },
                )
                FavoriteButton(
                    isFavorite = isFavorite,
                    boxSize = fnBox,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onToggleFavorite()
                    },
                )
                PageIconButton(
                    imageVector = MiuixIcons.Playlist,
                    description = "播放列表",
                    iconSize = 24.dp,
                    tint = ControlTintDim,
                    boxSize = fnBox,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onOpenQueue()
                    },
                )
                PageIconButton(
                    imageVector = MiuixIcons.More,
                    description = "更多",
                    iconSize = 22.dp,
                    tint = ControlTintDim,
                    boxSize = fnBox,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onMore()
                    },
                )
            }
            }
}

@Composable
private fun TabletPlayerUi(
    state: HyperPlayerState,
    track: PlayerTrack,
    reveal: PlayerReveal,
    isFavorite: Boolean,
    circular: Boolean,
    pageCorner: Dp,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    lyrics: List<LyricLine>,
    lyricIndex: Int,
    haptic: HapticFeedback,
    coverScale: Float,
    showLyricsTranslation: Boolean,
    lyricCentered: Boolean,
    titleCentered: Boolean,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(26.dp)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 44.dp, end = 44.dp, top = statusTop, bottom = navBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 44.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (lyrics.isNotEmpty()) {
                HyperLyricsView(
                    lines = lyrics,
                    currentIndex = lyricIndex,
                    showTranslation = showLyricsTranslation,
                    centered = lyricCentered,
                    onSeek = { state.seekTo(it) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                NoLyricsHint()
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val coverSize = minOf(maxWidth * 0.6f, maxHeight * 0.32f)
            PlayerRightControls(
                state = state,
                track = track,
                reveal = reveal,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onOpenQueue = onOpenQueue,
                onMore = onMore,
                onOpenSleepTimer = onOpenSleepTimer,
                haptic = haptic,
                titleCentered = titleCentered,
                modifier = Modifier.fillMaxSize(),
                cover = {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .size(coverSize)
                            .graphicsLayer { alpha = state.expandProgress.coerceIn(0f, 1f) }
                            .coverFrame(circular, pageCorner)
                            .saveCoverOnLongPress(track.coverUrl, track.title)
                            .then(
                                if (track.hasSong) {
                                    Modifier
                                } else {
                                    Modifier.background(
                                        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val url = track.coverUrl ?: ""
                        when {
                            !track.hasSong -> Image(
                                imageVector = MiuixIcons.Music,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f)),
                            )

                            url.isBlank() -> Image(
                                imageVector = MiuixIcons.Music,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f)),
                            )

                            else -> NetCover(
                                url = url,
                                modifier = Modifier.fillMaxSize().scale(coverScale),
                                corner = pageCorner,
                                contentDescription = track.title,
                                maxSize = CoverSize.LARGE.decode,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun LandscapePlayerUi(
    state: HyperPlayerState,
    track: PlayerTrack,
    reveal: PlayerReveal,
    isFavorite: Boolean,
    circular: Boolean,
    pageCorner: Dp,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    lyrics: List<LyricLine>,
    lyricIndex: Int,
    showLyrics: Boolean,
    onShowLyricsChange: (Boolean) -> Unit,
    haptic: HapticFeedback,
    coverScale: Float,
    showLyricsTranslation: Boolean,
    lyricCentered: Boolean,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(26.dp)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 28.dp, end = 28.dp, top = statusTop, bottom = navBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var swipeProgress by remember { mutableFloatStateOf(0f) }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val coverSize = minOf(maxWidth, maxHeight) * 0.86f
            Box(
                modifier = Modifier
                    .size(coverSize)
                    .graphicsLayer {
                        alpha = state.expandProgress.coerceIn(0f, 1f)
                    }

                    .then(
                        if (track.hasSong) {
                            Modifier
                        } else {
                            Modifier
                                .coverFrame(circular, pageCorner)
                                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
                        }
                    )
                    .then(
                        if (track.hasSong) {
                            Modifier.pointerInput(showLyrics) {
                                val threshold = size.width * 0.14f
                                detectHorizontalDragGestures(
                                    onDragStart = { swipeProgress = 0f },
                                    onDragEnd = { swipeProgress = 0f },
                                    onDragCancel = { swipeProgress = 0f },
                                ) { change, dragAmount ->
                                    change.consume()
                                    swipeProgress += -dragAmount
                                    if (!showLyrics && swipeProgress > threshold) {
                                        swipeProgress = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onShowLyricsChange(true)
                                    } else if (showLyrics && swipeProgress < -threshold) {
                                        swipeProgress = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onShowLyricsChange(false)
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = showLyrics,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    label = "landCoverLyrics",
                ) { isLyrics ->
                    when {
                        isLyrics && lyrics.isNotEmpty() -> HyperLyricsView(
                            lines = lyrics,
                            currentIndex = lyricIndex,
                            showTranslation = showLyricsTranslation,
                            centered = lyricCentered,
                            onSeek = { state.seekTo(it) },
                            modifier = Modifier.fillMaxSize(),
                            onDismiss = { onShowLyricsChange(false) },
                        )

                        isLyrics -> Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Image(
                                imageVector = MiuixIcons.File,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                colorFilter = ColorFilter.tint(PageTextSecondary.copy(alpha = 0.7f)),
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "没有歌词",
                                color = PageTextSecondary,
                                fontSize = 15.sp,
                            )
                        }

                        track.hasSong -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(coverScale)
                                .coverFrame(circular, pageCorner),
                            contentAlignment = Alignment.Center,
                        ) {
                            val url = track.coverUrl ?: ""
                            if (url.isBlank()) {
                                Image(
                                    imageVector = MiuixIcons.Music,
                                    contentDescription = null,
                                    modifier = Modifier.size(88.dp),
                                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f)),
                                )
                            } else {
                                NetCover(
                                    url = url,
                                    modifier = Modifier.fillMaxSize(),
                                    corner = pageCorner,
                                    contentDescription = track.title,
                                    maxSize = CoverSize.LARGE.decode,
                                )
                            }
                        }

                        else -> Image(
                            imageVector = MiuixIcons.Music,
                            contentDescription = null,
                            modifier = Modifier.size(88.dp),
                            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f)),
                        )
                    }
                }
            }
        }

        PlayerRightControls(
            state = state,
            track = track,
            reveal = reveal,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onOpenQueue = onOpenQueue,
            onMore = onMore,
            onOpenSleepTimer = onOpenSleepTimer,
            haptic = haptic,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: androidx.compose.ui.unit.Dp = 64.dp,
    iconSize: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "playPause",
    )

    Box(
        modifier = modifier
            .size(buttonSize)
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
                .size(iconSize)
                .graphicsLayer {
                    alpha = progress
                    val s = 0.70f + 0.30f * progress
                    scaleX = s
                    scaleY = s
                    rotationZ = -55f * (1f - progress)
                },
            colorFilter = ColorFilter.tint(ControlTint),
        )
        Image(
            imageVector = MiuixIcons.Play,
            contentDescription = "播放",
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = 1f - progress
                    val s = 0.70f + 0.30f * (1f - progress)
                    scaleX = s
                    scaleY = s
                    rotationZ = 55f * progress
                },
            colorFilter = ColorFilter.tint(ControlTint),
        )
    }
}

@Composable
private fun MiuixSkipIcon(
    isNext: Boolean,
    tint: Color,
    size: Dp,
) {
    val barModifier = Modifier
        .width(2.5.dp)
        .height(size * 0.62f)
        .clip(RoundedCornerShape(1.25.dp))
        .background(tint)
    val triangleModifier = Modifier
        .size(size * 0.9f)
        .rotate(if (isNext) 0f else 180f)

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isNext) {
            Box(barModifier)
            Spacer(Modifier.width(size * 0.08f))
        }
        Image(
            imageVector = MiuixIcons.Play,
            contentDescription = null,
            modifier = triangleModifier,
            colorFilter = ColorFilter.tint(tint),
        )
        if (isNext) {
            Spacer(Modifier.width(size * 0.08f))
            Box(barModifier)
        }
    }
}

private val PlayModeVectorIconSize = 25.dp

@Composable
private fun PlayModeButton(
    mode: PlayMode,
    onClick: () -> Unit,
    boxSize: Dp = 44.dp,
    iconScale: Float = 1f,
) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = { playerControlIconTransition() },
            label = "playModeIcon",
        ) { target ->
            val description = when (target) {
                PlayMode.ORDER -> "列表循环"
                PlayMode.SHUFFLE -> "随机播放"
                PlayMode.REPEAT_ONE -> "单曲循环"
            }
            if (target == PlayMode.ORDER) {
                Image(
                    imageVector = RepeatAllIcon,
                    contentDescription = description,
                    modifier = Modifier.size(PlayModeVectorIconSize * iconScale),
                    colorFilter = ColorFilter.tint(ControlTintDim),
                )
            } else {
                Image(
                    imageVector = if (target == PlayMode.SHUFFLE) ShuffleIcon else RepeatOneIcon,
                    contentDescription = description,
                    modifier = Modifier.size(PlayModeVectorIconSize * iconScale),
                    colorFilter = ColorFilter.tint(ControlTintDim),
                )
            }
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    boxSize: Dp = 44.dp,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isFavorite,
            transitionSpec = { playerControlIconTransition() },
            label = "favoriteIcon",
        ) { liked ->
            Image(
                imageVector = if (liked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                contentDescription = if (liked) "取消收藏" else "添加到收藏",
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(if (liked) ControlTint else ControlTintDim),
            )
        }
    }
}

@Composable
private fun PageIconButton(
    description: String,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    content: (@Composable () -> Unit)? = null,
    boxSize: Dp = 44.dp,
    tint: Color = ControlTint,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(boxSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            content != null -> content()
            painter != null -> Image(
                painter = painter,
                contentDescription = description,
                modifier = Modifier.size(iconSize),
            )
            else -> Image(
                imageVector = requireNotNull(imageVector),
                contentDescription = description,
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(tint),
            )
        }
    }
}

@Composable
private fun HyperProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var pending by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(progress, pending) {
        val target = pending ?: return@LaunchedEffect
        if (kotlin.math.abs(target - progress) < 0.012f) pending = null
    }

    val expand by animateFloatAsState(
        targetValue = if (scrubbing) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 620f,
            visibilityThreshold = 0.001f,
        ),
        label = "seekExpand",
    )

    val shown = when {
        scrubbing -> scrubValue
        pending != null -> pending!!
        else -> if (progress.isNaN() || progress.isInfinite()) 0f else progress.coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val value = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(value)
                    onSeekFinished(value)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        scrubbing = true
                        scrubValue = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        pending = scrubValue
                        onSeek(scrubValue)
                        onSeekFinished(scrubValue)
                        scrubbing = false
                    },
                    onDragCancel = { scrubbing = false },
                ) { change, _ ->
                    change.consume()
                    scrubValue = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                }
            }
            .drawBehind {
                val trackHeight = (7.dp + 8.dp * expand).toPx()
                val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                val top = (size.height - trackHeight) / 2f

                drawRoundRect(
                    color = Color.White.copy(alpha = 0.30f),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius,
                )

                val playedWidth = size.width * shown
                if (playedWidth > 0f) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(0f, top),
                        size = Size(playedWidth.coerceAtLeast(trackHeight), trackHeight),
                        cornerRadius = radius,
                    )
                }
            },
    )
}

private fun safeProgress(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    val raw = positionMs / durationMs.toFloat()
    if (raw.isNaN() || raw.isInfinite()) return 0f
    return raw.coerceIn(0f, 1f)
}

private fun <S> androidx.compose.animation.AnimatedContentTransitionScope<S>.playerControlIconTransition() =
    (
        fadeIn(tween(180, delayMillis = 130, easing = LinearOutSlowInEasing)) +
            scaleIn(
                tween(180, delayMillis = 130, easing = LinearOutSlowInEasing),
                initialScale = 0.6f,
            )
        ).togetherWith(
        fadeOut(tween(120, delayMillis = 20, easing = LinearEasing)) +
            scaleOut(tween(140, easing = LinearOutSlowInEasing), targetScale = 0.5f),
    )

@Composable
private fun NoLyricsHint() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            imageVector = MiuixIcons.File,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            colorFilter = ColorFilter.tint(PageTextSecondary.copy(alpha = 0.7f)),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "没有歌词",
            color = PageTextSecondary,
            fontSize = 15.sp,
        )
    }
}
