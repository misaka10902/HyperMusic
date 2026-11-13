package com.hyper.music.ui.activities
import androidx.compose.ui.graphics.takeOrElse
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.blur.LayerBackdrop

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
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
import com.hyper.music.music.WallpaperPageBox
import com.hyper.music.music.WallpaperManager
import com.hyper.music.music.CoverLoader
import com.hyper.music.music.LocalSongs
import com.hyper.music.music.MediaCache
import com.hyper.music.ui.components.ArtistPickerSheet
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.components.HyperSheet
import com.hyper.music.ui.components.PlaybackQueueSheet
import com.hyper.music.ui.components.PlaylistPickerSheet
import com.hyper.music.ui.components.PlaylistImportSheet
import com.hyper.music.ui.components.HyperBottomBar
import com.hyper.music.ui.components.TabletTopBar
import com.hyper.music.ui.components.SelectionActionBar
import com.hyper.music.ui.components.SelectionBarState
import com.hyper.music.ui.components.SleepTimerSheet
import com.hyper.music.ui.components.TrackMoreSheet
import com.hyper.music.ui.effects.miuix.LocalActiveBackdrop
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
import com.hyper.music.ui.screens.PlaylistDetailScreen
import com.hyper.music.ui.screens.PlaylistScreen
import com.hyper.music.ui.screens.PlayerSettingsScreen
import com.hyper.music.ui.screens.SettingsScreen
import com.hyper.music.ui.screens.WallpaperScreen
import com.hyper.music.ui.theme.CourseHyperTheme
import com.hyper.music.ui.utils.BottomFloatingWidthFraction
import com.hyper.music.ui.utils.applyThemeAwareSystemBars
import com.hyper.music.ui.utils.bottomContentFade
import com.hyper.music.ui.utils.isLargeScreen
import com.hyper.music.ui.utils.rememberWindowSizeDp
import com.hyper.music.util.CrashLogger
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
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.LocalDialogStates
import top.yukonga.miuix.kmp.utils.LocalPopupStates
import top.yukonga.miuix.kmp.utils.LocalRootDialogStates
import top.yukonga.miuix.kmp.utils.LocalRootPopupStates
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils
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

private val TAB_TITLES = listOf("探索", "历史", "歌单", "定制")

private const val TabletBarWidthFraction = 1f / 3f

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

    val tablet = isLargeScreen(rememberWindowSizeDp())

    
    val miniPlayerShift by animateDpAsState(
        targetValue = if (tablet || route == null) 0.dp else MiniPlayerHeight + MiniPlayerToNavBarSpacing,
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
    var showImportSheet by remember { mutableStateOf(false) }
    var importPendingSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var exportJson by remember { mutableStateOf<String?>(null) }

    var confirmDeletePlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var confirmDeleteHistory by remember { mutableStateOf<Song?>(null) }
    var confirmClearHistory by remember { mutableStateOf(false) }

    var downloadDeleteConfirm by remember { mutableStateOf<List<Song>?>(null) }
    var pendingDirectDelete by remember { mutableStateOf(emptyList<Song>()) }
    var pendingMediaDelete by remember { mutableStateOf(emptyList<Song>()) }
    var downloadsRefreshKey by remember { mutableIntStateOf(0) }
    var showCrashDialog by remember { mutableStateOf(CrashLogger.lastCrash.isNotBlank()) }

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
        showCrashDialog || showImportSheet

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
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    val rootBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    val playerPageBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    var activeBackdrop by remember { mutableStateOf<Backdrop>(rootBackdrop) }
    LaunchedEffect(playerState.isExpanded) {
        activeBackdrop = if (playerState.isExpanded) playerPageBackdrop else rootBackdrop
    }

    fun playList(songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        PlaybackController.playSongs(songs, index)
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    BackHandler(enabled = selectionActive) { exitSelection() }
    BackHandler(enabled = navBackStack.size > 1 && !selectionActive) { popRoute() }

    var exploreSearchEngaged by remember { mutableStateOf(false) }
    var exploreExitSearchSignal by remember { mutableIntStateOf(0) }
    BackHandler(
        enabled = exploreSearchEngaged && !selectionActive && navBackStack.size == 1,
    ) {
        exploreExitSearchSignal++
    }

    val overlayDialogStates = remember { mutableStateListOf<MiuixPopupUtils.DialogState>() }
    val overlayPopupStates = remember { mutableStateListOf<MiuixPopupUtils.PopupState>() }

    val advancedMaterial = rememberAdvancedMaterialEnabled()

    CompositionLocalProvider(
        LocalDialogStates provides overlayDialogStates,
        LocalPopupStates provides overlayPopupStates,
        LocalRootDialogStates provides overlayDialogStates,
        LocalRootPopupStates provides overlayPopupStates,
        LocalAdvancedMaterialEnabled provides advancedMaterial,
        LocalActiveBackdrop provides activeBackdrop,
    ) {
    @Composable
    fun NavPage(content: @Composable (LayerBackdrop) -> Unit) {
        val surface = MiuixTheme.colorScheme.surface

        val pageBackdrop = rememberLayerBackdrop {
            drawRect(surface)
            drawContent()
        }

        WallpaperPageBox {
            content(pageBackdrop)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {

        WallpaperBackground()
        val noPredictiveBackOwner = remember {
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher =
                    NavigationEventDispatcher().also { it.isEnabled = false }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(rootBackdrop),
        ) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides noPredictiveBackOwner) {
        NavDisplay(
            backStack = navBackStack,

            modifier = Modifier
                .fillMaxSize()
                .bottomContentFade(

                    startFromBottom = MiniPlayerToNavBarSpacing + BottomBarAreaHeight + 16.dp,
                    bottomAlpha = 0.12f,
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

    
    val topActionsContent: (@Composable () -> Unit) = {
        AnimatedContent(
            targetState = if (selectionActive) -1 else selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(190)) togetherWith fadeOut(animationSpec = tween(140))
            },
            contentAlignment = Alignment.CenterEnd,
            label = "topEndActions",
        ) { key ->
            if (key == -1) {
                val extra = selectionBar?.singleAction
                val selectAll = selectionBar?.selectAllAction
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
                        Spacer(Modifier.width(10.dp))
                    }
                    GlassIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (extra != null) extra.onClick() else exitSelection()
                        },
                    ) {
                        Icon(
                            imageVector = extra?.icon ?: MiuixIcons.Close,
                            contentDescription = extra?.label ?: "退出多选",
                            modifier = Modifier.size(22.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                when (key) {
                    0 -> GlassIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            playerState.collapse()
                            pushRoute(MusicRoute.Downloads())
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.FileDownloads,
                            contentDescription = "已下载的歌曲",
                            modifier = Modifier.size(23.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }

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

                    2 -> Row {
                        GlassIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                playerState.collapse()
                                pushRoute(MusicRoute.Favorites())
                            },
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Favorites,
                                contentDescription = "我喜欢的歌曲",
                                modifier = Modifier.size(22.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        GlassIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                importPendingSongs = emptyList()
                                showImportSheet = true
                            },
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
    }

                Scaffold(
                    containerColor = if (wallpaperActive.hasImage) Color.Transparent else MiuixTheme.colorScheme.surface,
                    topBar = {
                        val barScrollBehavior = when (selectedTab) {
                            0 -> exploreScrollBehavior
                            1 -> historyScrollBehavior
                            2 -> playlistScrollBehavior
                            else -> scrollBehavior
                        }
                        if (tablet) {
                            TabletTopBar(
                                titles = TAB_TITLES,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                backdrop = backdrop,
                                scrollBehavior = barScrollBehavior,
                                progressiveBlur = advancedMaterial,
                                actions = topActionsContent,
                            )
                        } else {
                            val barTitle = if (selectionActive) {
                                "已选择 ${selectionBar?.count ?: 0} 项"
                            } else {
                                TAB_TITLES[selectedTab]
                            }
                            GlassTopAppBar(
                                title = barTitle,
                                largeTitle = barTitle,
                                subtitle = if (selectionActive) "" else when (selectedTab) {
                                    1 -> "共${MusicLibrary.history.collectAsState().value.size}首历史播放歌曲"
                                    2 -> "共${MusicLibrary.playlists.collectAsState().value.size}个歌单"
                                    else -> ""
                                },
                                backdrop = backdrop,
                                progressiveBlur = advancedMaterial,
                                scrollBehavior = barScrollBehavior,
                                actions = { topActionsContent() },
                            )
                        }
                    },
                    bottomBar = {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    if (tablet) {

                                        MiniPlayerHeight + 32.dp
                                    } else {
                                        MiniPlayerHeight + MiniPlayerToNavBarSpacing + BottomBarAreaHeight
                                    }
                                ),
                        )
                    },
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    ) {

                        WallpaperBackground()
                        Crossfade(
                            targetState = selectedTab,
                            modifier = Modifier.fillMaxSize(),
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            label = "tabContent",
                        ) { tab ->
                            
                            Box(
                                modifier = if (tablet) {
                                    Modifier
                                        .fillMaxHeight()
                                        .widthIn(max = 1100.dp)
                                        .align(Alignment.TopCenter)
                                } else {
                                    Modifier.fillMaxSize()
                                },
                            ) {
                            when (tab) {
                            0 -> ExploreScreen(
                                onPlaySongs = { list, index -> playList(list, index) },
                                onMore = { song -> openMore(song, null, "删除歌曲") },
                                onOpenArtist = { name -> pushRoute(MusicRoute.Artist(name)) },
                                onOpenAlbum = { name, artist -> pushRoute(MusicRoute.Album(name, artist)) },
                                topInset = contentPadding.calculateTopPadding(),
                                active = !overlayVisible && !playerState.isExpanded,
                                onSearchEngagedChange = { exploreSearchEngaged = it },
                                exitSearchSignal = exploreExitSearchSignal,
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
                                    importPendingSongs = emptyList()
                                    showImportSheet = true
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
                                hyperBackdrop = backdrop,
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

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val dockFraction = BottomFloatingWidthFraction

            AnimatedVisibility(
                visible = if (tablet) {
                    !selectionActive
                } else {
                    !selectionActive && route !is MusicRoute.Wallpaper && route !is MusicRoute.PlayerSettings
                },
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
                    widthFraction = if (tablet) TabletBarWidthFraction else MiniPlayerWidthFraction,
                    modifier = Modifier
                        .offset(y = miniPlayerShift)
                        .padding(
                            bottom = if (tablet) 24.dp else BottomBarAreaHeight + MiniPlayerToNavBarSpacing,
                        ),
                )
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(

                    visible = route == null && !selectionActive && !tablet,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(200)),
                ) {
                    HyperBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        hyperBackdrop = activeBackdrop,
                        widthFraction = dockFraction,
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
                            widthFraction = if (tablet) TabletBarWidthFraction else BottomFloatingWidthFraction,
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

            CrashReportDialog(
                show = showCrashDialog,
                onDismiss = { showCrashDialog = false },
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
                    playerState.collapse()
                    pushRoute(MusicRoute.Album(song.albumText, song.artists.firstOrNull() ?: ""))
                },
                onOpenArtist = { song ->
                    playerState.collapse()
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
                    importPendingSongs = songs
                    showImportSheet = true
                },
            )

            PlaylistImportSheet(
                visible = showImportSheet,
                pendingSongs = importPendingSongs,
                onDismiss = {
                    showImportSheet = false
                    importPendingSongs = emptyList()
                },
                onImported = { name, songs, _ ->
                    val playlist = MusicLibrary.createPlaylist(name)
                    var added = 0
                    songs.forEach { if (MusicLibrary.addSongToPlaylist(playlist.id, it)) added++ }
                    Toast.makeText(
                        context,
                        "已添加歌单，共${added}首歌曲",
                        Toast.LENGTH_LONG,
                    ).show()
                    showImportSheet = false
                    importPendingSongs = emptyList()
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
            backdrop = activeBackdrop,
            pageBackdrop = playerPageBackdrop,
            onOpenQueue = { showQueueSheet = true },
            onOpenSleepTimer = { showSleepTimer = true },
            onMore = {
                val current = PlaybackController.currentSong()
                if (current != null) openMore(current, null, "删除歌曲")
            },
        )

        MiuixPopupUtils.MiuixPopupHost()
        }
    }
}

@Composable
private fun CrashReportDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    GlassDialog(
        title = "上次运行崩溃了",
        show = show,
        onDismissRequest = onDismiss,
        backdrop = LocalActiveBackdrop.current,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixTheme.colorScheme.secondaryContainer)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = CrashLogger.lastCrash,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "复制",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val manager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        manager.setPrimaryClip(
                            ClipData.newPlainText("crash", CrashLogger.lastCrash)
                        )
                        Toast.makeText(context, "已复制崩溃日志", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                )
                TextButton(
                    text = "关闭",
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
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
