package com.hyper.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hyper.music.music.GdApi
import com.hyper.music.music.SearchAlbum
import com.hyper.music.music.SearchArtist
import com.hyper.music.music.SearchHistoryStore
import com.hyper.music.music.Song
import com.hyper.music.music.WallpaperBackground
import com.hyper.music.music.WallpaperManager
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.components.RefreshIndicator
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.effects.miuix.rememberPopupBackdrop
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassTabRow
import top.yukonga.miuix.kmp.glass.GlassTabRowDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.concurrent.atomic.AtomicInteger


@Composable
fun ColumnScope.HyperSearchScreen(
    query: String,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchedKeyword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var artists by remember { mutableStateOf<List<SearchArtist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<SearchAlbum>>(emptyList()) }
    var category by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var noMore by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    val searchSeq = remember { AtomicInteger(0) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val searched = searchedKeyword.isNotBlank()
    val searchHistory by SearchHistoryStore.items.collectAsState()
    val wallpaperState by WallpaperManager.state.collectAsState()


    val density = LocalDensity.current
    val searchBarBottom = with(density) { WindowInsets.statusBars.getTop(density).toDp() } + 52.dp

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var clearingHistory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { SearchHistoryStore.init(context) }

    suspend fun runThreeWaySearch(keyword: String) {
        val kw = keyword.trim()
        if (kw.isBlank()) return
        val my = searchSeq.incrementAndGet()
        loading = true
        try {
            val sJob = scope.async { runCatching { GdApi.search(kw, 30, 1) }.getOrElse { emptyList() } }
            val aJob = scope.async { runCatching { GdApi.searchArtists(kw, 30) }.getOrElse { emptyList() } }
            val alJob = scope.async { runCatching { GdApi.searchAlbums(kw, 30) }.getOrElse { emptyList() } }
            val sList = sJob.await()
            val aList = aJob.await()
            val alList = alJob.await()
            if (my != searchSeq.get()) return


            songs = sList
            artists = aList
            albums = alList
            page = 1
            noMore = sList.size < 30
            if (kw != searchedKeyword) category = 0
            searchedKeyword = kw
            SearchHistoryStore.add(kw)
        } finally {

            if (my == searchSeq.get()) loading = false
        }
    }

    fun loadMoreSongs() {
        if (loadingMore || loading || noMore || searchedKeyword.isBlank()) return
        loadingMore = true
        scope.launch {
            val next = runCatching { GdApi.search(searchedKeyword, 30, page + 1) }.getOrElse { emptyList() }
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
            searchJob?.cancel()
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
        searchJob = scope.launch { runThreeWaySearch(kw) }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { category = it }
    }

    LaunchedEffect(listState, songs.size, category) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (searched && category == 0 && songs.isNotEmpty() && last >= songs.size - 6) {
                    loadMoreSongs()
                }
            }
    }


    LaunchedEffect(active) {
        if (!active) {
            searchJob?.cancel()
            delay(520)
            searchedKeyword = ""
            songs = emptyList()
            artists = emptyList()
            albums = emptyList()
            category = 0
            page = 1
            noMore = false
            loadingMore = false
            loading = false
        }
    }

    DisposableEffect(Unit) { onDispose { searchJob?.cancel() } }


    val stage = when {
        searched -> SearchStage.Results
        loading -> SearchStage.Loading
        else -> SearchStage.History
    }
    AnimatedContent(
        targetState = stage,
        modifier = modifier.fillMaxWidth().weight(1f),
        transitionSpec = {
            fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(160))
        },
        label = "searchStage",
    ) { current ->
        when (current) {
        SearchStage.History -> {


            Box(modifier = Modifier.fillMaxSize().imePadding()) {
                SearchHistorySection(
                    history = searchHistory,
                    topPadding = searchBarBottom + 12.dp,
                    clearing = clearingHistory,
                    onClearFinished = {
                        SearchHistoryStore.clear()
                        clearingHistory = false
                    },
                    onPick = { keyword ->


                        scope.launch { runThreeWaySearch(keyword) }
                    },
                    onClear = { showClearHistoryDialog = true },
                )
            }
        }
        SearchStage.Loading -> Box(modifier = Modifier.fillMaxSize().imePadding()) { SearchLoadingView() }
        SearchStage.Results -> {
            val advancedHeader = LocalAdvancedMaterialEnabled.current

            val tabTop = searchBarBottom + 12.dp
            val listTop = tabTop + GlassTabRowDefaults.Height + 12.dp


            val scrimHeight = tabTop + GlassTabRowDefaults.Height + 24.dp
            val headerBackdrop = rememberLayerBackdrop { drawContent() }


            val rampPx = with(density) { 48.dp.toPx() }
            val scrolled by remember(listState) {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex > 0) 1f
                    else (listState.firstVisibleItemScrollOffset / rampPx).coerceIn(0f, 1f)
                }
            }
            val headerStrength by animateFloatAsState(
                targetValue = scrolled,
                animationSpec = tween(200),
                label = "searchHeaderBand",
            )


            Box(modifier = Modifier.fillMaxSize()) {


                LaunchedEffect(searchedKeyword) {
                    if (searchedKeyword.isNotBlank()) {
                        if (pagerState.currentPage != 0) pagerState.scrollToPage(0)

                        listState.scrollToItem(0)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(headerBackdrop),
                ) {


                    if (wallpaperState.hasImage) {
                        WallpaperBackground()
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MiuixTheme.colorScheme.surface),
                        )
                    }
                    HorizontalPager(
                        state = pagerState,

                        modifier = Modifier.fillMaxSize().imePadding(),
                        beyondViewportPageCount = 1,
                    ) { page ->
                        SearchResults(
                            category = page,
                            songs = songs,
                            artists = artists,
                            albums = albums,
                            loading = loading,
                            listState = listState,
                            topPadding = listTop,
                            onPlaySongs = onPlaySongs,
                            onMore = onMore,
                            onOpenArtist = onOpenArtist,
                            onOpenAlbum = onOpenAlbum,
                            onLoadMoreSongs = { loadMoreSongs() },
                            loadingMoreSongs = loadingMore,
                        )
                    }
                }


                SearchHeaderScrim(
                    backdrop = headerBackdrop,
                    advanced = advancedHeader,
                    strength = headerStrength,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scrimHeight),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = tabTop)
                        .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding)
                        .zIndex(2f),
                ) {
                    GlassTabRow(
                        tabs = listOf("歌曲", "歌手", "专辑"),
                        selectedIndex = pagerState.currentPage,
                        onSelect = { index ->

                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        backdrop = null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = GlassTabRowDefaults.primaryColors(),
                    )
                }
            }
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
        backdrop = rememberPopupBackdrop(),
    )
}


private enum class SearchStage { History, Loading, Results }


@Composable
private fun SearchLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        RefreshIndicator(text = "正在搜索", size = 22.dp, strokeWidth = 2.dp)
    }
}


@Composable
private fun SearchHeaderScrim(
    backdrop: Backdrop,
    advanced: Boolean,
    strength: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme


    if (advanced) {
        val blendColors = remember(scheme.surface) {
            listOf(BlendColorEntry(scheme.surface.copy(alpha = 0.3f)))
        }
        val colors = BlurDefaults.blurColors(blendColors = blendColors)
        Box(
            modifier = modifier
                .graphicsLayer { alpha = strength }
                .progressiveTextureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
                    blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value,
                    colors = colors,
                    enabled = strength > 0.01f,
                ),
        )
    } else {
        Box(
            modifier = modifier
                .graphicsLayer { alpha = strength }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to scheme.surface.copy(alpha = 0.94f),
                            0.5f to scheme.surface.copy(alpha = 0.55f),
                            1f to Color.Transparent,
                        ),
                    )
                },
        )
    }
}
