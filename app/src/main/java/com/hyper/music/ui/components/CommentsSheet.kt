package com.hyper.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.hyper.music.music.GdApi
import com.hyper.music.music.Song
import com.hyper.music.music.SongComment
import com.hyper.music.ui.utils.verticalEdgeFade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max
import kotlin.math.roundToInt

private const val COMMENT_PAGE_SIZE = 20
private const val MAX_COMMENTS = 2000

@Composable
fun CommentsContent(song: Song, onBack: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<SongComment>>(emptyList()) }
    var cursor by remember { mutableStateOf(0L) }
    var pageNo by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    var emptyStreak by remember { mutableIntStateOf(0) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var sortByHot by remember { mutableStateOf(true) }
    var showSortPopup by remember { mutableStateOf(false) }
    var sortPopupVisible by remember { mutableStateOf(false) }
    var sortAnchor by remember { mutableStateOf(IntRect.Zero) }
    val listState = rememberLazyListState()

    suspend fun loadMore() {
        if (loading || loadingMore || endReached || comments.isEmpty()) return
        loadingMore = true
        try {
            val page = GdApi.comments(song, cursor, pageNo, COMMENT_PAGE_SIZE)
            val merged = (comments + page.comments).distinctBy { commentKey(it) }
            val grew = merged.size > comments.size
            comments = merged
            emptyStreak = if (grew) 0 else emptyStreak + 1
            if (page.cursor != 0L && page.cursor != cursor) cursor = page.cursor
            pageNo++
            endReached = emptyStreak >= 3 || comments.size >= MAX_COMMENTS
        } finally {
            loadingMore = false
        }
    }

    LaunchedEffect(song.id, refreshTick) {
        loading = true
        comments = emptyList()
        endReached = false
        emptyStreak = 0
        cursor = 0L
        pageNo = 1
        val start = System.currentTimeMillis()
        val page = GdApi.comments(song, start, 1, COMMENT_PAGE_SIZE)
        comments = page.comments
        val advanced = page.cursor != 0L && page.cursor != start
        cursor = if (advanced) page.cursor else start
        pageNo = 2
        endReached = page.comments.isEmpty()
        loading = false
        listState.scrollToItem(0)
    }

    val nearBottom by remember { derivedStateOf { listState.isNearBottom() } }
    LaunchedEffect(nearBottom) {
        if (!nearBottom) return@LaunchedEffect
        loadMore()
    }

    LaunchedEffect(showSortPopup) {
        if (showSortPopup) sortPopupVisible = true
    }

    fun closeSortPopup() {
        sortPopupVisible = false
        scope.launch {
            delay(170)
            showSortPopup = false
        }
    }

    val ordered = remember(comments, sortByHot) {
        if (sortByHot) {
            val hot = comments.filter { it.isHot }.sortedByDescending { it.likedCount }
            val rest = comments.filter { !it.isHot }
            hot + rest
        } else {
            comments
        }
    }

    val state = when {
        loading -> 0
        ordered.isEmpty() -> 1
        else -> 2
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Text(
                text = "评论",
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                color = scheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = MiuixIcons.ChevronBackward,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(scheme.onSurfaceSecondary),
                )
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { refreshTick++ },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        colorFilter = ColorFilter.tint(scheme.onSurfaceSecondary),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .onGloballyPositioned { coordinates ->
                            val rect = coordinates.boundsInWindow()
                            sortAnchor = IntRect(
                                rect.left.roundToInt(),
                                rect.top.roundToInt(),
                                rect.right.roundToInt(),
                                rect.bottom.roundToInt(),
                            )
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showSortPopup = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        imageVector = MiuixIcons.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(scheme.onSurfaceSecondary),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalEdgeFade(22.dp),
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(animationSpec = tween(220)),
                        initialContentExit = fadeOut(animationSpec = tween(140)),
                        sizeTransform = SizeTransform(
                            clip = true,
                            sizeAnimationSpec = { _, _ -> tween(280, easing = FastOutSlowInEasing) },
                        ),
                    )
                },
                label = "commentsState",
            ) { uiState ->
                when (uiState) {
                    0 -> Box(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        RefreshIndicator(text = "正在加载评论")
                    }
                    1 -> Box(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                imageVector = MiuixIcons.Messages,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                colorFilter = ColorFilter.tint(
                                    scheme.onSurfaceSecondary.copy(alpha = 0.6f),
                                ),
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(text = "没有评论", color = scheme.onSurfaceSecondary, fontSize = 15.sp)
                        }
                    }
                    else -> SheetCard {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 460.dp)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        ) {
                            items(ordered, key = { commentKey(it) }) { comment ->
                                CommentRow(comment)
                                Spacer(Modifier.height(18.dp))
                            }
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(top = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (endReached) {
                                        Text(
                                            text = "没有更多评论 · 共 ${ordered.size} 条",
                                            color = scheme.onSurfaceSecondary,
                                            fontSize = 12.sp,
                                        )
                                    } else {
                                        InfiniteProgressIndicator(
                                            color = scheme.onSurfaceSecondary,
                                            size = 20.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showSortPopup && sortAnchor != IntRect.Zero) {
            Popup(
                popupPositionProvider = SortPositionProvider(sortAnchor),
                onDismissRequest = { closeSortPopup() },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visible = sortPopupVisible,
                    enter = fadeIn(animationSpec = tween(150)) +
                        slideInVertically(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            initialOffsetY = { -it / 3 },
                        ) +
                        scaleIn(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            initialScale = 0.92f,
                            transformOrigin = TransformOrigin(1f, 0f),
                        ),
                    exit = fadeOut(animationSpec = tween(130)) +
                        slideOutVertically(
                            animationSpec = tween(150),
                            targetOffsetY = { -it / 3 },
                        ) +
                        scaleOut(
                            animationSpec = tween(150),
                            targetScale = 0.96f,
                            transformOrigin = TransformOrigin(1f, 0f),
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .width(190.dp)
                            .background(scheme.surfaceContainer, RoundedCornerShape(18.dp))
                            .padding(vertical = 6.dp),
                    ) {
                        GlassPopupItem(
                            text = "按热度",
                            selected = sortByHot,
                            onClick = { sortByHot = true; closeSortPopup() },
                        )
                        GlassPopupItem(
                            text = "按时间",
                            selected = !sortByHot,
                            onClick = { sortByHot = false; closeSortPopup() },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListState.isNearBottom(threshold: Int = 4): Boolean {
    if (!isScrollInProgress && firstVisibleItemIndex == 0 && layoutInfo.totalItemsCount <= threshold) {
        return true
    }
    val layout = layoutInfo
    val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
    return lastVisible >= layout.totalItemsCount - threshold
}

private fun commentKey(c: SongComment): String =
    if (c.commentId != 0L) "id:${c.commentId}" else "${c.userName}|${c.content}|${c.timeMs}"

private class SortPositionProvider(
    private val anchor: IntRect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = 8
        val x = (anchor.right - popupContentSize.width).coerceIn(
            gap,
            max(gap, windowSize.width - popupContentSize.width - gap),
        )
        var y = anchor.bottom + gap
        if (y + popupContentSize.height > windowSize.height) {
            y = anchor.top - popupContentSize.height - gap
        }
        return IntOffset(
            x,
            y.coerceIn(gap, max(gap, windowSize.height - popupContentSize.height - gap)),
        )
    }
}

@Composable
private fun CommentRow(comment: SongComment) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = comment.userName,
                color = scheme.onSurfaceSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (comment.location.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = comment.location,
                    color = scheme.onSurfaceSecondary.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            if (comment.dateText.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = comment.dateText,
                    color = scheme.onSurfaceSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = comment.content,
            color = scheme.onSurface,
            fontSize = 16.sp,
            lineHeight = 25.sp,
        )
    }
}
