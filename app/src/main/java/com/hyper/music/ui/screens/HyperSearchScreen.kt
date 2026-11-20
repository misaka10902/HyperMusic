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

/**
 * 全屏搜索的"内容层"，作为 [com.hyper.music.ui.search.GlassSearchOverlay] 的 content 使用。
 * 搜索框由 GlassSearchOverlay 管理（含转场动画），本组件只负责：
 *  - 根据 [query] 做防抖三路搜索（歌曲 / 歌手 / 专辑）
 *  - 搜索历史、结果、GlassTabRow（都位于搜索框下方）
 *
 * 严格复用 ExploreScreen 中已有的结果/历史 UI（internal 化），不引入任何新 textfield。
 */
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
    // 三个结果分类改成横向分页：可左右滑，点击 Tab 时用平移动画切页。
    val pagerState = rememberPagerState(pageCount = { 3 })
    val searched = searchedKeyword.isNotBlank()
    val searchHistory by SearchHistoryStore.items.collectAsState()
    val wallpaperState by WallpaperManager.state.collectAsState()
    // 内容层由 GlassSearchOverlay 铺满整屏，坐标与屏幕一致：
    // 搜索框底部 = 状态栏 + 上下各 4dp 内边距 + 44dp 输入框。
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
            // 先把数据落定，再切 searchedKeyword 触发历史→结果的转场，
            // 否则转场动画期间渲染的还是上一次的空列表。
            songs = sList
            artists = aList
            albums = alList
            page = 1
            noMore = sList.size < 30
            if (kw != searchedKeyword) category = 0
            searchedKeyword = kw
            SearchHistoryStore.add(kw)
        } finally {
            // 取消 / 异常都要复位，避免 loading 卡在 true 把结果区顶成加载态。
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

    // query 由 GlassSearchOverlay 的输入框驱动，这里做防抖搜索。
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

    // 关闭搜索：只取消在途请求。搜索结果保留到覆盖层退场动画结束之后，
    // 否则退场那一刻列表被清空，会闪出一帧「没有歌曲 / 歌手 / 专辑」。
    // 覆盖层收起后内容被移出组合、状态自然释放，下次打开是干净的初始态。
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

    // 搜索历史 → 搜索中 → 搜索结果：三态交叉淡入淡出，避免硬切。
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
            // 键盘避让挂在这一层（而不是覆盖层的整列）：
            // 这样内容列始终保持整屏高度，结果页的采样底面壁纸不会随键盘上移。
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
                        // 历史 chip 被点击：直接发起搜索（回填搜索框由 GlassSearchOverlay 的输入框负责，
                        // 这里通过 searchedKeyword 驱动结果显示）。
                        scope.launch { runThreeWaySearch(keyword) }
                    },
                    onClear = { showClearHistoryDialog = true },
                )
            }
        }
        SearchStage.Loading -> Box(modifier = Modifier.fillMaxSize().imePadding()) { SearchLoadingView() }
        SearchStage.Results -> {
            val advancedHeader = LocalAdvancedMaterialEnabled.current
            // TabRow 与搜索框之间恒为 12dp。
            val tabTop = searchBarBottom + 12.dp
            val listTop = tabTop + GlassTabRowDefaults.Height + 12.dp
            // 与官方 GlassSearchOverlay 一致：带高 = 头部（搜索框 + TabRow）+ 24dp 溢出，
            // 不要再往下拉长，否则整块头部都被糊成一片。
            val scrimHeight = tabTop + GlassTabRowDefaults.Height + 24.dp
            val headerBackdrop = rememberLayerBackdrop { drawContent() }

            // 内容是滚动进顶栏区域的：只在真正有内容钻到搜索框后面时才渐显模糊带，
            // 与顶栏 progressiveBlur 的「滚动了才有」保持一致。
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

            // 列表顶部 padding = listTop；从屏幕顶端向下滚时内容一路钻到搜索框 / TabRow 背后，
            // 由顶部那条从屏幕最顶端开始的模糊带收尾，不会出现硬切的边。
            Box(modifier = Modifier.fillMaxSize()) {
                // 结果区（含 pager）已进入组合、参与测量之后再复位分页，
                // 避免在搜索协程里对一个还没组合的 pager 做滚动而卡住整个搜索流程。
                LaunchedEffect(searchedKeyword) {
                    if (searchedKeyword.isNotBlank()) {
                        if (pagerState.currentPage != 0) pagerState.scrollToPage(0)
                        // 新的一次搜索回到列表顶部，顶部的模糊带也回到未激活状态。
                        listState.scrollToItem(0)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(headerBackdrop),
                ) {
                    // 采样底面：progressiveTextureBlur 采的是「这一层绘制出来的像素」快照，
                    // 不是屏幕合成结果。列表上方是 padding、行与行之间也是透明的，
                    // 这些 alpha=0 的像素在预乘空间里就是纯黑；模糊会先把它降到 1/8~1/16
                    // 分辨率再做加权，于是一块块黑斑就被放大出来——图里那些色块就是这么来的
                    //（官方 GlassSearchOverlay 在 layerBackdrop 的容器上同样垫了一层
                    //  background(colors.surface)，就是为了消除这层透明）。
                    // 这里垫的是搜索层背后真实的东西：壁纸；没有壁纸时退回页面底色。
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
                        // 只有列表避让键盘：壁纸与顶部模糊带保持整屏，键盘弹起时不会跟着动。
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

                // 顶部收尾：高级材质开 → 跟随内容滚动的渐进模糊；
                // 关 → 半透明渐变。高度盖到 TabRow 底部再往下一段，自然消失。
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
                            // 点击 Tab：动画平移到对应页，而不是硬切换。
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

/** 搜索区的三种形态：历史 → 搜索中 → 结果。 */
private enum class SearchStage { History, Loading, Results }

/** 防抖结束后、三路请求返回前的加载态，避免历史页一顿直接硬切成结果页。 */
@Composable
private fun SearchLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        RefreshIndicator(text = "正在搜索", size = 22.dp, strokeWidth = 2.dp)
    }
}

/**
 * 搜索结果顶部收尾：让滚到搜索框 / TabRow 背后的内容自然过渡，而不是被一条直线裁掉。
 *
 * 高级材质开启时用 miuix 的渐进模糊（与顶栏 progressiveBlur 同一套管线），
 * 关闭时退化为半透明渐变，成本低、效果接近。
 */
@Composable
private fun SearchHeaderScrim(
    backdrop: Backdrop,
    advanced: Boolean,
    strength: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    // 与 GlassTopAppBar 的 progressiveBlur 完全同一套参数：同一半径、同一曲线、
    // 同一层 blend 颜色，视觉上就像顶栏那条模糊带，而不是另做一种效果。
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
