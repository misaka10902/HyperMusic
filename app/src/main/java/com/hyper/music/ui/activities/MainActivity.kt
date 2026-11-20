package com.hyper.music.ui.activities
import androidx.compose.ui.graphics.takeOrElse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.blur.LayerBackdrop

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.hyper.music.music.AudioQuality
import com.hyper.music.music.Downloader
import com.hyper.music.music.GdApi
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.PlaybackController
import com.hyper.music.music.PlaybackService
import com.hyper.music.music.Playlist
import com.hyper.music.music.PlaylistImporter
import com.hyper.music.music.Song
import com.hyper.music.music.WallpaperBackground
import com.hyper.music.music.WallpaperManager
import com.hyper.music.music.CoverLoader
import com.hyper.music.music.LocalSongs
import com.hyper.music.music.MediaCache
import com.hyper.music.ui.components.ArtistPickerSheet
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.components.HyperSheet
import com.hyper.music.ui.components.PlaybackQueueSheet
import com.hyper.music.ui.components.PlaylistPickerSheet
import com.hyper.music.ui.components.PlaylistAddMenuPopup
import com.hyper.music.ui.components.PlaylistJsonImportDialog
import com.hyper.music.ui.components.PlaylistLinkImportDialog
import com.hyper.music.ui.components.HyperBottomBar
import com.hyper.music.ui.components.SelectionActionBar
import com.hyper.music.ui.components.SelectionBarState
import com.hyper.music.ui.components.SleepTimerSheet
import com.hyper.music.ui.components.TrackMoreSheet
import com.hyper.music.ui.effects.miuix.LocalActiveBackdrop
import com.hyper.music.ui.effects.miuix.LocalPageBackdrop
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.effects.miuix.rememberAdvancedMaterialEnabled
import com.hyper.music.ui.player.BottomBarAreaHeight
import com.hyper.music.ui.player.HyperMiniPlayer
import com.hyper.music.ui.player.HyperPlayerLayer
import com.hyper.music.ui.player.MiniPlayerHeight
import com.hyper.music.ui.player.MiniPlayerWidthFraction
import com.hyper.music.ui.player.MiniPlayerToNavBarSpacing
import com.hyper.music.ui.player.rememberHyperPlayerState
import com.hyper.music.ui.screens.AlbumScreen
import com.hyper.music.ui.screens.AboutAppScreen
import com.hyper.music.ui.screens.ArtistScreen
import com.hyper.music.ui.screens.DownloadsScreen
import com.hyper.music.ui.screens.ExploreScreen
import com.hyper.music.ui.screens.FavoritesScreen
import com.hyper.music.ui.screens.HistoryScreen
import com.hyper.music.ui.screens.HyperSearchScreen
import com.hyper.music.ui.screens.PlaylistDetailScreen
import com.hyper.music.ui.search.GlassSearchOverlay
import com.hyper.music.ui.search.rememberGlassSearchBarAlpha
import com.hyper.music.ui.search.glassSearchBarTransition
import com.hyper.music.ui.screens.PlaylistScreen
import com.hyper.music.ui.screens.PlayerSettingsScreen
import com.hyper.music.ui.screens.SettingsScreen
import com.hyper.music.ui.screens.WallpaperScreen
import com.hyper.music.ui.theme.CourseHyperTheme
import com.hyper.music.ui.utils.BottomFloatingHeight
import com.hyper.music.ui.utils.BottomFloatingWidthFraction
import com.hyper.music.ui.utils.applyThemeAwareSystemBars
import com.hyper.music.ui.utils.bottomContentFade
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.NativeTextField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.springAnimateToPage
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.core.NavKey
import com.hyper.music.ui.components.SheetCard
import com.hyper.music.ui.components.SheetHeader
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

private val TAB_TITLES = listOf("探索", "历史", "歌单", "设置")

/** bottomsheet 收起动画的大致时长；跳二级页前先等它走完，避免三层叠加。 */


private sealed interface MusicRoute : NavKey {
    val navId: Long
    data class Artist(val name: String, override val navId: Long = 0L) : MusicRoute
    data class Album(val name: String, val artist: String, override val navId: Long = 0L) : MusicRoute
    data class PlaylistDetail(val playlistId: String, override val navId: Long = 0L) : MusicRoute
    data class Favorites(override val navId: Long = 0L) : MusicRoute
    data class Wallpaper(override val navId: Long = 0L) : MusicRoute
    data class PlayerSettings(override val navId: Long = 0L) : MusicRoute
    data class Downloads(override val navId: Long = 0L) : MusicRoute
    data class AboutApp(override val navId: Long = 0L) : MusicRoute
}

private data object RootRoute : NavKey

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        applyThemeAwareSystemBars()
        WallpaperManager.init(this)
        setContent {
            CourseHyperTheme {
                HyperApp()
            }
        }
    }

    override fun onStop() {
        PlaybackController.saveSession()
        super.onStop()
    }

    override fun onDestroy() {

        CoverLoader.clearPinned()
        super.onDestroy()
    }
}

@Composable
fun HyperApp() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    // 主 Tab 横滑：pager 与 selectedTab 双向同步，点击导航与横滑共用 miuix 官方弹簧动画。
    //
    // 这里对齐 miuix demo 的 MainPagerState 语义：**点击时立即把 selectedTab 设为目标值**，
    // 让导航条的指示器弹簧立刻从当前位置跑起来；随后用 isNavigating 屏蔽
    // pager.currentPage 在动画途中反复取整造成的回写。
    // 之前只在 LaunchedEffect(currentPage) 里同步，而 currentPage 是取整值，弹簧跑过一半
    // 才跳一次，滑块于是「先卡住、再突然弹过去」，看起来就是一顿一顿。
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec,
    )
    var navJob by remember { mutableStateOf<Job?>(null) }
    var isNavigating by remember { mutableStateOf(false) }
    // 手指横滑 / 惯性停靠时（非点击导航），才把 pager 的取整页号回写给 selectedTab。
    LaunchedEffect(pagerState.currentPage, isNavigating) {
        if (!isNavigating && selectedTab != pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }
    }
    val selectTab: (Int) -> Unit = { index ->
        if (index != selectedTab) {
            navJob?.cancel()
            selectedTab = index
            isNavigating = true
            navJob = scope.launch {
                val myJob = coroutineContext.job
                try {
                    pagerState.springAnimateToPage(index)
                } finally {
                    if (navJob == myJob) {
                        isNavigating = false
                        if (pagerState.currentPage != index) selectedTab = pagerState.currentPage
                    }
                }
            }
        }
    }
    // 全屏玻璃搜索：单一布尔状态驱动，enter/exit 转场由 GlassSearchOverlay 与下方联动负责。
    var searchExpanded by remember { mutableStateOf(false) }
    val searchBarAlpha = rememberGlassSearchBarAlpha(searchExpanded)
    val scrollBehavior = MiuixScrollBehavior()
    val exploreScrollBehavior = MiuixScrollBehavior()
    val historyScrollBehavior = MiuixScrollBehavior()
    val playlistScrollBehavior = MiuixScrollBehavior()

    val playerState = rememberHyperPlayerState()

    val navBackStack = remember { navBackStackOf(RootRoute) }
    var routeSeq by remember { mutableStateOf(0L) }
    val route = navBackStack.lastOrNull() as? MusicRoute
    fun pushRoute(target: MusicRoute) {
        val stamped = when (target) {
            is MusicRoute.Artist -> target.copy(navId = ++routeSeq)
            is MusicRoute.Album -> target.copy(navId = ++routeSeq)
            is MusicRoute.PlaylistDetail -> target.copy(navId = ++routeSeq)
            is MusicRoute.Favorites -> target.copy(navId = ++routeSeq)
            is MusicRoute.Wallpaper -> target.copy(navId = ++routeSeq)
            is MusicRoute.PlayerSettings -> target.copy(navId = ++routeSeq)
            is MusicRoute.Downloads -> target.copy(navId = ++routeSeq)
            is MusicRoute.AboutApp -> target.copy(navId = ++routeSeq)
        }
        navBackStack.add(stamped)
    }
    fun popRoute() { if (navBackStack.size > 1) navBackStack.removeLastOrNull() }

    val miniPlayerShift by animateDpAsState(
        targetValue = if (route == null && !searchExpanded) 0.dp else MiniPlayerHeight + MiniPlayerToNavBarSpacing,
        animationSpec = tween(durationMillis = 300),
        label = "miniPlayerShift",
    )

    var moreSong by remember { mutableStateOf<Song?>(null) }
    var moreDelete by remember { mutableStateOf<((Song) -> Unit)?>(null) }
    var moreDeleteLabel by remember { mutableStateOf("删除歌曲") }
    var moreFromFavorites by remember { mutableStateOf(false) }
    var artistPickerSong by remember { mutableStateOf<Song?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createForSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var selectionBar by remember { mutableStateOf<SelectionBarState?>(null) }
    val selectionActive = selectionBar != null
    fun exitSelection() {
        selectionBar?.onExit?.invoke()
        selectionBar = null
    }
    var pickerSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var exportJson by remember { mutableStateOf<String?>(null) }

    // 「添加歌单」：右上角 + 号弹出菜单（链接 / JSON / 手动创建）。
    var addMenuExpanded by remember { mutableStateOf(false) }
    var addMenuAnchor by remember { mutableStateOf(Rect.Zero) }
    var showLinkImport by remember { mutableStateOf(false) }
    var linkImportBusy by remember { mutableStateOf(false) }
    var linkImportStatus by remember { mutableStateOf<String?>(null) }
    var linkImportError by remember { mutableStateOf<String?>(null) }
    var linkImportJob by remember { mutableStateOf<Job?>(null) }
    var showJsonImport by remember { mutableStateOf(false) }
    var jsonImportBusy by remember { mutableStateOf(false) }
    var jsonImportStatus by remember { mutableStateOf<String?>(null) }
    var jsonImportError by remember { mutableStateOf<String?>(null) }
    var jsonImportJob by remember { mutableStateOf<Job?>(null) }

    var confirmDeletePlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var confirmDeleteHistory by remember { mutableStateOf<Song?>(null) }
    var confirmClearHistory by remember { mutableStateOf(false) }

    var downloadDeleteConfirm by remember { mutableStateOf<List<Song>?>(null) }
    var pendingDirectDelete by remember { mutableStateOf(emptyList<Song>()) }
    var pendingMediaDelete by remember { mutableStateOf(emptyList<Song>()) }
    var downloadsRefreshKey by remember { mutableIntStateOf(0) }

    val rootView = LocalView.current
    val focusManager = LocalFocusManager.current
    val imm = remember {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    }

    fun hideKeyboard() {
        focusManager.clearFocus()
        rootView.clearFocus()
        imm?.hideSoftInputFromWindow(rootView.windowToken, 0)
    }

    val overlayVisible = route != null || moreSong != null || pickerSongs.isNotEmpty() ||
        showCreateDialog || renameTarget != null || showQualitySheet ||
        showQueueSheet || showSleepTimer || confirmDeletePlaylists.isNotEmpty() ||
        confirmDeleteHistory != null || downloadDeleteConfirm != null ||
        showLinkImport || showJsonImport || addMenuExpanded

    LaunchedEffect(selectedTab, overlayVisible) {
        if (selectedTab != 0 || overlayVisible) hideKeyboard()
    }

    DisposableEffect(context) {
        val owner = context as? androidx.lifecycle.LifecycleOwner
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && playerState.isExpanded) {
                hideKeyboard()
            }
        }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }

    fun openMore(
        song: Song,
        delete: ((Song) -> Unit)?,
        label: String,
        fromFavorites: Boolean = false,
    ) {
        hideKeyboard()
        moreDelete = delete
        moreDeleteLabel = label
        moreFromFavorites = fromFavorites
        moreSong = song
    }

    fun finishDownloadDelete(mediaSongs: List<Song>, mediaOk: Boolean) {
        scope.launch {

            val directDeleted = withContext(Dispatchers.IO) {
                LocalSongs.deleteDirect(context, pendingDirectDelete)
            }
            val actuallyDeleted = directDeleted.toMutableList()
            if (mediaOk) actuallyDeleted.addAll(mediaSongs)
            val uris = actuallyDeleted.mapNotNull { it.localUri.ifBlank { null } }.toSet()
            if (uris.isNotEmpty()) {

                MusicLibrary.removeByLocalUri(uris)
                PlaybackController.removeByLocalUri(uris)
                MediaCache.removeByLocalUris(context, uris)
            }
            pendingDirectDelete = emptyList()
            pendingMediaDelete = emptyList()
            downloadDeleteConfirm = null
            downloadsRefreshKey++
            val total = actuallyDeleted.size
            Toast.makeText(
                context,
                if (total > 0) "已删除 $total 首歌曲" else "删除失败",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val delLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->

        val mediaOk = result.resultCode == Activity.RESULT_OK
        finishDownloadDelete(pendingMediaDelete, mediaOk)
    }

    fun startDownloadDelete(targets: List<Song>) {
        val plan = LocalSongs.deletePlan(targets)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && plan.mediaStoreSongs.isNotEmpty()) {

            pendingDirectDelete = plan.directSongs
            pendingMediaDelete = plan.mediaStoreSongs
            try {
                val req = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    plan.mediaStoreSongs.map { Uri.parse(it.localUri) },
                )
                delLauncher.launch(IntentSenderRequest.Builder(req.intentSender).build())
            } catch (_: Throwable) {

                pendingDirectDelete = plan.directSongs + plan.mediaStoreSongs
                pendingMediaDelete = emptyList()
                finishDownloadDelete(emptyList(), false)
            }
        } else {

            pendingDirectDelete = plan.directSongs + plan.mediaStoreSongs
            pendingMediaDelete = emptyList()
            finishDownloadDelete(emptyList(), false)
        }
    }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {  },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            runCatching { if (future.isDone) future.get().release() }
        }
    }

    // JSON 导入：选完文件直接接着跑，进度/报错都在对话框里显示。
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        showJsonImport = true
        jsonImportBusy = true
        jsonImportError = null
        jsonImportStatus = "读取文件中…"
        jsonImportJob = scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                } ?: throw IllegalStateException("无法读取该文件")
                val res = PlaylistImporter.importFromJson(text) { done, total ->
                    jsonImportStatus = "匹配网易云歌曲 $done/$total"
                }
                val playlist = MusicLibrary.createPlaylist(res.name)
                var added = 0
                res.songs.forEach { if (MusicLibrary.addSongToPlaylist(playlist.id, it)) added++ }
                Toast.makeText(context, "已添加歌单，共${added}首歌曲", Toast.LENGTH_LONG).show()
                jsonImportBusy = false
                jsonImportStatus = null
                showJsonImport = false
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                jsonImportBusy = false
                jsonImportStatus = null
                jsonImportError = e.message ?: "导入失败"
            }
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val payload = exportJson
        exportJson = null
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                } ?: error("无法写入文件")
            }.onSuccess {
                Toast.makeText(context, "歌单已导出", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "导出失败：${it.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val backgroundColor = MiuixTheme.colorScheme.surface
    val wallpaperActive by WallpaperManager.state.collectAsState()

    // 页面内容层：录制范围是 Scaffold 的内容区（壁纸 + 各页面列表），
    // 顶栏 / 底栏 / 迷你播放器 / 多选栏都在这个范围之外采样，因此不存在
    //「采样者处在自己采样的图层里」的自引用（自引用会让显示列表引用自身，
    // 渲染线程回放时无限递归，最终 native 崩溃，且拿不到 Java 堆栈）。
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    // 壁纸层：只录壁纸，永远不含任何玻璃组件。卡片毛玻璃（cardGlass）、弹层（BottomSheet / Dropdown）
    // 一律采样这一层，而不是采样页面自己所在的图层。
    // 必须补一层不透明底色：没有设置壁纸时这里若为空，采样方（尤其是弹层）会拿到全透明内容，
    // 高级材质就会表现为「面板直接透明、模糊消失」。
    val wallpaperBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    // 浮层（迷你播放器 / 底栏 / 多选栏）采样这一层：录制的是「当前整页的渲染结果」，
    // 包含页面转场与底部淡出，因此二级页面滚动、切页时模糊跟着变，不会变成静态采样。
    // 挂载点是 NavDisplay 外面的那个 Box，浮层都在它之外，故不会自引用。
    val navDisplayBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    val activeBackdrop: Backdrop = navDisplayBackdrop

    fun playList(songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        PlaybackController.playSongs(songs, index)
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    BackHandler(enabled = selectionActive) { exitSelection() }
    BackHandler(enabled = navBackStack.size > 1 && !selectionActive) { popRoute() }

    // 搜索展开时返回键关闭搜索（GlassSearchOverlay 内部另有一条 NavigationBackHandler，
    // 但项目禁用了预测式返回，故以这里为准）。
    BackHandler(enabled = searchExpanded && !selectionActive) {
        searchExpanded = false
    }

    val advancedMaterial = rememberAdvancedMaterialEnabled()

    // 当前可见页面的内容层，由各页面的 NavPage 发布（见下）。
    // popup / bottomSheet / dialog 走 LocalPageBackdrop，采样到的是「当前这一页」的
    // 真实内容；它们画在页面录制范围之外，不会自引用。
    val activePageLayer = remember { mutableStateOf<Backdrop?>(backdrop) }

    // 播放页自己的内容层。播放页上的队列 / 音质 / 定时 sheet 由 popupHost 承载，
    // 采样的是 LocalPageBackdrop；播放页展开时把这一层切成播放页自身，
    // 否则 sheet 会糊到当前 Tab 页（首页）的内容。
    val playerPageLayer = rememberLayerBackdrop { drawContent() }
    var pageLayerBeforePlayer by remember { mutableStateOf<Backdrop?>(null) }
    LaunchedEffect(playerState.isExpanded) {
        if (playerState.isExpanded) {
            pageLayerBeforePlayer = activePageLayer.value
            activePageLayer.value = playerPageLayer
        } else if (activePageLayer.value === playerPageLayer) {
            activePageLayer.value = pageLayerBeforePlayer ?: backdrop
        }
    }

    CompositionLocalProvider(
        LocalAdvancedMaterialEnabled provides advancedMaterial,
        LocalActiveBackdrop provides wallpaperBackdrop,
        LocalPageBackdrop provides activePageLayer,
    ) {
    @Composable
    fun NavPage(content: @Composable (LayerBackdrop) -> Unit) {
        // 每个二级页面一份独立的内容层（照 miuix demo 的 GlassPage）。
        // 层必须由「页面自己」挂到 Scaffold 的 content Box 上：
        // 顶栏在 topBar（content 之外），是这一层的兄弟，采样它才不会自引用；
        // 壁纸层同理挂在 content 内部、只录壁纸，卡片采样它也不会自引用。
        val pageSurface = MiuixTheme.colorScheme.surface
        val pageLayer = rememberLayerBackdrop {
            drawRect(pageSurface)
            drawContent()
        }
        DisposableEffect(pageLayer) {
            activePageLayer.value = pageLayer
            onDispose {
                // 二级页销毁后回到根页面（内容层就是 `backdrop`），
                // 之后从根页面弹弹层仍然采得到实时内容。
                if (activePageLayer.value === pageLayer) activePageLayer.value = backdrop
            }
        }
        content(pageLayer)
    }

    // 最外层 Scaffold：miuix 的 dialog / dropdown 都由 Scaffold 的 popupHost 承载，
    // 而 popup 是在 content（迷你播放器、导航条、多选栏）之后绘制的。套这一层，
    // 所有弹窗就稳定压在底部浮层之上，不需要手搓 zIndex 也不怕层级反了。
    // 无 topBar / bottomBar、insets 置零，布局与原来完全一致。
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { _ ->
    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {

        val noPredictiveBackOwner = remember {
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher =
                    NavigationEventDispatcher().also { it.isEnabled = false }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(navDisplayBackdrop),
        ) {
        // 根层壁纸：页面内的壁纸跟着页面转场/缩放，底部内容淡出（bottomContentFade）
        // 与页面转场时露出来的是这一层。没有它，淡出区域露出的就是 surface 底色，
        // 看起来就成了「一块白色遮罩」。
        // 放进采样层内部：navDisplayBackdrop 因此录到「壁纸 + 页面」，搜索展开、
        // 页面整体 alpha 归零之后，迷你播放器/底栏采样到的仍是壁纸（有细节可糊），
        // 不会退化成一块纯色。
        WallpaperBackground()
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides noPredictiveBackOwner) {
        NavDisplay(
            backStack = navBackStack,

            modifier = Modifier
                .fillMaxSize()
                // 搜索展开时整条页面栈一起淡出（不只是 Root 页）：
                // 从搜索结果直接进艺术家/专辑页时，新页不会从搜索层底下「透出来」。
                .graphicsLayer { alpha = searchBarAlpha.value }
                // 关于页的底部渐隐只作用在内容上（由页面内部的 listModifier 施加）：
                // 这里如果照常整页渐隐，会把流光背景一起淡掉、露出根层壁纸。
                .then(
                    if (route is MusicRoute.AboutApp) {
                        Modifier
                    } else {
                        Modifier.bottomContentFade(
                            startFromBottom = MiniPlayerToNavBarSpacing + BottomBarAreaHeight + 16.dp,
                            bottomAlpha = 0.12f,
                        )
                    }
                ),
            onBack = { popRoute() },
            transition = NavTransitions.MiuixDefault,
            effects = NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = rememberNavSystemCornerRadius(),
                dimAmount = 0.5f,
                backdropColor = if (wallpaperActive.hasImage) Color.Transparent else MiuixTheme.colorScheme.surface,
            ),
        ) {
            entry<RootRoute> {

    
    // 顶栏标题：进入/退出多选时淡出 → 换字 → 淡入；切 Tab 直接换，不额外加动画。
    val barTitle = if (selectionActive) {
        "已选择 ${selectionBar?.count ?: 0} 项"
    } else {
        TAB_TITLES[selectedTab]
    }
    var shownTitle by remember { mutableStateOf(barTitle) }
    val titleSwapAlpha = remember { Animatable(1f) }
    var prevSelection by remember { mutableStateOf(selectionActive) }
    LaunchedEffect(barTitle, selectionActive) {
        val selectionChanged = selectionActive != prevSelection
        prevSelection = selectionActive
        if (shownTitle == barTitle) return@LaunchedEffect
        if (selectionChanged) {
            titleSwapAlpha.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
            shownTitle = barTitle
            titleSwapAlpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        } else {
            // 计数变化 / 切 Tab：直接替换，不闪。
            shownTitle = barTitle
        }
    }

    // 退出多选时 selectionBar 会立刻置空，但顶栏图标的淡出动画还要再跑 140ms。
    // 这里留一份最后的状态快照，让淡出阶段仍能看到原来的按钮（而不是先变空再淡出）。
    var actionsSnapshot by remember { mutableStateOf<SelectionBarState?>(null) }
    LaunchedEffect(selectionBar) {
        if (selectionBar != null) actionsSnapshot = selectionBar
    }

    // 顶栏右上角动作：多选态放全选/批量动作，普通态按 Tab 放清空历史或「添加歌单」。
    // 只有进入/退出多选才做淡入淡出；切 Tab 时 targetState 不变，图标直接换、没有动画。
    val topActionsContent: (@Composable () -> Unit) = {
        AnimatedContent(
            targetState = selectionActive,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(200)) +
                        scaleIn(animationSpec = tween(220), initialScale = 0.9f)
                    ).togetherWith(fadeOut(animationSpec = tween(140)))
                    .using(SizeTransform(clip = false) { _, _ -> tween(200, easing = FastOutSlowInEasing) })
            },
            contentAlignment = Alignment.Center,
            label = "topActions",
        ) { selecting ->
            if (selecting) {
                // 进入时 selectionBar 已在；退出时它先被置空，取快照让淡出阶段仍有按钮。
                val bar = selectionBar ?: actionsSnapshot
                val extra = bar?.singleAction
                val selectAll = bar?.selectAllAction
                Row {
                    if (selectAll != null) {
                        GlassIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                selectAll.onClick()
                            },
                        ) {
                            Icon(
                                imageVector = selectAll.icon,
                                contentDescription = selectAll.label,
                                modifier = Modifier.size(22.dp),
                                tint = (selectAll.tint).takeOrElse { MiuixTheme.colorScheme.onSurface },
                            )
                        }
                        // 右侧还有按钮时才留间距；否则这 10dp 会把「全选」整体往左推，
                        // 与普通态的「+」对不齐。
                        if (extra != null) Spacer(Modifier.width(10.dp))
                    }
                    // 取消/退出多选已移至顶栏左上角；右上角仅保留 singleAction（如添加到歌单）。
                    if (extra != null) {
                        GlassIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                extra.onClick()
                            },
                        ) {
                            Icon(
                                imageVector = extra.icon,
                                contentDescription = extra.label,
                                modifier = Modifier.size(22.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    // 探索页右上角不再放置下载按钮（入口下沉到探索页"已下载的歌曲"卡片）。
                    0 -> {}

                    1 -> GlassIconButton(
                        onClick = {
                            if (MusicLibrary.history.value.isEmpty()) return@GlassIconButton
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            confirmClearHistory = true
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = "清空历史记录",
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }

                    2 -> GlassIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            addMenuExpanded = true
                        },
                        // 记录按钮位置，作为「添加歌单」菜单的锚点。
                        modifier = Modifier.onGloballyPositioned { addMenuAnchor = it.boundsInWindow() },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "添加歌单",
                            modifier = Modifier.size(23.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }


                Scaffold(
                    modifier = Modifier
                        .then(if (searchExpanded) Modifier.clearAndSetSemantics { } else Modifier),
                    containerColor = if (wallpaperActive.hasImage) Color.Transparent else MiuixTheme.colorScheme.surface,
                    topBar = {
                        val barScrollBehavior = when (selectedTab) {
                            0 -> exploreScrollBehavior
                            1 -> historyScrollBehavior
                            2 -> playlistScrollBehavior
                            else -> scrollBehavior
                        }
                        GlassTopAppBar(
                            title = shownTitle,
                            largeTitle = shownTitle,
                            subtitle = "",
                            backdrop = backdrop,
                            progressiveBlur = advancedMaterial,
                            scrollBehavior = barScrollBehavior,
                            titleAlpha = { titleSwapAlpha.value },
                            contentModifier = Modifier.glassSearchBarTransition(searchExpanded) { 1f },
                            navigationIcon = {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = selectionActive,
                                    enter = fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.8f),
                                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.85f),
                                ) {
                                    GlassIconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        exitSelection()
                                    }) {
                                        Icon(
                                            imageVector = MiuixIcons.Close,
                                            contentDescription = "取消多选",
                                            modifier = Modifier.size(22.dp),
                                            tint = MiuixTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            },
                            actions = { topActionsContent() },
                        )
                    },
                    bottomBar = {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MiniPlayerHeight + MiniPlayerToNavBarSpacing + BottomBarAreaHeight),
                        )
                    },
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    ) {
                        // 不透明底色：壁纸淡入期间页面不能是透明的，否则会透出下层页面。
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(backgroundColor),
                        )
                        // 壁纸层嵌在内容层里（与 demo 一致）：内容层因此录得到壁纸，
                        // 顶栏 progressiveBlur 采样内容层时才有东西可模糊；
                        // 而壁纸层自身只含壁纸，卡片采样它不会自引用。
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(wallpaperBackdrop),
                        ) {
                            WallpaperBackground()
                        }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            // 与 miuix demo 一致：停靠用官方的导航弹簧，而不是 Pager 默认的 tween，
                            // 手指松开到吸附这一段才有连续的减速感，不会「先停一下再弹过去」。
                            flingBehavior = pagerFlingBehavior,
                            // 4 页全部预组合，避免第一次滑到某页时才组合造成的掉帧。
                            beyondViewportPageCount = 3,
                        ) { tab ->
                            Box(modifier = Modifier.fillMaxSize()) {
                            when (tab) {
                            0 -> ExploreScreen(
                                onPlaySongs = { list, index -> playList(list, index) },
                                onMore = { song -> openMore(song, null, "删除歌曲") },
                                onOpenArtist = { name -> pushRoute(MusicRoute.Artist(name)) },
                                onOpenAlbum = { name, artist -> pushRoute(MusicRoute.Album(name, artist)) },
                                onOpenFavorites = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.Favorites())
                                },
                                onOpenDownloads = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.Downloads())
                                },
                                topInset = contentPadding.calculateTopPadding(),
                                active = !overlayVisible && !playerState.isExpanded,
                                scrollBehavior = exploreScrollBehavior,
                            )

                            1 -> HistoryScreen(
                                onPlay = { list, index -> playList(list, index) },
                                onMore = { song ->
                                    openMore(song, { s -> confirmDeleteHistory = s }, "从历史记录中删除")
                                },
                                topInset = contentPadding.calculateTopPadding(),
                                scrollBehavior = historyScrollBehavior,
                                onSelectionChange = { selectionBar = it },
                                onBatchAddToPlaylist = { songs -> pickerSongs = songs },
                                onBatchAddToQueue = { songs ->
                                    songs.forEach { PlaybackController.addToQueue(it) }
                                    Toast.makeText(
                                        context,
                                        "已添加 ${songs.size} 首到播放队列",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                                onBatchRemoveHistory = { songs ->
                                    songs.forEach { MusicLibrary.removeHistory(it.id) }
                                    Toast.makeText(
                                        context,
                                        "已删除 ${songs.size} 条历史记录",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            )

                            2 -> PlaylistScreen(
                                onOpen = { playlist -> pushRoute(MusicRoute.PlaylistDetail(playlistId = playlist.id)) },
                                onCreate = {
                                    createForSongs = emptyList()
                                    showCreateDialog = true
                                },
                                topInset = contentPadding.calculateTopPadding(),
                                scrollBehavior = playlistScrollBehavior,
                                onRename = { playlist -> renameTarget = playlist },
                                onSelectionChange = { selectionBar = it },
                                onBatchDelete = { list -> confirmDeletePlaylists = list },
                            )

                            else -> SettingsScreen(
                                topInset = contentPadding.calculateTopPadding(),
                                scrollBehavior = scrollBehavior,
                                hyperBackdrop = wallpaperBackdrop,
                                onOpenWallpaper = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.Wallpaper())
                                },
                                onOpenPlayerSettings = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.PlayerSettings())
                                },
                                onOpenAbout = {
                                    playerState.collapse()
                                    pushRoute(MusicRoute.AboutApp())
                                },
                            )
                            }
                            }
                        }

                    }
                }
            }
            entry<MusicRoute.Artist> { current ->
                NavPage { pageBackdrop ->
                    ArtistScreen(
                        artistName = current.name,
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song -> openMore(song, null, "删除歌曲") },
                        onOpenAlbum = { album -> pushRoute(MusicRoute.Album(album, current.name)) },
                    )
                }
            }
            entry<MusicRoute.Album> { current ->
                NavPage { pageBackdrop ->
                    AlbumScreen(
                        albumName = current.name,
                        artistName = current.artist,
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song -> openMore(song, null, "删除歌曲") },
                    )
                }
            }
            entry<MusicRoute.PlaylistDetail> { current ->
                NavPage { pageBackdrop ->
                    PlaylistDetailScreen(
                        playlistId = current.playlistId,
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song, removable ->
                            openMore(
                                song,
                                if (removable) {
                                    { s -> MusicLibrary.removeSongFromPlaylist(current.playlistId, s.id) }
                                } else null,
                                "从歌单移除",
                            )
                        },
                        onSelectionChange = { selectionBar = it },
                        onExport = {
                            val p = MusicLibrary.playlist(current.playlistId)
                            if (p == null || p.songs.isEmpty()) {
                                Toast.makeText(context, "歌单为空，无法导出", Toast.LENGTH_SHORT).show()
                            } else {
                                exportJson = PlaylistImporter.exportJson(p)
                                val safeName = p.name
                                    .replace(Regex("""[\\/:*?"<>|]"""), "_")
                                    .ifBlank { "playlist" }
                                exportLauncher.launch("$safeName.json")
                            }
                        },
                    )
                }
            }
            entry<MusicRoute.Wallpaper> {
                NavPage { pageBackdrop ->
                    WallpaperScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.PlayerSettings> {
                NavPage { pageBackdrop ->
                    PlayerSettingsScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
            entry<MusicRoute.Favorites> {
                NavPage { pageBackdrop ->
                    FavoritesScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song ->
                            openMore(
                                song,
                                { s -> MusicLibrary.removeFavorite(s.id) },
                                "取消收藏",
                                fromFavorites = true,
                            )
                        },
                        onSelectionChange = { selectionBar = it },
                    )
                }
            }
            entry<MusicRoute.Downloads> {
                NavPage { pageBackdrop ->
                    DownloadsScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                        onPlay = { list, index -> playList(list, index) },
                        onMore = { song ->
                            openMore(
                                song,
                                { s -> downloadDeleteConfirm = listOf(s) },
                                "删除歌曲",
                            )
                        },
                        onSelectionChange = { selectionBar = it },
                        onBatchAddToPlaylist = { songs -> pickerSongs = songs },
                        onBatchAddToQueue = { songs ->
                            songs.forEach { PlaybackController.addToQueue(it) }
                            Toast.makeText(
                                context,
                                "已添加 ${songs.size} 首到播放队列",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onRequestDelete = { startDownloadDelete(it) },
                        refreshSignal = downloadsRefreshKey,
                    )
                }
            }
            entry<MusicRoute.AboutApp> {
                NavPage { pageBackdrop ->
                    AboutAppScreen(
                        onBack = { popRoute() },
                        backdrop = pageBackdrop,
                    )
                }
            }
        }
        }
        }

        // 全屏玻璃搜索层：位于页面层之上、迷你播放器/底栏之下。
        // 层级顺序因此是：NavDisplay → 搜索层 → 播放条/底栏 → 播放页 → popupHost。
        //  - 搜索结果不会再压住播放条（图3）；
        //  - 播放条照常采样 navDisplayBackdrop，模糊效果与其他页面一致；
        //  - 内容走同一条 bottomContentFade，底部和其他界面一样渐隐到壁纸；
        //  - 搜索展开时整条页面栈 alpha 归零，从结果进二级页不会透出重叠内容（图2）。
        GlassSearchOverlay(
            expanded = searchExpanded,
            onDismissRequest = { searchExpanded = false },
            label = "搜索歌曲、歌手和专辑...",
            modifier = Modifier.bottomContentFade(
                startFromBottom = MiniPlayerToNavBarSpacing + BottomBarAreaHeight + 16.dp,
                bottomAlpha = 0.12f,
            ),
        ) { q ->
            HyperSearchScreen(
                query = q,
                active = searchExpanded,
                onPlaySongs = { list, index ->
                    playList(list, index)
                    searchExpanded = false
                },
                onMore = { song -> openMore(song, null, "删除歌曲") },
                onOpenArtist = { name ->
                    searchExpanded = false
                    playerState.collapse()
                    pushRoute(MusicRoute.Artist(name))
                },
                onOpenAlbum = { name, artist ->
                    searchExpanded = false
                    playerState.collapse()
                    pushRoute(MusicRoute.Album(name, artist))
                },
            )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val dockFraction = BottomFloatingWidthFraction

            AnimatedVisibility(
                visible = !selectionActive && route !is MusicRoute.Wallpaper && route !is MusicRoute.PlayerSettings,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(240)) { it / 2 } + fadeOut(tween(160)),
            ) {
                HyperMiniPlayer(
                    state = playerState,
                    backdrop = activeBackdrop,
                    onExpand = {
                        if (playerState.track.hasSong) {
                            playerState.expand()
                        } else {
                            Toast.makeText(context, "当前播放列表为空", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenQueue = { showQueueSheet = true },
                    widthFraction = MiniPlayerWidthFraction,
                    modifier = Modifier
                        .offset(y = miniPlayerShift)
                        .padding(bottom = BottomBarAreaHeight + MiniPlayerToNavBarSpacing),
                )
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = route == null && !selectionActive && !searchExpanded,
                    // 恢复导航条上下移动隐藏/出现的动画；位置高度不变，仅纵向位移+淡入淡出。
                    enter = slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(220)),
                    exit = slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(160)),
                ) {
                    // 搜索按钮的收缩/展开交给 GlassNavigationBar 内部统一处理：
                    // 它会同时动画按钮宽度、间距、缩放与透明度，tab 区域随之重算，
                    // 不再需要在这里手搓 animateDpAsState + 手写 alpha/scale（那会慢一帧）。
                    val showSearchButton = selectedTab == 0
                    HyperBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = selectTab,
                        hyperBackdrop = activeBackdrop,
                        widthFraction = dockFraction,
                        trailingAction = if (showSearchButton) {
                            {
                                GlassIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        searchExpanded = true
                                    },
                                    size = BottomFloatingHeight,
                                    backdrop = activeBackdrop,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Glass.Search,
                                        contentDescription = "搜索",
                                        modifier = Modifier.size(26.dp),
                                        tint = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }

                AnimatedVisibility(
                    visible = selectionActive,
                    enter = fadeIn(tween(200, delayMillis = 70)) +
                        scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.88f),
                    exit = fadeOut(tween(150)) +
                        scaleOut(tween(200), targetScale = 0.92f),
                ) {
                    selectionBar?.let { bar ->
                        SelectionActionBar(
                            state = bar,
                            backdrop = activeBackdrop,
                            widthFraction = BottomFloatingWidthFraction,
                        )
                    }
                }
            }
        }

            CreatePlaylistDialog(
                show = showCreateDialog,
                onDismiss = { showCreateDialog = false },
                onConfirm = { name ->
                    if (name.isNotBlank()) {
                        val playlist: Playlist = MusicLibrary.createPlaylist(name.trim())
                        createForSongs.forEach { MusicLibrary.addSongToPlaylist(playlist.id, it) }
                        createForSongs = emptyList()
                        showCreateDialog = false
                        Toast.makeText(context, "已创建「${playlist.name}」", Toast.LENGTH_SHORT).show()
                    }
                },
                backdrop = activeBackdrop,
            )

            CreatePlaylistDialog(
                show = renameTarget != null,
                title = "重命名歌单",
                initialName = renameTarget?.name.orEmpty(),
                confirmText = "保存",
                onDismiss = { renameTarget = null },
                onConfirm = { name ->
                    val target = renameTarget
                    if (target != null && name.isNotBlank()) {
                        MusicLibrary.renamePlaylist(target.id, name.trim())
                        renameTarget = null
                        Toast.makeText(context, "已重命名", Toast.LENGTH_SHORT).show()
                    }
                },
                backdrop = activeBackdrop,
            )

            TrackMoreSheet(
                song = moreSong,
                onDismiss = { moreSong = null },
                showFavoriteOption = !moreFromFavorites,
                onPlayNext = {
                    PlaybackController.playNext(it)
                    Toast.makeText(context, "下一首将播放该歌曲", Toast.LENGTH_SHORT).show()
                },
                onAddToQueue = {
                    PlaybackController.addToQueue(it)
                    Toast.makeText(context, "已添加到队列", Toast.LENGTH_SHORT).show()
                },
                onAddToPlaylist = { song ->
                    scope.launch {
                        kotlinx.coroutines.delay(120)
                        pickerSongs = listOf(song)
                    }
                },
                onAddToFavorite = { song ->
                    MusicLibrary.toggleFavoriteWithToast(context, song)
                },
                onOpenAlbum = { song ->
                    // 菜单项点击时 TrackMoreSheet 已经调过 onDismiss（弹层自己在回收），
                    // 这里不再额外等待，直接关搜索层并跳转：跳转与弹层回收同时进行，
                    // 少掉那 0.3s 的死等。
                    playerState.collapse()
                    searchExpanded = false
                    pushRoute(MusicRoute.Album(song.albumText, song.artists.firstOrNull() ?: ""))
                },
                onOpenArtist = { song ->
                    playerState.collapse()
                    searchExpanded = false
                    if (song.artists.size > 1) {
                        artistPickerSong = song
                    } else {
                        pushRoute(MusicRoute.Artist(song.artistText))
                    }
                },
                onChangeQuality = { showQualitySheet = true },
                onDownload = { song ->
                    Toast.makeText(context, "开始下载…", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        val result = Downloader.download(context, song)
                        val message = when (result) {
                            is Downloader.Result.Success -> "下载完成"
                            is Downloader.Result.Failure -> result.message
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                },
                onShare = { song ->
                    val text = "${song.name} - ${song.artistText}\n${GdApi.shareUrl(song)}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, song.name)
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "分享歌曲"))
                },
                onDelete = moreDelete,
                deleteLabel = moreDeleteLabel,
            )

            PlaylistPickerSheet(
                songs = pickerSongs,
                onDismiss = { pickerSongs = emptyList() },
                onPick = { songs, playlist ->
                    var added = 0
                    songs.forEach { if (MusicLibrary.addSongToPlaylist(playlist.id, it)) added++ }
                    val message = when {
                        added == 0 -> "该歌单已有这首歌"
                        songs.size == 1 -> "已添加到「${playlist.name}」"
                        added == songs.size -> "已添加 ${added} 首到「${playlist.name}」"
                        else -> "已添加 ${added} 首，其余已在「${playlist.name}」"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    pickerSongs = emptyList()
                },
                onCreateNew = { songs ->
                    pickerSongs = emptyList()
                    createForSongs = songs
                    showCreateDialog = true
                },
            )

            // 「添加歌单」菜单：链接导入 / JSON 导入 / 手动创建
            PlaylistAddMenuPopup(
                expanded = addMenuExpanded,
                anchorBounds = addMenuAnchor,
                backdrop = activeBackdrop,
                onDismiss = { addMenuExpanded = false },
                onImportLink = {
                    linkImportBusy = false
                    linkImportStatus = null
                    linkImportError = null
                    showLinkImport = true
                },
                onImportJson = {
                    jsonLauncher.launch(
                        arrayOf(
                            "application/json",
                            "text/plain",
                            "application/octet-stream",
                            "*/*",
                        ),
                    )
                },
                onCreate = {
                    createForSongs = emptyList()
                    showCreateDialog = true
                },
            )

            PlaylistLinkImportDialog(
                show = showLinkImport,
                busy = linkImportBusy,
                status = linkImportStatus,
                error = linkImportError,
                backdrop = activeBackdrop,
                onDismiss = {
                    // 「取消」在导入途中也可点：先中断在跑的任务，再清状态关窗。
                    linkImportJob?.cancel()
                    linkImportJob = null
                    linkImportBusy = false
                    showLinkImport = false
                    linkImportStatus = null
                    linkImportError = null
                },
                onImport = { link ->
                    linkImportJob?.cancel()
                    linkImportJob = scope.launch {
                        linkImportBusy = true
                        linkImportError = null
                        linkImportStatus = "获取歌单…"
                        try {
                            val res = PlaylistImporter.importFromLink(link) { done, total ->
                                linkImportStatus = "匹配网易云歌曲 $done/$total"
                            }
                            val playlist = MusicLibrary.createPlaylist(res.name)
                            var added = 0
                            res.songs.forEach { if (MusicLibrary.addSongToPlaylist(playlist.id, it)) added++ }
                            Toast.makeText(context, "已添加歌单，共${added}首歌曲", Toast.LENGTH_LONG).show()
                            linkImportBusy = false
                            linkImportStatus = null
                            showLinkImport = false
                        } catch (e: Throwable) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            linkImportBusy = false
                            linkImportStatus = null
                            linkImportError = e.message ?: "导入失败"
                        } finally {
                            linkImportJob = null
                        }
                    }
                },
            )

            PlaylistJsonImportDialog(
                show = showJsonImport,
                busy = jsonImportBusy,
                status = jsonImportStatus,
                error = jsonImportError,
                backdrop = activeBackdrop,
                onDismiss = {
                    jsonImportJob?.cancel()
                    jsonImportJob = null
                    jsonImportBusy = false
                    jsonImportStatus = null
                    jsonImportError = null
                    showJsonImport = false
                },
            )

            ArtistPickerSheet(
                song = artistPickerSong,
                onDismiss = { artistPickerSong = null },
                onPick = { _, artist -> pushRoute(MusicRoute.Artist(artist)) },
            )

            PlaybackQueueSheet(
                visible = showQueueSheet,
                onDismiss = { showQueueSheet = false },
            )

            ConfirmDialog(
                show = confirmDeletePlaylists.isNotEmpty(),
                title = "删除歌单",
                summary = if (confirmDeletePlaylists.size == 1) {
                    "确定要删除「${confirmDeletePlaylists.first().name}」吗？此操作不可恢复"
                } else {
                    "确定要删除选中的 ${confirmDeletePlaylists.size} 个歌单吗？此操作不可恢复"
                },
                confirmText = "删除",
                onConfirm = {
                    val targets = confirmDeletePlaylists
                    confirmDeletePlaylists = emptyList()
                    targets.forEach { MusicLibrary.deletePlaylist(it.id) }
                    Toast.makeText(
                        context,
                        if (targets.size == 1) "已删除「${targets.first().name}」"
                        else "已删除 ${targets.size} 个歌单",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onDismiss = { confirmDeletePlaylists = emptyList() },
                backdrop = activeBackdrop,
            )

            ConfirmDialog(
                show = confirmDeleteHistory != null,
                title = "删除历史记录",
                summary = "确定要从历史记录中删除「${confirmDeleteHistory?.name.orEmpty()}」吗？",
                confirmText = "删除",
                onConfirm = {
                    val target = confirmDeleteHistory
                    confirmDeleteHistory = null
                    if (target != null) MusicLibrary.removeHistory(target.id)
                },
                onDismiss = { confirmDeleteHistory = null },
                backdrop = activeBackdrop,
            )

            ConfirmDialog(
                show = downloadDeleteConfirm != null,
                title = "删除歌曲",
                summary = if ((downloadDeleteConfirm?.size ?: 0) == 1) {
                    "确认从下载文件夹中删除这首歌吗？"
                } else {
                    "确认从下载文件夹中删除 ${downloadDeleteConfirm?.size ?: 0} 首歌曲吗？"
                },
                confirmText = "删除",
                onConfirm = {
                    val targets = downloadDeleteConfirm ?: emptyList()
                    downloadDeleteConfirm = null
                    startDownloadDelete(targets)
                },
                onDismiss = { downloadDeleteConfirm = null },
                backdrop = activeBackdrop,
            )

            ConfirmDialog(
                show = confirmClearHistory,
                title = "清空历史记录",
                summary = "确定要清空全部历史播放记录吗？",
                confirmText = "清空",
                onConfirm = {
                    confirmClearHistory = false
                    MusicLibrary.clearHistory()
                },
                onDismiss = { confirmClearHistory = false },
                backdrop = activeBackdrop,
            )

            QualitySheet(
                visible = showQualitySheet,
                onDismiss = { showQualitySheet = false },
                onPick = { quality ->
                    MusicLibrary.setQuality(quality)
                    PlaybackController.reloadCurrent()
                    showQualitySheet = false
                },
            )

            SleepTimerSheet(
                visible = showSleepTimer,
                onDismiss = { showSleepTimer = false },
            )

        HyperPlayerLayer(
            state = playerState,
            contentBackdrop = playerPageLayer,
            onOpenQueue = { showQueueSheet = true },
            onOpenSleepTimer = { showSleepTimer = true },
            onMore = {
                val current = PlaybackController.currentSong()
                if (current != null) openMore(current, null, "删除歌曲")
            },
        )

        }
    }
    }
}

@Composable
private fun CreatePlaylistDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    backdrop: Backdrop? = null,
    title: String = "新建歌单",
    initialName: String = "",
    confirmText: String = "创建",
) {
    var name by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(show) {
        if (show) name = initialName
    }

    val dismiss: () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus()
        onDismiss()
    }
    val confirm: (String) -> Unit = { value ->
        keyboard?.hide()
        focusManager.clearFocus()
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        onConfirm(value)
    }

    GlassDialog(
        title = title,
        show = show,
        onDismissRequest = dismiss,
        backdrop = backdrop?.takeIf { LocalAdvancedMaterialEnabled.current },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
            ) {
                NativeTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    hint = "歌单名称",
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        dismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = confirmText,
                    onClick = { confirm(name.trim()) },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QualitySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (AudioQuality) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val current = MusicLibrary.settings.value.quality

    HyperSheet(visible = visible, onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader("音质")
            SheetCard {
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onPick(quality) }
                            .padding(horizontal = 14.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = quality.label,
                            color = if (quality == current) scheme.primary else scheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = quality.bitrateText,
                            color = if (quality == current) scheme.primary else scheme.onSurfaceSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}
