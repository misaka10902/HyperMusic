package com.hyper.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.hyper.music.music.GdApi
import com.hyper.music.music.Song
import com.hyper.music.music.SongComment
import com.hyper.music.ui.utils.verticalEdgeFade
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val COMMENT_PAGE_SIZE = 20
private const val MAX_COMMENTS = 2000

@Composable
fun CommentsContent(song: Song, onBack: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    var comments by remember { mutableStateOf<List<SongComment>>(emptyList()) }
    var cursor by remember { mutableStateOf(0L) }
    var pageNo by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    suspend fun loadMore() {
        if (loading || loadingMore || endReached || comments.isEmpty()) return
        loadingMore = true
        try {
            val page = GdApi.comments(song, cursor, pageNo, COMMENT_PAGE_SIZE)
            comments = (comments + page.comments).distinctBy { commentKey(it) }
            if (page.cursor != 0L && page.cursor != cursor) cursor = page.cursor
            pageNo++
            endReached = page.comments.size < COMMENT_PAGE_SIZE || comments.size >= MAX_COMMENTS
        } finally {
            loadingMore = false
        }
    }

    LaunchedEffect(song.id) {
        loading = true
        comments = emptyList()
        endReached = false
        cursor = 0L
        pageNo = 1
        val start = System.currentTimeMillis()
        val page = GdApi.comments(song, start, 1, COMMENT_PAGE_SIZE)
        comments = page.comments
        val advanced = page.cursor != 0L && page.cursor != start
        cursor = if (advanced) page.cursor else start
        pageNo = 2
        endReached = page.comments.size < COMMENT_PAGE_SIZE
        loading = false
        listState.scrollToItem(0)
    }

    val nearBottom by remember { derivedStateOf { listState.isNearBottom() } }
    LaunchedEffect(nearBottom) {
        if (!nearBottom) return@LaunchedEffect
        loadMore()
    }

    val state = when {
        loading -> 0
        comments.isEmpty() -> 1
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
        }
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
                            modifier = Modifier.size(56.dp),
                            colorFilter = ColorFilter.tint(
                                scheme.disabledOnSecondaryVariant,
                            ),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(text = "没有评论", color = scheme.onSurfaceSecondary, fontSize = 17.sp)
                    }
                }


                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.surfaceContainer, RoundedCornerShape(20.dp)),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp)
                            .verticalEdgeFade(22.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                    ) {
                        items(comments, key = { commentKey(it) }) { comment ->
                            CommentRow(comment)
                            Spacer(Modifier.height(18.dp))
                        }
                        if (!endReached || comments.size >= COMMENT_PAGE_SIZE) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(top = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (endReached) {
                                        Text(
                                            text = "没有更多评论 · 共 ${comments.size} 条",
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

@Composable
private fun CommentRow(comment: SongComment) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 6.dp)) {
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
