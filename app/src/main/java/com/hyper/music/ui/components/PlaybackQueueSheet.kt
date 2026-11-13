package com.hyper.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.NetCover
import com.hyper.music.music.PlaybackController
import com.hyper.music.music.Song
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Redo
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent

private val CurrentRowCorner = 18.dp

@Composable
fun PlaybackQueueSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val state by PlaybackController.state.collectAsState()
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible && state.index >= 0) {
            listState.scrollToItem(state.index.coerceAtLeast(0))
        }
    }

    val frameColor = if (scheme.background.luminance() < 0.5f) {
        Color.White
    } else {
        scheme.onSurface
    }

    HyperSheet(visible = visible, onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = 4.dp),
            ) {
                RoundIconButton(
                    icon = MiuixIcons.Close,
                    description = "关闭",
                    iconSize = 23.dp,
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = onDismiss,
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "播放队列",
                        color = scheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (state.queue.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${state.index + 1} / ${state.queue.size}",
                            color = scheme.onSurfaceSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
                }
                RoundIconButton(
                    icon = MiuixIcons.Delete,
                    description = "清空队列",
                    iconSize = 24.dp,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    tint = if (state.queue.isEmpty()) {
                        scheme.onSurfaceSecondary.copy(alpha = 0.35f)
                    } else {
                        scheme.onSurface
                    },
                    onClick = {
                        if (state.queue.isEmpty()) return@RoundIconButton
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        confirmClear = true
                    },
                )
            }

            if (state.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "队列还是空的",
                        color = scheme.onSurfaceSecondary,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(queueSheetListHeight(state.queue.size))
                        .fadeListEdges(listState),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = QueueListVerticalPadding,
                    ),
                ) {
                    itemsIndexed(state.queue, key = { index, song -> "${song.id}#$index" }) { index, song ->
                        QueueRow(
                            song = song,
                            isCurrent = index == state.index,
                            frameColor = frameColor,
                            canMoveUp = index > state.index,
                            canMoveDown = index < state.index,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                PlaybackController.playAt(index)
                            },
                            onMoveUp = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                PlaybackController.moveInQueue(index, index - 1)
                            },
                            onMoveDown = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                PlaybackController.moveInQueue(index, index + 1)
                            },
                            onRemove = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                PlaybackController.removeFromQueue(song.id)
                            },
                        )
                    }
                }
            }
        }
    }

    ConfirmDialog(
        show = confirmClear,
        title = "清空播放列表",
        summary = "确定要清空播放列表吗？此操作会终止当前播放的歌曲",
        confirmText = "清空",
        onConfirm = {
            confirmClear = false
            PlaybackController.clearQueue()
            onDismiss()
        },
        onDismiss = { confirmClear = false },
    )
}

private fun queueSheetListHeight(count: Int) =
    QueueRowHeight * count.coerceAtMost(6) + QueueListVerticalPadding * 2

private fun Modifier.fadeListEdges(
    listState: LazyListState,
    fade: Dp = 16.dp,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = size.height
        val fadePx = fade.toPx()
        val topFrac = (fadePx / h).coerceIn(0f, 0.45f)
        val bottomFrac = (fadePx / h).coerceIn(0f, 0.45f)
        val canScrollBack = listState.canScrollBackward
        val canScrollFwd = listState.canScrollForward
        val stops = ArrayList<Pair<Float, Color>>(4)
        stops.add(0f to (if (canScrollBack) Color.Transparent else Color.Black))
        if (canScrollBack) stops.add(topFrac to Color.Black)
        if (canScrollFwd) stops.add((1f - bottomFrac) to Color.Black)
        stops.add(1f to (if (canScrollFwd) Color.Transparent else Color.Black))
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = stops.toTypedArray(),
                startY = 0f,
                endY = h,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

private val QueueRowHeight = 62.dp

private val QueueListVerticalPadding = 4.dp

@Composable
private fun QueueRow(
    song: Song,
    isCurrent: Boolean,
    frameColor: Color,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(CurrentRowCorner))
            .then(
                if (isCurrent) {
                    Modifier
                        .background(scheme.onSurface.copy(alpha = 0.06f))
                        .border(
                            width = 1.4.dp,
                            color = frameColor.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(CurrentRowCorner),
                        )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetCover(
            url = GdApi.coverUrl(song, CoverSize.SMALL.request).ifBlank { null },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            corner = 22.dp,
            maxSize = CoverSize.SMALL.decode,
            contentDescription = song.name,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = scheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = song.artistText,
                color = scheme.onSurfaceSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (canMoveUp || canMoveDown) {
            RoundIconButton(
                icon = if (canMoveUp) MiuixIcons.Undo else MiuixIcons.Redo,
                description = if (canMoveUp) "上移播放位置" else "下移播放位置",
                iconSize = 19.dp,
                tint = scheme.onSurface.copy(alpha = 0.85f),
                onClick = if (canMoveUp) onMoveUp else onMoveDown,
            )
        } else {
            Spacer(Modifier.width(40.dp))
        }
        Spacer(Modifier.width(4.dp))
        RoundIconButton(
            icon = MiuixIcons.Hide,
            description = "从播放列表移除",
            iconSize = 19.dp,
            tint = scheme.onSurface.copy(alpha = 0.85f),
            onClick = onRemove,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}
