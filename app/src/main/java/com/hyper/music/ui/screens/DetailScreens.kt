package com.hyper.music.ui.screens
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.takeOrElse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.NetCover
import com.hyper.music.music.Song
import com.hyper.music.music.WallpaperManager
import com.hyper.music.music.WallpaperBackground
import com.hyper.music.ui.basic.chipBounce
import com.hyper.music.ui.basic.rememberChipBounce
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.effects.miuix.LocalActiveBackdrop
import com.hyper.music.ui.effects.miuix.rememberPopupBackdrop
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
import com.hyper.music.ui.components.DragReorderState
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
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Item
import top.yukonga.miuix.kmp.icon.glass.Music1
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
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer

private val DetailBottomReserve = 76.dp

/** 详情页可选的下拉刷新配置；为 null 时页面不挂刷新。 */
internal class PullRefreshSpec(
    val isRefreshing: Boolean,
    val onRefresh: () -> Unit,
    val texts: List<String> = listOf("下拉刷新", "松手刷新", "正在刷新", "刷新成功"),
)

private const val EXPORT_TIP =
    "点击即可将该歌单以JSON格式导出，导出的歌单和 Otter Music 等支持从JSON文件导入歌单的应用互通"

/**
 * 列表删除条目时的补位动画。
 *
 * 被删的条目按 `fadeOutSpec` 淡出，其余条目按这个 spec 平移补位，两者同一帧起步，
 * 因此是「消失 + 平移」的联动，而不是先空一块再跳上来。
 * 拖拽排序期间返回 null：那段时间条目是靠 graphicsLayer 平移的，
 * 数据一换位置再叠一层布局动画就会先弹回原位再滑过去。
 */
private fun DragReorderState.placementSpec() =
    if (isDragging || settling || suppressPlacement) {
        null
    } else {
        spring<IntOffset>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    }

/**
 * 按 pager 的连续页码在 `[0, lastIndex]` 之间插值。
 *
 * `currentPage + currentPageOffsetFraction` 在翻页过程中是连续的（0 → 1 → 2 …），
 * 用它插值可以让「跟随列表滚动量」的东西（表头位移、标题透明度）在横滑时平滑过渡；
 * 只读 `settledPage` 的话，手指松开、页面停稳的那一帧数值会突然跳到新页，看着就是一次突变。
 */
private inline fun interpolateOverPages(
    pagerState: PagerState,
    lastIndex: Int,
    valueAt: (Int) -> Float,
): Float {
    if (lastIndex <= 0) return valueAt(0)
    val p = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, lastIndex.toFloat())
    val lo = floor(p).toInt().coerceIn(0, lastIndex)
    val hi = (lo + 1).coerceAtMost(lastIndex)
    val t = (p - lo).coerceIn(0f, 1f)
    val start = valueAt(lo)
    return if (t <= 0f) start else start + (valueAt(hi) - start) * t
}

@Composable
internal fun DetailPage(
    title: String,
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    header: @Composable () -> Unit,
    background: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    compactTitle: Boolean = false,
    // 默认隐藏顶栏小标题，页面滚过头图后才淡入（专辑 / 艺术家 / 歌单 / 喜欢页在用）。
    autoHideTitle: Boolean = false,
    subtitle: String? = null,
    listState: LazyListState = rememberLazyListState(),
    // 分页模式：给「艺术家页」这类需要左右横滑切 Tab 的页面用。
    // 每个 page 自己一份 LazyListState，表头随页面一起横移（整页平移，而不是只换内容）。
    pagerState: PagerState? = null,
    pagerStates: List<LazyListState> = emptyList(),
    pagerList: (LazyListScope.(Int) -> Unit)? = null,
    // 关于页用：背景完全交给 background()（流光），不画壁纸，卡片/顶栏的采样对象
    // 也换成调用方给的这一层，否则流光页的毛玻璃会去糊一张不存在的壁纸。
    hideWallpaper: Boolean = false,
    activeBackdrop: LayerBackdrop? = null,
    // 挂在滚动内容外面的修饰符。关于页用它做「只渐变内容、不渐变流光背景」。
    listModifier: Modifier = Modifier,
    endAction: (@Composable () -> Unit)? = null,
    endActionKey: Any? = null,
    multiSelect: Boolean = false,
    // 多选时的选中数量：> 0 时标题显示「已选择 N 项」，与歌单详情页保持一致。
    selectionCount: Int = 0,
    onExitMultiSelect: (() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    pullRefresh: PullRefreshSpec? = null,
    list: LazyListScope.() -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val scrollBehavior = MiuixScrollBehavior()
    val density = LocalDensity.current

    // 本页自己的壁纸层：只录壁纸，嵌在内容层内部（与 miuix demo 一致）。
    // 之前用的是全局根层那一份，页面转场后坐标还是旧的，采样会错位、
    // 并把上一页的内容带出来。
    val wallpaperBackdrop = rememberLayerBackdrop()

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeightDp = with(density) { headerHeightPx.toDp() }

    // 分页模式（艺术家页）下，表头位移直接读「表头占位 item」在视口里的偏移：
    // 占位 item 就是给表头留的那块高度（顶栏 + 表头），它跟歌曲在同一个列表里，
    // 所以这块偏移就是列表真实的滚动量，1:1、连续、没有第二个坐标系。
    //
    // 不用 firstVisibleItemScrollOffset：它相对 contentPadding / 占位 item 的语义
    // 在各版本里不一致，之前「一上滑表头就整个消失」就是这里读错了值（一上来就
    // 被 coerce 到满高度）。这里直接拿布局偏移，没有歧义。
    val pagerHeaderScroll = remember(pagerState, pagerStates) {
        if (pagerState == null || pagerStates.isEmpty()) {
            null
        } else {
            derivedStateOf {
                val height = headerHeightPx.toFloat()
                interpolateOverPages(pagerState, pagerStates.lastIndex) { index ->
                    val state = pagerStates.getOrNull(index) ?: return@interpolateOverPages 0f
                    val first = state.layoutInfo.visibleItemsInfo.firstOrNull()
                    val raw = if (first == null || first.index > 0) {
                        height
                    } else {
                        -first.offset.toFloat()
                    }
                    raw.coerceIn(0f, height)
                }
            }
        }
    }

    // 小标题透明度：没滚动时隐藏，头图滚走约七成后完全显示；多选态始终显示
    // （标题此时是「已选择 N 项」，必须可见）。在绘制阶段读，不引起重组。
    //
    // 分页模式（艺术家页）下两页的滚动量可能不同，横滑时要按连续页码插值，
    // 否则停稳那一帧会从 0 直接跳到 1，小标题闪一下。
    val collapsedTitleAlpha = remember(listState, autoHideTitle, multiSelect, pagerState, pagerStates, pagerHeaderScroll) {
        derivedStateOf {
            val total = headerHeightPx.coerceAtLeast(1).toFloat()
            fun progressOf(state: LazyListState?): Float {
                if (state == null) return 0f
                val scrolled = if (state.firstVisibleItemIndex > 0) {
                    total
                } else {
                    state.firstVisibleItemScrollOffset.toFloat()
                }
                return (scrolled / (total * 0.7f)).coerceIn(0f, 1f)
            }
            when {
                !autoHideTitle || multiSelect -> 1f
                pagerHeaderScroll != null ->
                    (pagerHeaderScroll.value / (total * 0.8f)).coerceIn(0f, 1f)
                else -> progressOf(listState)
            }
        }
    }

    // 表头滚到快贴住顶栏时淡出。顶栏是半透明玻璃，如果让 Tab 胶囊一直往上穿，
    // 就会跟小标题叠成一团（之前那张截图）。这里只用最后三成行程做淡化：
    // 表头依旧是跟着列表连续往上卷的，没有一个刀切式的边界。
    val pagerHeaderAlpha = remember(pagerHeaderScroll) {
        if (pagerHeaderScroll == null) {
            null
        } else {
            derivedStateOf {
                val total = headerHeightPx.coerceAtLeast(1).toFloat()
                val t = (pagerHeaderScroll.value / total - 0.62f) / 0.24f
                (1f - t).coerceIn(0f, 1f)
            }
        }
    }

    // 多选时标题换成「已选择 N 项」；切换用淡出 → 换字 → 淡入，而不是硬切。
    val barTitle = if (multiSelect && selectionCount > 0) {
        "已选择 $selectionCount 项"
    } else {
        title
    }
    var shownTitle by remember { mutableStateOf(barTitle) }
    val titleSwap = remember { Animatable(1f) }
    var prevSelecting by remember { mutableStateOf(multiSelect) }
    LaunchedEffect(barTitle, multiSelect) {
        val selectionChanged = multiSelect != prevSelecting
        prevSelecting = multiSelect
        if (shownTitle == barTitle) return@LaunchedEffect
        if (selectionChanged && multiSelect) {
            // 进入多选：collapsedTitleAlpha 此时才放开显示，从 0 淡入，
            // 避免「闪一下 → 淡出 → 再淡入」。
            titleSwap.snapTo(0f)
            shownTitle = barTitle
            titleSwap.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        } else if (selectionChanged) {
            titleSwap.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
            shownTitle = barTitle
            titleSwap.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        } else {
            shownTitle = barTitle
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (WallpaperManager.state.collectAsState().value.hasImage) {
            Color.Transparent
        } else {
            MiuixTheme.colorScheme.surface
        },
        topBar = {
            GlassTopAppBar(
                title = shownTitle,
                largeTitle = shownTitle,
                subtitle = subtitle.orEmpty(),
                expandable = !compactTitle,
                backdrop = backdrop,
                progressiveBlur = advancedMaterial,
                scrollBehavior = scrollBehavior,
                titleAlpha = { collapsedTitleAlpha.value * titleSwap.value },
                navigationIcon = {
                    // 多选时左上角 X 覆盖返回键位置；否则显示返回箭头。
                    androidx.compose.animation.AnimatedContent(
                        targetState = multiSelect,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(120))
                        },
                        label = "detailNavIcon",
                    ) { selecting ->
                        GlassIconButton(
                            onClick = {
                                if (selecting && onExitMultiSelect != null) onExitMultiSelect() else onBack()
                            },
                        ) {
                            Icon(
                                imageVector = if (selecting) MiuixIcons.Close else MiuixIcons.Glass.ChevronBackward,
                                contentDescription = if (selecting) "退出多选" else "返回",
                                modifier = Modifier.size(if (selecting) 22.dp else 25.dp).offset(x = 1.5.dp, y = 0.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                actions = {
                    // 固定 44dp 容器并居中：切换多选/普通状态时，动作按钮的
                    // 触控区与图标中心都保持在同一个位置，不会因为内容变化而左右移动。
                    val hasAction = endAction != null
                    AnimatedContent(
                        targetState = endActionKey ?: title,
                        modifier = if (hasAction) Modifier.width(44.dp) else Modifier,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(190)) togetherWith fadeOut(animationSpec = tween(140))
                        },
                        contentAlignment = Alignment.Center,
                        label = "detailEndAction",
                    ) { endAction?.invoke() }
                },
            )
        },
    ) { contentPadding ->
        val topBarHeightDp = contentPadding.calculateTopPadding()
        CompositionLocalProvider(LocalActiveBackdrop provides (activeBackdrop ?: wallpaperBackdrop)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 内容层（= 顶栏采样对象）。顶栏在 Scaffold 的 topBar，
                    // 不在这个 Box 里，所以这里是「兄弟采样」，不会自引用。
                    .layerBackdrop(backdrop),
            ) {
                // 不透明底色：换壁纸时壁纸是淡入的，页面若全透明，过渡期间会透出
                // NavDisplay 下面那一页（上一页的列表/头图），看起来像「透图」。
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MiuixTheme.colorScheme.surface),
                )
                // 壁纸层：只录壁纸。嵌在内容层内部，内容层因此录得到壁纸，
                // 顶栏 progressiveBlur 才有可模糊的内容；壁纸层自身不含采样者。
                // 关于页（hideWallpaper）整层不画：那里由流光自己铺满不透明底色。
                if (!hideWallpaper) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(wallpaperBackdrop),
                    ) {
                        WallpaperBackground()
                    }
                }
                Box(Modifier.matchParentSize()) { background() }
            val listBody: @Composable () -> Unit = {
                val pg = pagerState
                val pages = pagerList
                if (pg != null && pages != null) {
                    // 一整条连续滚动：表头不是画在另一块画布上，而是列表里第一个
                    // 真实的占位 item，跟歌曲在同一条滚动线上，1:1 一起走。
                    // 表头只是「照着这条滚动线的偏移再画一遍」的覆盖层：
                    //  - 竖向：位移读的就是占位 item 的布局偏移（pagerHeaderScroll），
                    //    没有第二个坐标系，所以不会有停住、跳变、刀切；
                    //  - 横向：它在 pager 外面，左右滑只换 Tab 下面的列表，表头原地不动；
                    //  - 手势：这一层不挂任何 pointerInput，所以从表头上起手上下滑
                    //    直接落到下面的列表；左右滑落到 pager 的识别区（内容区）。
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pg,
                            modifier = Modifier.fillMaxSize(),
                            // 两页都预组合：滑动时列表已经画好，不会滑到一半才出现内容。
                            beyondViewportPageCount = 1,
                        ) { page ->
                            val pageState = pagerStates.getOrNull(page) ?: listState
                            LazyColumn(
                                state = pageState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .overScrollVertical(onOffsetChanged = { offset -> scrollBehavior.state.overscrollOffset = offset })
                                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                                    .scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove),
                                contentPadding = PaddingValues(
                                    bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                                        .asPaddingValues().calculateBottomPadding() + DetailBottomReserve,
                                ),
                            ) {
                                // 顶栏让位 + 表头高度，一块真实占位。它跟歌曲在同一个
                                // LazyColumn 里，所以上滑时是一整屏内容连续往上走。
                                item(key = "detail-header-space") {
                                    Spacer(Modifier.height(topBarHeightDp + headerHeightDp))
                                }
                                pages(this, page)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = topBarHeightDp)
                                .graphicsLayer {
                                    translationY = -(pagerHeaderScroll?.value ?: 0f)
                                    alpha = pagerHeaderAlpha?.value ?: 1f
                                },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { headerHeightPx = it.height },
                            ) {
                                header()
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical(onOffsetChanged = { offset -> scrollBehavior.state.overscrollOffset = offset })
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
                }
            }

            val refresh = pullRefresh
            if (refresh != null) {
                // 刷新头按 contentPadding.top 下移，正好出现在顶栏下方，
                // 而不是从屏幕最顶端（状态栏）往下拉——与 miuix demo 的用法一致。
                PullToRefresh(
                    isRefreshing = refresh.isRefreshing,
                    onRefresh = refresh.onRefresh,
                    contentPadding = PaddingValues(top = topBarHeightDp),
                    refreshTexts = refresh.texts,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(Modifier.fillMaxSize().then(listModifier)) {
                        listBody()
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().then(listModifier)) {
                    listBody()
                }
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
            GlassIconButton(
                onClick = { toggleSelectAll() },
                // 顶栏 actions 的右内边距是 12dp，全选框比页面右上角的「+」略靠里；
                // 右移 4dp 与其他顶栏按钮的视觉位置对齐。
                modifier = Modifier.offset(x = 4.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.SelectAll,
                    contentDescription = if (allSelected) "取消全选" else "全选",
                    modifier = Modifier.size(22.dp),
                    tint = (if (allSelected) scheme.primary else Color.Unspecified).takeOrElse { MiuixTheme.colorScheme.onSurface },
                )
            }
        }
    } else exportAction

    DetailPage(
        title = if (multiSelect) "已选择 ${selectedIds.size} 项" else playlist?.name ?: "歌单",
        onBack = onBack,
        backdrop = backdrop,
        compactTitle = true,
        autoHideTitle = true,
        listState = listState,
        endAction = topEndAction,
        endActionKey = multiSelect,
        multiSelect = multiSelect,
        selectionCount = selectedIds.size,
        onExitMultiSelect = { exitEdit() },
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
                            imageVector = MiuixIcons.Glass.Item,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LibraryEmptyState(
                        icon = MiuixIcons.Glass.Music1,
                        text = "歌单还没有歌曲",
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
                        placementSpec = dragState.placementSpec(),
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
        backdrop = rememberPopupBackdrop(),
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
        autoHideTitle = true,
        multiSelect = multiSelect,
        selectionCount = selectedIds.size,
        onExitMultiSelect = { selectedIds = emptySet() },
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LibraryEmptyState(
                        icon = MiuixIcons.FavoritesFill,
                        text = "暂无收藏的歌曲",
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
                            placementSpec = dragState.placementSpec(),
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
        backdrop = rememberPopupBackdrop(),
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
    // 歌曲 / 专辑各一页：点 chip 与左右横滑共用同一个 pager，切 Tab 是整页横移，
    // 不再重播「淡入 + 上移」的入场动画。
    val pagerState = rememberPagerState(pageCount = { 2 })
    val songListState = rememberLazyListState()
    val albumListState = rememberLazyListState()
    val pageStates = remember(songListState, albumListState) {
        listOf(songListState, albumListState)
    }

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(loading) {
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
        autoHideTitle = true,
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
                    TabChip("歌曲", selected = pagerState.currentPage == 0) {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }
                    TabChip("专辑", selected = pagerState.currentPage == 1) {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    }
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
        listState = pageStates[pagerState.currentPage],
        pagerState = pagerState,
        pagerStates = pageStates,
        pagerList = { tabPage ->
            if (!loading) when {
                tabPage == 0 -> {
                if (songs.isEmpty()) item {
                    val a = itemAlpha(0)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = a
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
                        },
                    )
                }
            }
            }
        },
    )
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
        autoHideTitle = true,
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
