package com.hyper.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyper.music.music.LocalSongs
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.Song
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.glass.GlassTabRow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.hyper.music.ui.components.EmptyHint
import com.hyper.music.ui.components.RefreshIndicator
import com.hyper.music.ui.components.SongPairRow
import com.hyper.music.ui.components.TrackRow
import com.hyper.music.ui.basic.chipBounce
import com.hyper.music.ui.basic.rememberChipBounce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.squircle.squircleClip
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.hyper.music.music.SearchHistoryStore
import com.hyper.music.ui.utils.contentFraction
import com.hyper.music.ui.utils.songColumns
import top.yukonga.miuix.kmp.utils.overScrollVertical
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassAlpha
import com.hyper.music.ui.effects.miuix.cardGlassColors
import com.hyper.music.ui.effects.miuix.rememberSquircleShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.icon.glass.OverseasMusic
import top.yukonga.miuix.kmp.icon.glass.HealthyPhoneUsage
import top.yukonga.miuix.kmp.icon.glass.FileDownloads

internal val SectionGap = 12.dp

private val PlayBarTopReserve = 133.dp

@Composable
fun ExploreScreen(
    onPlaySongs: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    active: Boolean = true,
    scrollBehavior: ScrollBehavior? = null,
) {
    val context = LocalContext.current
    val scheme = MiuixTheme.colorScheme
    val history by MusicLibrary.history.collectAsState()
    val playlists by MusicLibrary.playlists.collectAsState()
    val favorites by MusicLibrary.favorites.collectAsState()

    val localSongs by produceState(initialValue = emptyList<Song>(), context) {
        value = withContext(Dispatchers.IO) { LocalSongs.scan(context) }
    }

    // 推荐列表用 remember 缓存：仅在历史/歌单变化时重算，避免组合期写全局可变状态、
    // 也避免每次重组都 shuffled() 造成列表抖动与 LaunchedEffect 反复重启。
    val recommendSongs: List<Song> = remember(history, playlists) {
        val pool = (history + playlists.flatMap { it.songs }).distinctBy { it.id }
        if (pool.isEmpty()) emptyList() else pool.shuffled().take(10)
    }

    LaunchedEffect(recommendSongs) {
        recommendSongs.forEach { s ->
            val c = s.cover
            if (c.isNotBlank()) CoverLoader.pin(c, CoverSize.MEDIUM.decode)
        }
    }

    val columns = songColumns()
    val fraction = contentFraction(columns)
    // 标题与卡片刻意用**两个不同**的边距：
    //   标题 titlePad = 26.dp  —— 与 GlassTopAppBar 的大标题（TopAppBarDefaults.TitlePadding）左对齐；
    //   卡片 cardPad  = 20.dp  —— 与全局其他页面的 Card 边距一致。
    // 推荐卡片的 LazyRow 自己撑满屏宽，用 contentPadding 做内缩，
    // 这样才能在横滑时让内容一直滑到屏幕边缘、而不是被 padding 提前裁掉。
    val titlePad = 26.dp
    val cardPad = 20.dp
    // 与「猜你想听」推荐卡（squircleClip(20.dp)）同一条 squircle 轮廓：
    // Card 自身用 squircleSurface 画底，玻璃层必须用同一个 Shape，圆角才不会被
    // 圆角矩形叠出双层弧线。
    val cardShape = rememberSquircleShape(20.dp)
    val cardColors = cardGlassColors()

    Box(
        modifier = modifier
            .fillMaxSize()
            // 把overscroll 反馈给 topbar：短内容页无法产生真实滚动时，靠 overscrollOffset 保持模糊渐变。
            .overScrollVertical(onOffsetChanged = { offset -> scrollBehavior?.state?.overscrollOffset = offset })
            .then(scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = topInset + 4.dp,
                    bottom = PlayBarTopReserve + 24.dp,
                ),
        ) {
            // 猜你想听：有推荐时直接展示可滚动歌曲卡片，不套白色 Card 遮罩。
            SectionHeading(text = "猜你想听", horizontalPadding = titlePad)
            // 空占位卡 ↔ 歌曲横滑列表：交叉淡入淡出，容器高度平滑变化，
            // 下方的「我喜欢的歌曲」等区块因此随内容高度自然下移。
            AnimatedContent(
                targetState = recommendSongs.isEmpty(),
                transitionSpec = {
                    (
                        fadeIn(animationSpec = tween(260)) + slideInVertically(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                        ) { h -> h / 10 }
                        ).togetherWith(
                        fadeOut(animationSpec = tween(170)),
                    ).using(
                        SizeTransform(clip = false) { _, _ ->
                            tween(300, easing = FastOutSlowInEasing)
                        },
                    )
                },
                label = "recommendSection",
            ) { empty ->
                if (empty) {
                    Card(
                        cornerRadius = 20.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = cardPad)
                            .cardGlass(cardShape),
                        insideMargin = PaddingValues(16.dp),
                        colors = cardColors,
                    ) {
                        ExploreEmptyCard(
                            icon = MiuixIcons.Glass.OverseasMusic,
                            title = "还没有推荐歌曲",
                            subtitle = "多听几首歌，我们会为你生成专属推荐",
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = cardPad),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(recommendSongs, key = { _, s -> s.id }) { index, song ->
                            RecommendCard(
                                song = song,
                                // 新增卡片（1→2、2→3…）以淡入出现，已有卡片不动；
                                // 首次加载整排列表时不播放（Compose 只在后续插入时触发）。
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(320, easing = FastOutSlowInEasing),
                                    fadeOutSpec = tween(200, easing = FastOutSlowInEasing),
                                ),
                            ) { onPlaySongs(recommendSongs, index) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 我喜欢的歌曲
            SectionHeading(text = "我喜欢的歌曲", horizontalPadding = titlePad)
            Card(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardPad)
                    .cardGlass(cardShape),
                // 底部内边距归零：留白交给「查看收藏」行自己撑，
                // 这样文字到横线、到卡片下沿的距离才能严格相等。
                insideMargin = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp),
                colors = cardColors,
            ) {
                FavoritesCard(
                    hasSongs = favorites.isNotEmpty(),
                    count = favorites.size,
                    covers = favorites.take(3).mapNotNull { it.cover },
                    onOpen = onOpenFavorites,
                )
            }

            Spacer(Modifier.height(20.dp))

            // 本地歌曲：右侧不显示专辑封面（cover = null）。
            SectionHeading(text = "本地歌曲", horizontalPadding = titlePad)
            Card(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardPad)
                    .cardGlass(cardShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenDownloads,
                    ),
                insideMargin = PaddingValues(16.dp),
                colors = cardColors,
            ) {
                if (localSongs.isEmpty()) {
                    ExploreEmptyCard(
                        icon = MiuixIcons.Glass.FileDownloads,
                        title = "还没有本地歌曲",
                        subtitle = "将歌曲下载到本地，随时离线畅听",
                    )
                } else {
                    ExploreFilledCard(
                        icon = MiuixIcons.Glass.FileDownloads,
                        title = "共${localSongs.size}首本地歌曲",
                        subtitle = "点击管理本地歌曲",
                        cover = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    text: String,
    horizontalPadding: Dp = 0.dp,
) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = horizontalPadding, end = horizontalPadding, top = 4.dp, bottom = 12.dp),
    )
}

@Composable
private fun ExploreEmptyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.disabledOnSecondaryVariant),
        )
        Spacer(Modifier.height(14.dp))
        Text(text = title, color = MiuixTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text = subtitle, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = actionText,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAction,
                ),
            )
        }
    }
}

@Composable
private fun ExploreFilledCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    cover: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Image(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.disabledOnSecondaryVariant),
            )
            Spacer(Modifier.height(14.dp))
            Text(text = title, color = MiuixTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(text = subtitle, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
        }
        if (!cover.isNullOrBlank()) {
            NetCover(
                url = cover,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                corner = 12.dp,
                maxSize = CoverSize.MEDIUM.decode,
            )
        }
    }
}

/**
 * 我喜欢的歌曲卡片。
 *
 * 点击只在底部「查看收藏」那一行生效；没有收藏时该行常驻但置灰不可点。
 * 右下角的堆叠封面按参考图画：三张向右下错开，最前面一张最大。
 */
@Composable
private fun FavoritesCard(
    hasSongs: Boolean,
    count: Int,
    covers: List<String>,
    onOpen: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Image(
                    imageVector = MiuixIcons.Glass.HealthyPhoneUsage,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    colorFilter = ColorFilter.tint(scheme.disabledOnSecondaryVariant),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (hasSongs) "共${count}首收藏歌曲" else "还没有喜欢的歌曲",
                    color = scheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (hasSongs) "点击管理收藏的歌曲" else "快收藏几首喜欢的歌曲吧",
                    color = scheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                )
            }
            if (covers.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                FavoritesCoverStack(covers)
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(scheme.onSurface.copy(alpha = 0.12f)),
        )
        // 常驻的「查看收藏」行：高度固定，文字落在横线与卡片下沿的正中。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clickable(
                    enabled = hasSongs,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpen,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "查看收藏",
                // 分组标题 18sp Bold 下移 2px → 16sp，行高 46dp 与垂直居中不动。
                color = if (hasSongs) scheme.primary else scheme.onSurfaceVariantSummary.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * 封面向左依次减小：最左最大、最右最小，竖直方向统一居中（上下对称）。
 *
 * 容器宽度按实际张数算（1 张 64dp、2 张 86dp、3 张 104dp），
 * 整组始终贴右边，收藏只剩一两首时也不会缩到左边留一块空白。
 */
@Composable
private fun FavoritesCoverStack(covers: List<String>) {
    if (covers.isEmpty()) return
    // 每张的宽、高，以及相对「前一张右缘」露出的宽度（第一张无需露出）。
    val specs = listOf(
        Triple(64.dp, 84.dp, 0.dp),
        Triple(53.dp, 70.dp, 22.dp),
        Triple(43.dp, 57.dp, 18.dp),
    ).take(covers.size)

    val offsets = specs.indices.map { i ->
        var x = 0.dp
        for (k in 1..i) x += specs[k - 1].first - specs[k].first + specs[k].third
        x
    }
    val totalWidth = offsets.last() + specs.last().first

    Box(modifier = Modifier.width(totalWidth).height(84.dp)) {
        // 从右往左绘制，保证最大的那张压在最上层。
        for (i in specs.indices.reversed()) {
            val (w, h, _) = specs[i]
            StackCover(
                url = covers.getOrNull(i),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = offsets[i])
                    .size(w, h),
            )
        }
    }
}

@Composable
private fun StackCover(url: String?, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        if (!url.isNullOrBlank()) {
            NetCover(
                url = url,
                modifier = Modifier.fillMaxSize(),
                corner = 10.dp,
                maxSize = CoverSize.MEDIUM.decode,
            )
        }
    }
}

@Composable
internal fun SearchResults(
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
    val contentPadding = PaddingValues(
        // top 完全由调用方控制，便于与 GlassTabRow 保持精确 12dp 间距。
        top = topPadding,
        bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
            .asPaddingValues().calculateBottomPadding() + PlayBarTopReserve + SectionGap,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp),
    ) {
        when (category) {
            0 -> SongResultList(
                songs = songs,
                loading = loading,
                listState = listState,
                contentPadding = contentPadding,
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
                    contentPadding = contentPadding,
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
                    contentPadding = contentPadding,
                ) {
                    items(albums, key = { it.id }) { al ->
                        AlbumResultRow(album = al, onClick = { onOpenAlbum(al.name, al.artist) })
                    }
                }
            }
        }
    }
}

@Composable
internal fun SongResultList(
    songs: List<Song>,
    loading: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    contentPadding: PaddingValues,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onLoadMore: () -> Unit,
    loadingMore: Boolean,
) {
    val columns = songColumns()
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
            contentPadding = contentPadding,
        ) {
            itemsIndexed(songs.chunked(columns), key = { _, group -> group.first().id }) { groupIndex, group ->
                SongPairRow(
                    pair = group,
                    startIndex = groupIndex * columns,
                    columns = columns,
                ) { index, song ->
                    TrackRow(
                        song = song,
                        onClick = { onPlaySongs(songs, index) },
                        onMore = { onMore(song) },
                        contentPadding = PaddingValues(
                            horizontal = if (columns == 2) 8.dp else 0.dp,
                            vertical = 8.dp,
                        ),
                        moreAlignEnd = true,
                    )
                }
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
internal fun ArtistResultRow(artist: SearchArtist, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 0.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetCover(
            url = artist.avatar.ifBlank { null },
            modifier = Modifier.size(48.dp),
            corner = 100.dp,
            maxSize = CoverSize.SMALL.decode,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = artist.name,
            color = scheme.onSurface,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun AlbumResultRow(album: SearchAlbum, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 0.dp, vertical = 8.dp),
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
private fun RecommendCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val bounce = rememberChipBounce(interactionSource)
    val highlightAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.12f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "recommendCardHighlight",
    )
    Box(
        modifier = modifier
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
internal fun SearchHistorySection(
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
    val columns = songColumns()
    val fraction = contentFraction(columns)
    val sidePad = if (columns == 2) 0.dp else 28.dp
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding + 8.dp, bottom = 140.dp),
    ) {
        Text(
            text = "搜索历史",
            color = scheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = sidePad, end = sidePad, bottom = 16.dp),
        )
        if (history.isEmpty()) {
            Text(
                text = "暂无搜索历史",
                color = scheme.onSurfaceSecondary.copy(alpha = 0.75f),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = sidePad),
            )
        } else {

            Box(modifier = Modifier.graphicsLayer { alpha = clearAlpha.value }) {
            Column {
            FlowRow(
                modifier = Modifier.padding(start = sidePad, end = if (columns == 2) sidePad else 18.dp),
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
}

@Composable
internal fun HistoryChip(
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
