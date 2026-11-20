package com.hyper.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.NetCover
import com.hyper.music.music.Playlist
import com.hyper.music.music.Song
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.hyper.music.ui.components.ReorderGeom
import com.hyper.music.ui.components.SelectionAction
import com.hyper.music.ui.components.SelectionBarState
import com.hyper.music.ui.components.SongPairRow
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.components.TrackRow
import com.hyper.music.ui.components.dragReorderItem
import com.hyper.music.ui.components.dragReorderSurface
import com.hyper.music.ui.components.rememberDragReorderState
import com.hyper.music.ui.utils.isAppDarkTheme
import com.hyper.music.ui.utils.isLargeScreen
import top.yukonga.miuix.kmp.utils.overScrollVertical
import com.hyper.music.ui.utils.rememberWindowSizeDp
import com.hyper.music.ui.utils.songColumns
import com.hyper.music.ui.effects.miuix.rememberPopupBackdrop
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.glass.Item
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.WorldClock
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.foundation.Image

private val ContentBottomReserve = 145.dp

@Composable
fun HistoryScreen(
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    scrollBehavior: ScrollBehavior? = null,
    onSelectionChange: (SelectionBarState?) -> Unit = {},
    onBatchAddToPlaylist: (List<Song>) -> Unit = {},
    onBatchAddToQueue: (List<Song>) -> Unit = {},
    onBatchRemoveHistory: (List<Song>) -> Unit = {},
) {
    val history by MusicLibrary.history.collectAsState()
    val listState = rememberLazyListState()
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    val columns = songColumns()

    var displayItems by remember { mutableStateOf(history) }
    var showEmpty by remember { mutableStateOf(history.isEmpty()) }
    val listAlpha = remember { Animatable(1f) }
    val emptyAlpha by animateFloatAsState(
        targetValue = if (showEmpty) 1f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "historyEmptyAlpha",
    )
    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            displayItems = history
            showEmpty = false
            listAlpha.snapTo(1f)
        } else if (!showEmpty) {
            listAlpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            showEmpty = true
        }
    }

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val multiSelect = selectedIds.isNotEmpty()
    BackHandler(enabled = multiSelect) { selectedIds = emptySet() }

    var confirmDelete by remember { mutableStateOf<List<Song>?>(null) }

    LaunchedEffect(selectedIds, history) {
        val picked = history.filter { it.id in selectedIds }
        if (picked.isEmpty()) {
            onSelectionChange(null)
        } else {
            onSelectionChange(
                SelectionBarState(
                    count = picked.size,
                    actions = listOf(
                        SelectionAction(
                            label = "加入歌单",
                            icon = MiuixIcons.Playlist,
                        ) { onBatchAddToPlaylist(picked) },
                        SelectionAction(
                            label = "加入队列",
                            icon = MiuixIcons.ListView,
                        ) {
                            onBatchAddToQueue(picked)
                            selectedIds = emptySet()
                        },
                        SelectionAction(
                            label = "删除",
                            icon = MiuixIcons.Delete,
                            tint = scheme.error,
                        ) { confirmDelete = picked },
                    ),
                    onExit = { selectedIds = emptySet() },
                )
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionChange(null) }
    }

    if (showEmpty) {

        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { alpha = emptyAlpha },
            contentAlignment = Alignment.Center,
        ) {
            LibraryEmptyState(
                icon = MiuixIcons.WorldClock,
                text = "没有历史记录",
            )
        }
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = listAlpha.value }
                .overScrollVertical(onOffsetChanged = { offset -> scrollBehavior?.state?.overscrollOffset = offset })
                .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove)
                .then(
                    scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) }
                        ?: Modifier
                ),
            contentPadding = PaddingValues(top = topInset, bottom = ContentBottomReserve),
        ) {

            itemsIndexed(displayItems.chunked(columns), key = { _, group -> group.first().id }) { groupIndex, group ->
                SongPairRow(
                    pair = group,
                    startIndex = groupIndex * columns,
                    columns = columns,

                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(200),
                        placementSpec = spring<IntOffset>(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        fadeOutSpec = tween(260),
                    ),
                ) { index, song ->
                    TrackRow(
                        song = song,
                        onClick = {
                            if (multiSelect) {
                                selectedIds = if (song.id in selectedIds) {
                                    selectedIds - song.id
                                } else {
                                    selectedIds + song.id
                                }
                            } else {
                                onPlay(displayItems, index)
                            }
                        },
                        onMore = { onMore(song) },
                        selectionMode = multiSelect,
                        selected = song.id in selectedIds,
                        contentPadding = if (columns == 2) {
                            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        } else {
                            PaddingValues(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIds = selectedIds + song.id
                        },
                    )
                }
            }
        }

    }

    ConfirmDialog(
        show = confirmDelete != null,
        title = "删除历史",
        summary = "确认从历史记录中删除 ${confirmDelete?.size ?: 0} 首歌曲吗？",
        confirmText = "删除",
        onConfirm = {
            confirmDelete?.let { onBatchRemoveHistory(it) }
            selectedIds = emptySet()
            confirmDelete = null
        },
        onDismiss = { confirmDelete = null },
        backdrop = rememberPopupBackdrop(),
    )
}

@Composable
fun PlaylistScreen(
    onOpen: (Playlist) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    scrollBehavior: ScrollBehavior? = null,
    onRename: (Playlist) -> Unit = {},
    onSelectionChange: (SelectionBarState?) -> Unit = {},
    onBatchDelete: (List<Playlist>) -> Unit = {},
) {
    val playlists by MusicLibrary.playlists.collectAsState()
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }


    var deleteAnchor by remember { mutableIntStateOf(-1) }
    val motionScope = rememberCoroutineScope()

    var editing by remember { mutableStateOf(false) }
    val multiSelect = editing
    fun exitEdit() {
        editing = false
        selectedIds = emptySet()


        onSelectionChange(null)
    }
    BackHandler(enabled = multiSelect) { exitEdit() }

    LaunchedEffect(selectedIds, playlists, editing) {


        if (!editing) return@LaunchedEffect
        val picked = playlists.filter { it.id in selectedIds }
        val allSelected = playlists.isNotEmpty() && picked.size == playlists.size
        onSelectionChange(
            SelectionBarState(
                count = picked.size,
                actions = listOf(

                    SelectionAction(
                        label = "重命名",
                        icon = MiuixIcons.Rename,
                        enabled = picked.size == 1,
                    ) {
                        picked.singleOrNull()?.let { onRename(it) }
                        exitEdit()
                    },
                    SelectionAction(
                        label = "删除",
                        icon = MiuixIcons.Delete,
                        tint = scheme.error,
                        enabled = picked.isNotEmpty(),
                    ) {
                        val removed = picked.map { it.id }.toSet()
                        val anchor = playlists.indexOfFirst { it.id in removed }
                        deleteAnchor = if (anchor >= 0) anchor else 0
                        onBatchDelete(picked)
                        exitEdit()

                        motionScope.launch {
                            delay(600)
                            deleteAnchor = -1
                        }
                    },
                ),
                selectAllAction = SelectionAction(
                    label = if (allSelected) "取消全选" else "全选",
                    icon = MiuixIcons.SelectAll,
                    tint = if (allSelected) scheme.primary else Color.Unspecified,
                ) {

                    selectedIds = if (allSelected) emptySet() else playlists.map { it.id }.toSet()
                },
                onExit = { exitEdit() },
            )
        )
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionChange(null) }
    }

    val gridState = rememberLazyGridState()

    val wideScreen = isLargeScreen(rememberWindowSizeDp())
    val dragState = rememberDragReorderState { from, to ->
        MusicLibrary.reorderPlaylists(from, to)
    }
    fun gridGeoms(): List<ReorderGeom> = gridState.layoutInfo.visibleItemsInfo.map { info ->
        ReorderGeom(
            info.index,
            info.offset.x.toFloat(),
            info.offset.y.toFloat(),
            info.size.width.toFloat(),
            info.size.height.toFloat(),
        )
    }


    val density = LocalDensity.current
    LaunchedEffect(dragState.isDragging) {
        if (!dragState.isDragging) return@LaunchedEffect
        val edge = with(density) { 96.dp.toPx() }
        val maxStep = with(density) { 16.dp.toPx() }
        val topBound = with(density) { topInset.toPx() }
        while (isActive) {
            withFrameNanos { }
            val info = gridState.layoutInfo
            val bottom = info.viewportEndOffset.toFloat()
            val p = dragState.pointer.y
            val step = when {
                p < topBound + edge -> -maxStep * ((topBound + edge - p) / edge).coerceIn(0.15f, 1.6f)
                p > bottom - edge -> maxStep * ((p - (bottom - edge)) / edge).coerceIn(0.15f, 1.6f)
                else -> 0f
            }
            if (step != 0f) {
                gridState.scrollBy(step)
                dragState.onViewportScrolled()
            }
        }
    }
    val insetModifier = modifier

    if (playlists.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LibraryEmptyState(
                icon = MiuixIcons.Glass.Item,
                text = "没有歌单",
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()


            .dragReorderSurface(dragState, multiSelect) { gridGeoms() },
    ) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(if (wideScreen) 4 else 2),
        modifier = insetModifier
            .fillMaxSize()
            .overScrollVertical(onOffsetChanged = { offset -> scrollBehavior?.state?.overscrollOffset = offset })
            .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove)
            .then(
                scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) }
                    ?: Modifier
            ),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topInset,
            bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                .asPaddingValues().calculateBottomPadding() + ContentBottomReserve,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(playlists, key = { _, p -> p.id }) { index, playlist ->
            PlaylistCard(
                playlist = playlist,
                selected = playlist.id in selectedIds,
                selectionMode = multiSelect,
                dragModifier = Modifier

                    .animateItem(
                        fadeInSpec = tween(200),


                        placementSpec = if (dragState.isDragging || dragState.settling || dragState.suppressPlacement) {
                            null
                        } else if (deleteAnchor >= 0) {
                            tween<IntOffset>(
                                durationMillis = 260,
                                delayMillis = (kotlin.math.abs(index - deleteAnchor) * 20).coerceAtMost(100),
                                easing = FastOutSlowInEasing,
                            )
                        } else {
                            spring<IntOffset>(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            )
                        },
                        fadeOutSpec = tween(260),
                    )
                    .dragReorderItem(
                        id = playlist.id,
                        index = index,
                        state = dragState,
                        enabled = multiSelect,
                        snapshot = { gridGeoms() },
                    ),
                onClick = {
                    if (multiSelect) {
                        selectedIds = if (playlist.id in selectedIds) {
                            selectedIds - playlist.id
                        } else {
                            selectedIds + playlist.id
                        }
                    } else {
                        onOpen(playlist)
                    }
                },
                onLongPress = if (multiSelect) null else {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedIds = setOf(playlist.id)
                        editing = true
                    }
                },
            )
        }
    }
    }
}

@Composable
internal fun LibraryEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            colorFilter = ColorFilter.tint(scheme.disabledOnSecondaryVariant),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = text,
            color = scheme.onSurfaceSecondary.copy(alpha = 0.7f),
            fontSize = 17.sp,
        )
    }
}

private val PlaylistCoverCorner = 16.dp

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    dragModifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val cover = remember(playlist.songs) {
        playlist.songs.firstOrNull()?.let {
            GdApi.coverUrl(it, CoverSize.MEDIUM.request).ifBlank { null }
        }
    }
    val cardShape = RoundedCornerShape(24.dp)

    Card(
        cornerRadius = 24.dp,
        modifier = dragModifier
            .fillMaxWidth()
            .cardGlass(cardShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            ),
        insideMargin = PaddingValues(12.dp),
        colors = cardGlassColors(),
    ) {
        Column(modifier = Modifier.alpha(if (selectionMode && !selected) 0.68f else 1f)) {
            Box {
                if (cover != null) {
                    NetCover(
                        url = cover,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        corner = PlaylistCoverCorner,
                        maxSize = CoverSize.MEDIUM.decode,
                    )
                } else {
                    PlaylistCoverPlaceholder(corner = PlaylistCoverCorner)
                }
                if (selectionMode) {
                    Checkbox(
                        state = if (selected) ToggleableState.On else ToggleableState.Off,
                        onClick = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = playlist.name,
                color = scheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${playlist.songs.size} 首",
                color = scheme.onSurfaceSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PlaylistCoverPlaceholder(corner: Dp) {

    val placeholderGray = if (!isAppDarkTheme()) Color(0xFFE3E3E6) else Color(0xFF33353A)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(corner))
            .background(placeholderGray),
    )
}
