package com.hyper.music.ui.screens
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.takeOrElse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.R
import top.yukonga.miuix.kmp.icon.extended.RecordingTape
import androidx.compose.ui.res.painterResource
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.NetCover
import com.hyper.music.music.Song
import com.hyper.music.music.WallpaperManager
import com.hyper.music.music.WallpaperBackground
import com.hyper.music.ui.basic.RightAlignedTooltipPositionProvider
import com.hyper.music.ui.basic.chipBounce
import com.hyper.music.ui.basic.rememberChipBounce
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.effects.miuix.LocalActiveBackdrop
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.effects.miuix.cardGlassAlpha
import com.hyper.music.ui.components.EmptyHint
import com.hyper.music.ui.components.RefreshIndicator
import com.hyper.music.ui.components.ReorderGeom
import com.hyper.music.ui.components.ReorderGeometryRegistry
import com.hyper.music.ui.components.SelectionAction
import com.hyper.music.ui.components.SelectionBarState
import com.hyper.music.ui.components.SongPairRow
import com.hyper.music.ui.components.TrackRow
import com.hyper.music.ui.components.dragReorderItem
import com.hyper.music.ui.components.rememberDragReorderState
import com.hyper.music.ui.components.reorderGeometry
import com.hyper.music.ui.components.formatTime
import top.yukonga.miuix.kmp.utils.overScrollVertical
import com.hyper.music.ui.utils.songColumns
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.RichTooltipBox
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateTo
import androidx.compose.ui.graphics.graphicsLayer

private val DetailBottomReserve = 76.dp

private const val EXPORT_TIP =
    "点击即可将该歌单以JSON格式导出，导出的歌单和 Otter Music 等支持从JSON文件导入歌单的应用互通"

@Composable
internal fun DetailPage(
    title: String,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    header: @Composable () -> Unit,
    background: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    compactTitle: Boolean = false,
    subtitle: String? = null,
    listState: LazyListState = rememberLazyListState(),
    endAction: (@Composable () -> Unit)? = null,
    endActionKey: Any? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    list: LazyListScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val scrollBehavior = MiuixScrollBehavior()
    val density = LocalDensity.current

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeightDp = with(density) { headerHeightPx.toDp() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (WallpaperManager.state.collectAsState().value.hasImage) {
            Color.Transparent
        } else {
            MiuixTheme.colorScheme.surface
        },
        topBar = {
            GlassTopAppBar(
                title = title,
                largeTitle = title,
                subtitle = subtitle.orEmpty(),
                showLargeTitle = !compactTitle,
                backdrop = backdrop,
                progressiveBlur = advancedMaterial,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    GlassIconButton(
                        onClick = onBack,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Glass.ChevronBackward,
                            contentDescription = "返回",
                            modifier = Modifier.size(25.dp).offset(x = 1.5.dp, y = 0.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    
                    AnimatedContent(
                        targetState = endActionKey ?: title,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(190)) togetherWith fadeOut(animationSpec = tween(140))
                        },
                        contentAlignment = Alignment.CenterEnd,
                        label = "detailEndAction",
                    ) { endAction?.invoke() }
                },
            )
        },
    ) { contentPadding ->
        val topBarHeightDp = contentPadding.calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            WallpaperBackground()
            Box(Modifier.matchParentSize()) { background() }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove),
                contentPadding = PaddingValues(
                    top = topBarHeightDp,
                    bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                        .asPaddingValues().calculateBottomPadding() + DetailBottomReserve,
                ),
            ) {
                item {
                    Box(Modifier.fillMaxWidth().onSizeChanged { headerHeightPx = it.height }) {
                        header()
                    }
                }
                list()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topBarHeightDp + headerHeightDp, bottom = 96.dp),
            ) {
                overlay()
            }
        }
    }
}

@Composable
private fun LoadMoreButton(
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val height = 40.dp
    val rotation by animateFloatAsState(
        targetValue = if (loading) 360f else 0f,
        animationSpec = if (loading) {
            infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing))
        } else {
            tween(durationMillis = 200)
        },
        label = "loadMoreSpin",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .squircleClip(height / 2)
                .background(scheme.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !loading,
                ) { if (!loading) onClick() }
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(height)
                    .wrapContentWidth(),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    imageVector = MiuixIcons.Refresh,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation),
                    colorFilter = ColorFilter.tint(scheme.onPrimary),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (loading) "加载中…" else "加载更多",
                color = scheme.onPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PlayAllButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MiuixTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val bounce = rememberChipBounce(interactionSource)
    Row(
        modifier = modifier

            .chipBounce(bounce)
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.primary)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Image(
            imageVector = MiuixIcons.Play,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            colorFilter = ColorFilter.tint(scheme.onPrimary),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = "播放全部", color = scheme.onPrimary, fontSize = 13.sp)
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song, Boolean) -> Unit,
    onSelectionChange: (SelectionBarState?) -> Unit = {},
    onExport: (() -> Unit)? = null,
) {
    val columns = songColumns()
    val playlists by MusicLibrary.playlists.collectAsState()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    val songs = playlist?.songs.orEmpty()
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val cover = remember(songs) {
        songs.firstOrNull()?.picId?.let { GdApi.picUrl(it, CoverSize.MEDIUM.request).ifBlank { null } }
    }

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var editing by remember { mutableStateOf(false) }
    val multiSelect = editing
    fun exitEdit() {
        editing = false
        selectedIds = emptySet()
    }
    BackHandler(enabled = multiSelect) { exitEdit() }
    var confirmDelete by remember { mutableStateOf(false) }
    val allSelected = songs.isNotEmpty() && songs.all { it.id in selectedIds }
    fun toggleSelectAll() {
        selectedIds = if (allSelected) emptySet() else songs.map { it.id }.toSet()
    }
    LaunchedEffect(selectedIds, songs, editing) {
        if (!editing) {
            onSelectionChange(null)
            return@LaunchedEffect
        }
        val picked = songs.filter { it.id in selectedIds }
        onSelectionChange(
            SelectionBarState(
                count = picked.size,
                actions = listOf(
                    SelectionAction(
                        label = "删除",
                        icon = MiuixIcons.Delete,
                        tint = scheme.error,
                        enabled = picked.isNotEmpty(),
                    ) { confirmDelete = true },
                ),
                selectAllAction = SelectionAction(
                    label = if (allSelected) "取消全选" else "全选",
                    icon = MiuixIcons.SelectAll,
                    tint = if (allSelected) scheme.primary else Color.Unspecified,
                ) { toggleSelectAll() },
                onExit = { exitEdit() },
            )
        )
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionChange(null) }
    }

    val listState = rememberLazyListState()
    val dragState = rememberDragReorderState { from, to ->
        MusicLibrary.reorderPlaylistSongs(playlistId, from, to)
    }
    val geomRegistry = remember { ReorderGeometryRegistry() }
    fun songGeoms(): List<ReorderGeom> = geomRegistry.snapshot(songs.size)

    val exportAction: (@Composable () -> Unit)? = if (onExport == null) {
        null
    } else {
        {

            RichTooltipBox(
                text = EXPORT_TIP,
                title = "提示",
                actionText = "我知道了",
                positioning = TooltipAnchorPosition.Below,
                positionProvider = remember {
                    RightAlignedTooltipPositionProvider(spacing = 8)
                },
            ) {
                GlassIconButton(
                    onClick = onExport,
                ) {
                    Icon(
                        imageVector = MiuixIcons.UploadCloud,
                        contentDescription = "导出歌单",
                        modifier = Modifier.size(25.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    val topEndAction: (@Composable () -> Unit)? = if (multiSelect) {
        {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(
                    onClick = { toggleSelectAll() },
                ) {
                    Icon(
                        imageVector = MiuixIcons.SelectAll,
                        contentDescription = if (allSelected) "取消全选" else "全选",
                        modifier = Modifier.size(22.dp),
                        tint = (if (allSelected) scheme.primary else Color.Unspecified).takeOrElse { MiuixTheme.colorScheme.onSurface },
                    )
                }
                Spacer(Modifier.width(10.dp))
                GlassIconButton(
                    onClick = { exitEdit() },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Close,
                        contentDescription = "退出多选",
                        modifier = Modifier.size(22.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    } else exportAction

    DetailPage(
        title = if (multiSelect) "已选择 ${selectedIds.size} 项" else playlist?.name ?: "歌单",
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        listState = listState,
        endAction = topEndAction,
        endActionKey = multiSelect,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (cover == null) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(scheme.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_playlist_placeholder),
                            contentDescription = null,
                            modifier = Modifier.size(46.dp),
                            colorFilter = ColorFilter.tint(
                                scheme.onSurfaceSecondary.copy(alpha = 0.35f)
                            ),
                        )
                    }
                } else {
                    NetCover(
                        url = cover,
                        modifier = Modifier.size(96.dp),
                        corner = 22.dp,
                        maxSize = CoverSize.MEDIUM.decode,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = playlist?.name ?: "歌单",
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${songs.size} 首歌曲",
                    color = scheme.onSurfaceSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PlayAllButton(onClick = { if (songs.isNotEmpty()) onPlay(songs, 0) })
                Spacer(Modifier.height(8.dp))
            }
        },
    ) {
        if (songs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.foundation.Image(
                        imageVector = MiuixIcons.RecordingTape,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        colorFilter = ColorFilter.tint(
                            scheme.onSurfaceSecondary.copy(alpha = 0.55f)
                        ),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "歌单还没有歌曲",
                        color = scheme.onSurfaceSecondary.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            itemsIndexed(songs.chunked(columns), key = { _, group -> group.first().id }) { groupIndex, group ->
                SongPairRow(
                    pair = group,
                    startIndex = groupIndex * columns,
                    columns = columns,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(200),
                        placementSpec = null,
                        fadeOutSpec = tween(260),
                    ),
                ) { index, song ->
                    TrackRow(
                        song = song,
                        modifier = Modifier
                            .dragReorderItem(
                                id = song.id,
                                index = index,
                                state = dragState,
                                enabled = multiSelect,
                                snapshot = { songGeoms() },
                            )
                            .reorderGeometry(geomRegistry, index),
                        onClick = {
                            if (multiSelect) {
                                selectedIds = if (song.id in selectedIds) {
                                    selectedIds - song.id
                                } else {
                                    selectedIds + song.id
                                }
                            } else {
                                onPlay(songs, index)
                            }
                        },
                        onMore = { onMore(song, true) },
                        selectionMode = multiSelect,
                        selected = song.id in selectedIds,
                        contentPadding = if (columns == 2) {
                            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        } else {
                            PaddingValues(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        },
                        onLongPress = if (multiSelect) null else {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editing = true
                                selectedIds = selectedIds + song.id
                            }
                        },
                    )
                }
            }
        }
    }

    ConfirmDialog(
        show = confirmDelete,
        title = "删除歌曲",
        summary = "是否删除这些歌曲？",
        onConfirm = {
            confirmDelete = false
            val targets = songs.filter { it.id in selectedIds }
            targets.forEach { MusicLibrary.removeSongFromPlaylist(playlistId, it.id) }
            exitEdit()
        },
        onDismiss = { confirmDelete = false },
        backdrop = LocalActiveBackdrop.current,
    )
}

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onSelectionChange: (SelectionBarState?) -> Unit = {},
) {
    val songs by MusicLibrary.favorites.collectAsState()
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val multiSelect = selectedIds.isNotEmpty()
    BackHandler(enabled = multiSelect) { selectedIds = emptySet() }

    var confirmUnfavorite by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIds, songs) {
        val picked = songs.filter { it.id in selectedIds }
        if (picked.isEmpty()) {
            onSelectionChange(null)
        } else {
            onSelectionChange(
                SelectionBarState(
                    count = picked.size,
                    actions = listOf(
                        SelectionAction(
                            label = "删除",
                            icon = MiuixIcons.Delete,
                            tint = scheme.error,
                        ) { confirmUnfavorite = true },
                    ),
                    onExit = { selectedIds = emptySet() },
                )
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionChange(null) }
    }

    val listState = rememberLazyListState()
    val dragState = rememberDragReorderState { from, to ->
        MusicLibrary.reorderFavorites(from, to)
    }
    fun songGeoms(): List<ReorderGeom> = listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
        val dataIndex = info.index - 1
        if (dataIndex < 0) null else ReorderGeom(
            dataIndex,
            0f,
            info.offset.toFloat(),
            0f,
            info.size.toFloat(),
        )
    }

    DetailPage(
        title = "我喜欢的歌曲",
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        listState = listState,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        imageVector = MiuixIcons.Favorites,
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        colorFilter = ColorFilter.tint(
                            scheme.onSurfaceSecondary.copy(alpha = 0.55f)
                        ),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "我喜欢的歌曲",
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${songs.size} 首歌曲",
                    color = scheme.onSurfaceSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PlayAllButton(onClick = { if (songs.isNotEmpty()) onPlay(songs, 0) })
                Spacer(Modifier.height(8.dp))
            }
        },
    ) {
        if (songs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.foundation.Image(
                        imageVector = MiuixIcons.FavoritesFill,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        colorFilter = ColorFilter.tint(
                            scheme.onSurfaceSecondary.copy(alpha = 0.55f)
                        ),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "暂无收藏的歌曲",
                        color = scheme.onSurfaceSecondary.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                TrackRow(
                    song = song,
                    modifier = Modifier

                        .animateItem(
                            fadeInSpec = tween(200),
                            placementSpec = null,
                            fadeOutSpec = tween(260),
                        )
                        .dragReorderItem(
                            id = song.id,
                            index = index,
                            state = dragState,
                            enabled = multiSelect,
                            snapshot = { songGeoms() },
                        ),
                    onClick = {
                        if (multiSelect) {
                            selectedIds = if (song.id in selectedIds) {
                                selectedIds - song.id
                            } else {
                                selectedIds + song.id
                            }
                        } else {
                            onPlay(songs, index)
                        }
                    },
                    onMore = { onMore(song) },
                    selectionMode = multiSelect,
                    selected = song.id in selectedIds,
                    onLongPress = if (multiSelect) null else {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedIds = selectedIds + song.id
                        }
                    },
                )
            }
        }
    }

    ConfirmDialog(
        show = confirmUnfavorite,
        title = "取消收藏",
        summary = "是否取消收藏这些歌曲？",
        confirmText = "取消收藏",
        onConfirm = {
            confirmUnfavorite = false
            val targets = songs.filter { it.id in selectedIds }
            targets.forEach { MusicLibrary.removeFavorite(it.id) }
            selectedIds = emptySet()
        },
        onDismiss = { confirmUnfavorite = false },
        backdrop = LocalActiveBackdrop.current,
    )
}

@Composable
fun ArtistScreen(
    artistName: String,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val columns = songColumns()
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    val slidePx = with(LocalDensity.current) { 12.dp.toPx() }

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(loading, tab) {
        if (!loading) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
    }
    fun itemAlpha(index: Int): Float =
        ((reveal.value - (index * 0.015f).coerceAtMost(0.12f)) / 0.7f).coerceIn(0f, 1f)
    var artistPic by remember { mutableStateOf("") }
    val scheme = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()

    var page by remember { mutableIntStateOf(1) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }

    LaunchedEffect(artistName) {
        loading = true
        page = 1
        hasMore = true
        launch {
            artistPic = runCatching { GdApi.artistAvatar(artistName) }.getOrDefault("")
        }
        songs = GdApi.artistSongs(artistName, 30)
        loading = false
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val next = page + 1
            val more = runCatching { GdApi.artistSongs(artistName, 30, next) }
                .getOrDefault(emptyList())
            val fresh = more.filter { s -> songs.none { it.id == s.id } }
            if (fresh.isNotEmpty()) {
                songs = songs + fresh
                page = next
            }
            hasMore = fresh.isNotEmpty()
            loadingMore = false
        }
    }

    val albums = remember(songs) {
        songs.groupBy { it.album }.filterKeys { it.isNotBlank() }
    }
    val cover = remember(artistPic, songs) {
        when {
            artistPic.isNotBlank() -> artistPic
            else -> songs.firstOrNull()?.picId
                ?.let { GdApi.picUrl(it, CoverSize.MEDIUM.request) } ?: ""
        }.ifBlank { null }
    }

    DetailPage(
        title = artistName,
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NetCover(
                    url = cover,
                    modifier = Modifier.size(96.dp).clip(CircleShape),
                    corner = 100.dp,
                    maxSize = CoverSize.MEDIUM.decode,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = artistName,
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${songs.size}首歌曲 · ${albums.size}个专辑",
                    color = scheme.onSurfaceSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TabChip("歌曲", selected = tab == 0) { tab = 0 }
                    TabChip("专辑", selected = tab == 1) { tab = 1 }
                }
                Spacer(Modifier.height(10.dp))
            }
        },
        overlay = {

            AnimatedVisibility(
                visible = loading,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(240)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                RefreshIndicator(text = "正在刷新")
            }
        },
    ) {
        if (!loading) when {
            tab == 0 -> {
                if (songs.isEmpty()) item {
                    val a = itemAlpha(0)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = a
                                translationY = (1f - a) * slidePx
                            },
                    ) {
                        EmptyHint("暂无歌曲")
                    }
                }
                itemsIndexed(songs.chunked(columns), key = { _, group -> group.first().id }) { groupIndex, group ->
                    SongPairRow(
                        pair = group,
                        startIndex = groupIndex * columns,
                        columns = columns,
                    ) { index, song ->
                        val a = itemAlpha(index)
                        TrackRow(
                            song = song,
                            onClick = { onPlay(songs, index) },
                            onMore = { onMore(song) },
                            contentPadding = if (columns == 2) {
                                PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            } else {
                                PaddingValues(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                            },
                            modifier = Modifier.graphicsLayer {
                                alpha = a
                                translationY = (1f - a) * slidePx
                            },
                        )
                    }
                }
                if (hasMore) item {
                    val a = itemAlpha(songs.size)
                    LoadMoreButton(
                        loading = loadingMore,
                        onClick = { loadMore() },
                        modifier = Modifier.graphicsLayer {
                            alpha = a
                            translationY = (1f - a) * slidePx
                        },
                    )
                }
            }
            else -> {
                if (albums.isEmpty()) item {
                    val a = itemAlpha(0)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = a
                                translationY = (1f - a) * slidePx
                            },
                    ) {
                        EmptyHint("暂无专辑")
                    }
                }
                albums.entries.forEachIndexed { index, entry ->
                    val album = entry.key
                    val albumSongs = entry.value
                    item(key = "album-$album") {
                        val a = itemAlpha(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onOpenAlbum(album) }
                                .padding(start = 28.dp, top = 8.dp, end = 52.dp, bottom = 8.dp)
                                .graphicsLayer {
                                    alpha = a
                                    translationY = (1f - a) * slidePx
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NetCover(
                                url = GdApi.picUrl(
                                    albumSongs.first().picId,
                                    CoverSize.SMALL.request,
                                ).ifBlank { null },
                                modifier = Modifier.size(44.dp).clip(CircleShape),
                                corner = 22.dp,
                                maxSize = CoverSize.SMALL.decode,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album,
                                    color = scheme.onSurface,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${albumSongs.size} 首",
                                    color = scheme.onSurfaceSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
                if (hasMore) item {
                    val a = itemAlpha(albums.size)
                    LoadMoreButton(
                        loading = loadingMore,
                        onClick = { loadMore() },
                        modifier = Modifier.graphicsLayer {
                            alpha = a
                            translationY = (1f - a) * slidePx
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumScreen(
    albumName: String,
    artistName: String,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
) {
    val columns = songColumns()
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scheme = MiuixTheme.colorScheme
    val slidePx = with(LocalDensity.current) { 12.dp.toPx() }

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(loading) {
        if (!loading) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
    }
    fun itemAlpha(index: Int): Float =
        ((reveal.value - (index * 0.015f).coerceAtMost(0.12f)) / 0.7f).coerceIn(0f, 1f)

    LaunchedEffect(albumName) {
        loading = true
        val all = GdApi.albumSongs(albumName, 30)
        songs = all.filter { it.album.equals(albumName, ignoreCase = true) }.ifEmpty { all }
        loading = false
    }

    val cover = remember(songs) {
        songs.firstOrNull()?.picId?.let { GdApi.picUrl(it, CoverSize.MEDIUM.request).ifBlank { null } }
    }
    val totalMs = remember(songs) { songs.sumOf { it.durationMs } }
    val albumArtistLabel = remember(songs, artistName) {
        val mainArtists = songs.mapNotNull {
            it.artists.firstOrNull()?.trim()?.takeIf { n -> n.isNotEmpty() }
        }.distinct()
        when {
            mainArtists.size > 1 -> "Various Artists"
            artistName.isNotBlank() -> artistName
            else -> songs.firstOrNull()?.artistText?.ifBlank { null } ?: "未知专辑艺术家"
        }
    }

    DetailPage(
        title = albumName,
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NetCover(
                    url = cover,
                    modifier = Modifier.size(96.dp).clip(CircleShape),
                    corner = 48.dp,
                    maxSize = CoverSize.MEDIUM.decode,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = albumName,
                    color = scheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = albumArtistLabel,
                    color = scheme.onSurfaceSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${songs.size} 首歌曲" + if (totalMs > 0) " · ${formatTime(totalMs)}" else "",
                    color = scheme.onSurfaceSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PlayAllButton(onClick = { if (songs.isNotEmpty()) onPlay(songs, 0) })
                Spacer(Modifier.height(8.dp))
            }
        },
        overlay = {
            AnimatedVisibility(
                visible = loading,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(240)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                RefreshIndicator(text = "正在刷新")
            }
        },
    ) {
        if (!loading) when {
            songs.isEmpty() -> item { EmptyHint("暂无歌曲") }
            else -> itemsIndexed(songs.chunked(columns), key = { _, group -> group.first().id }) { groupIndex, group ->
                SongPairRow(
                    pair = group,
                    startIndex = groupIndex * columns,
                    columns = columns,
                ) { index, song ->
                    val a = itemAlpha(index)
                    TrackRow(
                        song = song,
                        onClick = { onPlay(songs, index) },
                        onMore = { onMore(song) },
                        showCover = false,
                        contentPadding = if (columns == 2) {
                            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        } else {
                            PaddingValues(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        },
                        modifier = Modifier.graphicsLayer {
                            alpha = a
                            translationY = (1f - a) * slidePx
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme

    val unselectedBg = scheme.surface.copy(alpha = cardGlassAlpha())
    val interactionSource = remember { MutableInteractionSource() }
    val bounce = rememberChipBounce(interactionSource)
    Box(

        modifier = Modifier
            .chipBounce(bounce)
            .squircleClip(18.dp)
            .background(if (selected) scheme.primary else unselectedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 26.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else scheme.onSurface,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
