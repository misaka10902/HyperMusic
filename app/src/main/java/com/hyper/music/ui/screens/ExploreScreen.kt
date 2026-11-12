package com.hyper.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hyper.music.music.GdApi
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.Song
import com.hyper.music.ui.components.EmptyHint
import com.hyper.music.ui.components.RefreshIndicator
import com.hyper.music.ui.components.TrackRow
import com.hyper.music.ui.basic.chipBounce
import com.hyper.music.ui.basic.rememberChipBounce
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.concurrent.atomic.AtomicInteger
import android.content.Context
import com.hyper.music.music.CoverSize
import androidx.compose.ui.text.font.FontWeight
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Music
import com.hyper.music.music.NetCover
import com.hyper.music.music.CoverLoader
import com.hyper.music.music.SearchAlbum
import com.hyper.music.music.SearchArtist
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.async
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.squircle.squircleClip
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.hyper.music.music.SearchHistoryStore
import com.hyper.music.music.WallpaperManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.interaction.collectIsPressedAsState
import top.yukonga.miuix.kmp.basic.Card
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import com.hyper.music.ui.effects.miuix.cardGlassAlpha
import com.hyper.music.ui.effects.miuix.LocalActiveBackdrop

private val SearchFieldHeight = 46.dp

private val SearchFieldTopGap = 10.dp

private val SectionGap = 12.dp

private val TabChipHeight = 36.dp

private val PlayBarTopReserve = 133.dp

private fun Modifier.fadeVerticalEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.07f to Color.Black,
                0.93f to Color.Black,
                1.00f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
fun ExploreScreen(
    onPlaySongs: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    active: Boolean = true,
    onSearchEngagedChange: (Boolean) -> Unit = {},
    exitSearchSignal: Int = 0,
) {
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var searchedKeyword by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var artists by remember { mutableStateOf<List<SearchArtist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<SearchAlbum>>(emptyList()) }
    var category by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var noMore by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    val searchSeq = remember { AtomicInteger(0) }

    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var inputRef by remember { mutableStateOf<android.widget.EditText?>(null) }
    val imm = remember {
        context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as? android.view.inputmethod.InputMethodManager
    }
    val listState = rememberLazyListState()
    val searched = searchedKeyword.isNotBlank()
    val searchHistory by SearchHistoryStore.items.collectAsState()
    LaunchedEffect(Unit) { SearchHistoryStore.init(context) }

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var pendingClearHistory by remember { mutableStateOf(false) }

    var clearingHistory by remember { mutableStateOf(false) }
    LaunchedEffect(pendingClearHistory) {
        if (!pendingClearHistory) return@LaunchedEffect
        val et = inputRef
        et?.clearFocus()
        val token = et?.windowToken
        if (token != null) imm?.hideSoftInputFromWindow(token, 0)
        keyboard?.hide()
        focused = false
        delay(160)
        showClearHistoryDialog = true
        pendingClearHistory = false
    }

    LaunchedEffect(active) {
        if (!active) {
            val et = inputRef
            et?.clearFocus()
            val token = et?.windowToken
            if (token != null) imm?.hideSoftInputFromWindow(token, 0)
            keyboard?.hide()
        }
    }

    suspend fun runThreeWaySearch(keyword: String, forced: Boolean) {
        val kw = keyword.trim()
        if (kw.isBlank()) return
        if (!forced && kw == searchedKeyword) { loading = false; return }
        loading = true
        val my = searchSeq.incrementAndGet()
        val sJob = scope.async {
            runCatching { GdApi.search(kw, 30, 1) }.getOrElse { emptyList() }
        }
        val aJob = scope.async {
            runCatching { GdApi.searchArtists(kw, 30) }.getOrElse { emptyList() }
        }
        val alJob = scope.async {
            runCatching { GdApi.searchAlbums(kw, 30) }.getOrElse { emptyList() }
        }
        val sList = sJob.await()
        val aList = aJob.await()
        val alList = alJob.await()
        if (my != searchSeq.get()) return
        if (kw != searchedKeyword) category = 0
        songs = sList
        artists = aList
        albums = alList
        searchedKeyword = kw
        SearchHistoryStore.add(kw)
        page = 1
        noMore = sList.size < 30
        loading = false
    }

    fun submit(keyword: String, hideKeyboard: Boolean) {
        val t = keyword.trim()
        if (t.isBlank()) return
        searchJob?.cancel()
        searchJob = scope.launch { runThreeWaySearch(t, forced = true) }
        if (hideKeyboard) {
            keyboard?.hide()

            inputRef?.clearFocus()
        }
    }

    fun exitSearch() {
        searchJob?.cancel()
        query = ""
        searchMode = false
        focused = false

        loading = false
        songs = emptyList()
        artists = emptyList()
        albums = emptyList()
        searchedKeyword = ""
        category = 0
        page = 1
        noMore = false
        loadingMore = false
        val et = inputRef
        et?.clearFocus()
        val token = et?.windowToken
        if (token != null) imm?.hideSoftInputFromWindow(token, 0)
        keyboard?.hide()
    }

    val searchEngaged = searchMode || query.isNotBlank() || searchedKeyword.isNotBlank()

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    fun closeKeyboard() {
        focused = false
        val et = inputRef
        et?.clearFocus()
        val token = et?.windowToken
        if (token != null) imm?.hideSoftInputFromWindow(token, 0)
        keyboard?.hide()
    }

    LaunchedEffect(exitSearchSignal) {
        if (exitSearchSignal <= 0) return@LaunchedEffect
        if (imeVisible) closeKeyboard() else exitSearch()
    }
    LaunchedEffect(searchEngaged) { onSearchEngagedChange(searchEngaged) }
    DisposableEffect(Unit) { onDispose { onSearchEngagedChange(false) } }

    fun loadMoreSongs() {
        if (loadingMore || loading || noMore || searchedKeyword.isBlank()) return
        loadingMore = true
        scope.launch {
            val next = runCatching {
                GdApi.search(searchedKeyword, 30, page + 1)
            }.getOrElse { emptyList() }
            val fresh = next.filter { n -> songs.none { it.id == n.id } }
            if (fresh.isEmpty()) noMore = true else {
                songs = songs + fresh
                page += 1
            }
            loadingMore = false
        }
    }

    LaunchedEffect(query) {
        val kw = query.trim()
        if (kw.isBlank()) {
            songs = emptyList()
            artists = emptyList()
            albums = emptyList()
            searchedKeyword = ""
            loading = false
            page = 1
            noMore = false
            return@LaunchedEffect
        }
        delay(500)
        if (query.trim() != kw) return@LaunchedEffect

        if (kw == searchedKeyword) return@LaunchedEffect
        searchJob?.cancel()
        searchJob = scope.launch { runThreeWaySearch(kw, forced = false) }
    }

    LaunchedEffect(listState, songs.size, category) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (searched && category == 0 && songs.isNotEmpty() && last >= songs.size - 6) {
                    loadMoreSongs()
                }
            }
    }

    val screenKey = when {
        !searchMode -> "browse"
        !searched -> "history"
        else -> "results-$category"
    }

    val scheme = MiuixTheme.colorScheme
    val wallpaperActive = WallpaperManager.state.collectAsState().value.hasImage

    val tabsVisible = searchMode && searched

    val searchFieldZone = SearchFieldTopGap + SearchFieldHeight + SectionGap
    val tabZone = if (tabsVisible) TabChipHeight else 0.dp
    val headerHeight = searchFieldZone + tabZone
    val contentTop = headerHeight

    val screenAlpha = remember(screenKey) { Animatable(0f) }
    val screenSlidePx = with(LocalDensity.current) { 16.dp.toPx() }
    LaunchedEffect(screenKey) {
        screenAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topInset),
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = screenAlpha.value
                    translationY = (1f - screenAlpha.value) * screenSlidePx
                },
        ) {
            when {

                loading && searchedKeyword.isBlank() -> SearchLoadingIndicator()

                !searchMode -> RecommendSection(
                    onPlaySongs = onPlaySongs,
                    topPadding = contentTop,
                )

                !searched -> SearchHistorySection(
                    history = searchHistory,
                    topPadding = contentTop,
                    clearing = clearingHistory,
                    onClearFinished = {
                        SearchHistoryStore.clear()
                        clearingHistory = false
                    },
                    onPick = { kw ->
                        query = kw
                        submit(kw, hideKeyboard = true)
                    },
                    onClear = { pendingClearHistory = true },
                )

                else -> SearchResults(
                    category = category,
                    songs = songs,
                    artists = artists,
                    albums = albums,
                    loading = loading,
                    listState = listState,
                    topPadding = contentTop,
                    onPlaySongs = onPlaySongs,
                    onMore = onMore,
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onLoadMoreSongs = { loadMoreSongs() },
                    loadingMoreSongs = loadingMore,
                )
            }
        }

        if (!wallpaperActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentTop)
                    .background(scheme.surface),
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { submit(query.trim(), hideKeyboard = true) },
                onFocusChange = { focused = it; if (it) searchMode = true },
                searchMode = searchMode,
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, top = SearchFieldTopGap),
                onInputCreated = { inputRef = it },
                onInputReleased = { inputRef = null },
            )
            if (tabsVisible) {

                Spacer(Modifier.height(SectionGap))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TabChipHeight)
                        .padding(start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SegmentTab("歌曲", selected = category == 0) { category = 0 }
                    SegmentTab("歌手", selected = category == 1) { category = 1 }
                    SegmentTab("专辑", selected = category == 2) { category = 2 }
                }
            }
        }

        val showExit = searchMode || query.isNotBlank()
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp, top = SearchFieldTopGap)
                .size(SearchFieldHeight)
                .zIndex(10f)
                .then(
                    if (showExit) Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { exitSearch() })
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = showExit,
                enter = scaleIn(
                    initialScale = 0f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
                ) + fadeIn(animationSpec = tween(120)),
                exit = scaleOut(
                    targetScale = 0f,
                    animationSpec = tween(160),
                ) + fadeOut(animationSpec = tween(140)),
            ) {
                Box(
                    modifier = Modifier
                        .size(SearchFieldHeight)
                        .clip(CircleShape)
                        .cardGlass(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        imageVector = MiuixIcons.Close,
                        contentDescription = "退出搜索",
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(scheme.onSurfaceSecondary),
                    )
                }
            }
        }

        ConfirmDialog(
            show = showClearHistoryDialog,
            title = "清除搜索历史",
            summary = "确认清空所有搜索历史吗？",
            confirmText = "清空",
            onConfirm = {
                showClearHistoryDialog = false

                clearingHistory = true
            },
            onDismiss = { showClearHistoryDialog = false },
            backdrop = LocalActiveBackdrop.current,
        )
    }

}

@Composable
private fun BrandEmptyState() {
    val scheme = MiuixTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                imageVector = MiuixIcons.Music,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                colorFilter = ColorFilter.tint(scheme.onSurfaceSecondary.copy(alpha = 0.55f)),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "想听什么？搜一下吧！",
                color = scheme.onSurfaceSecondary.copy(alpha = 0.85f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun SearchResults(
    category: Int,
    songs: List<Song>,
    artists: List<SearchArtist>,
    albums: List<SearchAlbum>,
    loading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    topPadding: Dp,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    onLoadMoreSongs: () -> Unit,
    loadingMoreSongs: Boolean,
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,

                top = topPadding + SectionGap,

                bottom = PlayBarTopReserve + SectionGap,
            ),
    ) {
        Card(
            modifier = Modifier.fillMaxSize().cardGlass(RoundedCornerShape(20.dp)),
            cornerRadius = 20.dp,
            insideMargin = PaddingValues(vertical = 6.dp),
            colors = cardGlassColors(),
        ) {
            Box(modifier = Modifier.weight(1f).fadeVerticalEdges()) {
                when (category) {
                    0 -> SongResultList(
                        songs = songs,
                        loading = loading,
                        listState = listState,
                        onPlaySongs = onPlaySongs,
                        onMore = onMore,
                        onLoadMore = onLoadMoreSongs,
                        loadingMore = loadingMoreSongs,
                    )

                    1 -> if (artists.isEmpty() && !loading) {
                        EmptyHint(
                            text = "没有找到相关歌手",
                            centered = true,
                            icon = MiuixIcons.Contacts,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 2.dp),
                        ) {
                            items(artists, key = { it.id }) { a ->
                                ArtistResultRow(artist = a, onClick = { onOpenArtist(a.name) })
                            }
                        }
                    }

                    2 -> if (albums.isEmpty() && !loading) {
                        EmptyHint(
                            text = "没有找到相关专辑",
                            centered = true,
                            icon = MiuixIcons.Album,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 2.dp),
                        ) {
                            items(albums, key = { it.id }) { al ->
                                AlbumResultRow(album = al, onClick = { onOpenAlbum(al.name, al.artist) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentTab(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme

    val unselectedBg = scheme.surfaceContainer.copy(alpha = cardGlassAlpha())
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else scheme.onSurface,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun SongResultList(
    songs: List<Song>,
    loading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onLoadMore: () -> Unit,
    loadingMore: Boolean,
) {
    if (songs.isEmpty() && !loading) {
        EmptyHint(
            text = "没有找到相关歌曲",
            centered = true,
            icon = MiuixIcons.Music,
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            itemsIndexed(songs, key = { index, s -> "${s.id}#$index" }) { index, song ->
                TrackRow(
                    song = song,
                    onClick = { onPlaySongs(songs, index) },
                    onMore = { onMore(song) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    moreAlignEnd = true,
                )
            }
            if (loadingMore) {
                item(key = "loadingMore") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        RefreshIndicator(text = "正在加载", size = 20.dp, strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistResultRow(artist: SearchArtist, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetCover(
            url = artist.avatar.ifBlank { null },
            modifier = Modifier.size(48.dp),
            corner = 100.dp,
            maxSize = CoverSize.SMALL.decode,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                color = scheme.onSurface,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "歌手",
                color = scheme.onSurfaceSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun AlbumResultRow(album: SearchAlbum, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetCover(
            url = album.cover.ifBlank { null },
            modifier = Modifier.size(48.dp),
            corner = 100.dp,
            maxSize = CoverSize.SMALL.decode,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                color = scheme.onSurface,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = album.artist.ifBlank { "未知艺人" },
                color = scheme.onSurfaceSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    searchMode: Boolean = false,
    onInputCreated: (android.widget.EditText) -> Unit = {},
    onInputReleased: () -> Unit = {},
) {
    val scheme = MiuixTheme.colorScheme
    val height = SearchFieldHeight
    val primaryArgb = scheme.primary.toArgb()
    val fieldShape = RoundedCornerShape(height / 2)
    val density = LocalDensity.current

    val searchActive = searchMode || query.isNotBlank()

    val transition = updateTransition(searchActive, label = "glassSearch")
    val progress = transition.animateFloat(
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
        label = "glassSearchGeometry",
    ) { if (it) 1f else 0f }
    val endSpacePx = with(density) { 60.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val endSpace = (endSpacePx * progress.value).toInt()
                    val width = (constraints.maxWidth - endSpace).coerceAtLeast(0)
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width, maxWidth = width)
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }
                .height(height)
                .clip(fieldShape)
                .cardGlass(fieldShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Image(
                imageVector = MiuixIcons.Search,
                contentDescription = null,
                modifier = Modifier.size(21.dp),
                colorFilter = ColorFilter.tint(scheme.onSurfaceSecondary),
            )
            Spacer(Modifier.size(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                NativeSearchInput(
                    value = query,
                    onValueChange = onQueryChange,
                    onSearch = onSearch,
                    onFocusChange = onFocusChange,
                    hint = "搜索歌曲、歌手或专辑...",
                    primaryArgb = primaryArgb,
                    textColorArgb = scheme.onSurface.toArgb(),
                    hintColorArgb = scheme.onSurfaceSecondary.toArgb(),
                    modifier = Modifier.fillMaxWidth(),
                    onInputCreated = onInputCreated,
                    onInputReleased = onInputReleased,
                )
            }
        }

    }
}

@Composable
private fun NativeSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    hint: String,
    primaryArgb: Int,
    textColorArgb: Int,
    hintColorArgb: Int,
    modifier: Modifier = Modifier,
    onInputCreated: (android.widget.EditText) -> Unit = {},
    onInputReleased: () -> Unit = {},
) {
    val watcher = remember(onValueChange) {
        object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                s?.toString()?.let { if (it != value) onValueChange(it) }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            android.widget.EditText(ctx).apply {
                setSingleLine(true)
                background = null
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                setPadding(0, 0, 0, 0)
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH or
                    android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
                textSize = 17f
                typeface = android.graphics.Typeface.create(
                    typeface, android.graphics.Typeface.NORMAL,
                )
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                        onSearch()
                        true
                    } else false
                }
                addTextChangedListener(watcher)
                onFocusChangeListener =
                    android.view.View.OnFocusChangeListener { _, hasFocus -> onFocusChange(hasFocus) }
                setOnClickListener { view ->
                    view.requestFocus()
                    view.post {
                        (ctx.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as? android.view.inputmethod.InputMethodManager)
                            ?.showSoftInput(view, 0)
                    }
                }
                onInputCreated(this)
            }
        },
        onRelease = { onInputReleased() },
        update = { et ->
            et.hint = hint
            et.setTextColor(textColorArgb)
            et.setHintTextColor(hintColorArgb)
            et.highlightColor = android.graphics.Color.argb(
                51,
                android.graphics.Color.red(primaryArgb),
                android.graphics.Color.green(primaryArgb),
                android.graphics.Color.blue(primaryArgb),
            )
            runCatching {
                et.textCursorDrawable?.setTint(primaryArgb)
                et.textSelectHandle?.setTint(primaryArgb)
                et.textSelectHandleLeft?.setTint(primaryArgb)
                et.textSelectHandleRight?.setTint(primaryArgb)
            }
            if (et.text?.toString() != value) {
                et.removeTextChangedListener(watcher)
                et.setText(value)
                et.setSelection(value.length)
                et.addTextChangedListener(watcher)
            }
        },
        modifier = modifier,
    )
}

private object RecommendCache {
    var songs: List<Song>? = null
}

@Composable
private fun RecommendSection(onPlaySongs: (List<Song>, Int) -> Unit, topPadding: Dp) {
    val history by MusicLibrary.history.collectAsState()
    val playlists by MusicLibrary.playlists.collectAsState()

    val songs: List<Song> = RecommendCache.songs ?: run {
        val pool = (history + playlists.flatMap { it.songs }).distinctBy { it.id }
        val computed = if (pool.isEmpty()) emptyList() else pool.shuffled().take(10)
        if (computed.isNotEmpty()) RecommendCache.songs = computed
        computed
    }

    LaunchedEffect(RecommendCache.songs) {
        RecommendCache.songs?.forEach { s ->
            val c = s.cover
            if (c.isNotBlank()) CoverLoader.pin(c, CoverSize.MEDIUM.decode)
        }
    }
    if (songs.isEmpty()) {
        BrandEmptyState()
        return
    }
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().padding(top = topPadding)) {
        Text(
            text = "猜你想听",
            color = scheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 14.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                RecommendCard(song = song) { onPlaySongs(songs, index) }
            }
        }
    }
}

@Composable
private fun RecommendCard(song: Song, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val bounce = rememberChipBounce(interactionSource)
    val highlightAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.12f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "recommendCardHighlight",
    )
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(280.dp)
            .chipBounce(bounce)
            .squircleClip(20.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        NetCover(
            url = song.cover.ifBlank { null },
            modifier = Modifier.fillMaxSize(),
            corner = 0.dp,
            maxSize = CoverSize.MEDIUM.decode,
            pinned = true,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.62f),
                        )
                    )
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = highlightAlpha)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
        ) {
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.artistText,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySection(
    history: List<String>,
    topPadding: Dp,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    clearing: Boolean = false,
    onClearFinished: () -> Unit = {},
) {
    val scheme = MiuixTheme.colorScheme

    val chipBg = scheme.surfaceContainer.copy(alpha = cardGlassAlpha())

    var removingKeywords by remember { mutableStateOf(setOf<String>()) }
    val clearScope = rememberCoroutineScope()
    LaunchedEffect(history) {
        if (removingKeywords.isNotEmpty()) {
            removingKeywords = removingKeywords.intersect(history.toSet())
        }
    }

    val clearAlpha = remember { Animatable(1f) }
    LaunchedEffect(clearing) {
        if (!clearing) {
            clearAlpha.snapTo(1f)
            return@LaunchedEffect
        }
        clearAlpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        onClearFinished()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding + 8.dp, bottom = 140.dp),
    ) {
        Text(
            text = "搜索历史",
            color = scheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 16.dp),
        )
        if (history.isEmpty()) {
            Text(
                text = "暂无搜索历史",
                color = scheme.onSurfaceSecondary.copy(alpha = 0.75f),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 28.dp),
            )
        } else {

            Box(modifier = Modifier.graphicsLayer { alpha = clearAlpha.value }) {
            Column {
            FlowRow(
                modifier = Modifier.padding(start = 28.dp, end = 18.dp),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                history.forEach { keyword ->
                    key(keyword) {
                        HistoryChip(
                            keyword = keyword,
                            removing = keyword in removingKeywords,
                            chipBg = chipBg,
                            onPick = { onPick(keyword) },
                            onLongClick = {
                                if (keyword !in removingKeywords) {
                                    removingKeywords = removingKeywords + keyword
                                }
                            },

                            onGone = { SearchHistoryStore.remove(keyword) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val clearInteraction = remember { MutableInteractionSource() }
                val clearBounce = rememberChipBounce(clearInteraction)
                Box(
                    modifier = Modifier
                        .chipBounce(clearBounce)
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipBg)
                        .clickable(
                            interactionSource = clearInteraction,
                            indication = null,
                            onClick = {

                                clearScope.launch {
                                    delay(120)
                                    onClear()
                                }
                            },
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "清除所有",
                        color = scheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            }
            }
        }
    }
}

@Composable
private fun HistoryChip(
    keyword: String,
    removing: Boolean,
    chipBg: Color,
    onPick: () -> Unit,
    onLongClick: () -> Unit,
    onGone: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme

    val progress = remember { Animatable(0f) }
    LaunchedEffect(removing) {
        if (removing) {
            progress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            onGone()
        }
    }

    val p = progress.value
    val interactionSource = remember { MutableInteractionSource() }
    val bounce = rememberChipBounce(interactionSource)

    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val w = (placeable.width * (1f - p)).roundToInt()
                layout(w, placeable.height) {

                    placeable.placeRelative(0, 0)
                }
            },
    ) {
        Row(

            modifier = Modifier.graphicsLayer { alpha = 1f - p },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(

                modifier = Modifier
                    .height(40.dp)
                    .graphicsLayer {
                        val s = bounce.scale.value * (1f - p)
                        scaleX = s
                        scaleY = s
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(chipBg)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { if (!removing) onPick() },
                        onLongClick = { if (!removing) onLongClick() },
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = keyword,
                    color = scheme.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(10.dp))
        }
    }
}

private val SearchLoadingTop = 320.dp

@Composable
private fun SearchLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        RefreshIndicator(
            text = "正在刷新",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = SearchLoadingTop),
        )
    }
}
