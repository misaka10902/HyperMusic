package com.hyper.music.ui.player

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.LyricLine
import com.hyper.music.music.MusicLibrary
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first
import kotlin.math.abs

private val LyricsInnerPadding = 8.dp

@Composable
fun HyperLyricsView(
    lines: List<LyricLine>,
    currentIndex: Int,
    showTranslation: Boolean,
    centered: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val settings by MusicLibrary.settings.collectAsState()
    val baseFontSize = settings.lyricFontSize
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex.coerceIn(0, lines.lastIndex.coerceAtLeast(0)),
    )
    var positioned by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex, lines.size) {
        if (currentIndex < 0 || lines.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        if (listState.isScrollInProgress) {
            snapshotFlow { listState.isScrollInProgress }.first { !it }
        }

        suspend fun centerDelta(): Float? {
            val info = listState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val item = info.visibleItemsInfo.firstOrNull { it.index == currentIndex }
            if (item != null) {
                return item.offset + item.size / 2f - viewportCenter
            }
            listState.animateScrollToItem(currentIndex.coerceAtMost(lines.lastIndex))
            withFrameNanos { }
            val info2 = listState.layoutInfo
            val c2 = (info2.viewportStartOffset + info2.viewportEndOffset) / 2f
            val item2 = info2.visibleItemsInfo.firstOrNull { it.index == currentIndex } ?: return null
            return item2.offset + item2.size / 2f - c2
        }

        val delta = centerDelta() ?: return@LaunchedEffect
        if (!positioned) {
            if (delta != 0f) listState.dispatchRawDelta(delta)
            positioned = true
        } else if (delta != 0f) {

            listState.animateScrollBy(
                delta,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            )

            val settle = centerDelta()
            if (settle != null && abs(settle) > 0.5f) {
                listState.animateScrollBy(
                    settle,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val half = maxHeight / 2 - 46.dp

        if (lines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无歌词",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val edgeFade = Brush.verticalGradient(
                0f to Color.Transparent,
                0.14f to Color.Black,
                0.86f to Color.Black,
                1f to Color.Transparent,
            )
            val sideInset = if (centered) LyricsInnerPadding else maxWidth * (1f - CoverSizeFraction) / 2f
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = edgeFade, blendMode = BlendMode.DstIn)
                    },

                contentPadding = PaddingValues(
                    start = sideInset,
                    end = sideInset,
                    top = half.coerceAtLeast(0.dp),
                    bottom = half.coerceAtLeast(0.dp),
                ),
                horizontalAlignment = if (centered) {
                    Alignment.CenterHorizontally
                } else {
                    Alignment.Start
                },
            ) {
                items(lines.size, key = { i -> "${lines[i].timeMs}_$i" }) { i ->
                    LyricRow(
                        line = lines[i],
                        distance = (i - currentIndex).let { if (it < 0) -it else it },
                        isCurrent = i == currentIndex,
                        showTranslation = showTranslation,
                        centered = centered,
                        baseFontSize = baseFontSize,
                        onSeek = onSeek,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricRow(
    line: LyricLine,
    distance: Int,
    isCurrent: Boolean,
    showTranslation: Boolean,
    centered: Boolean,
    baseFontSize: Float,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val hasTranslation = showTranslation && line.translation.isNotBlank()

    val targetAlpha = if (isCurrent) 1f else when (distance) {
        1 -> 0.58f
        2 -> 0.44f
        else -> 0.30f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing),
        label = "lyricRowAlpha",
    )
    val focus by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing),
        label = "lyricRowFocus",
    )

    val align = if (centered) TextAlign.Center else TextAlign.Start

    val fontSize = (baseFontSize + 2f * focus).sp
    val translationSize = (baseFontSize * 0.81f + focus).sp
    val mainLineHeight = (baseFontSize * 1.43f).sp
    val translationLineHeight = (baseFontSize * 1.14f).sp
    val weightSettings by MusicLibrary.settings.collectAsState()
    val baseWeight = weightSettings.lyricFontWeight
    val fontWeight = FontWeight((baseWeight + if (focus > 0.5f) 100 else 0).coerceAtMost(900))
    val translationWeight = FontWeight((baseWeight - 100).coerceAtLeast(300))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (isCurrent) onDismiss() else onSeek(line.timeMs)
            }
            .padding(vertical = 12.dp),
        horizontalAlignment = if (centered) {
            Alignment.CenterHorizontally
        } else {
            Alignment.Start
        },
    ) {
        if (line.text.isNotBlank()) {
            Text(
                text = line.text,
                color = Color.White.copy(alpha = alpha),
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = align,
                lineHeight = mainLineHeight,
            )
        } else {
            Spacer(Modifier.height(10.dp))
        }
        if (hasTranslation && line.text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = line.translation,
                color = Color.White.copy(alpha = alpha * 0.78f),
                fontSize = translationSize,
                fontWeight = translationWeight,
                textAlign = align,
                lineHeight = translationLineHeight,
            )
        }
    }
}
