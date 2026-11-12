package com.hyper.music.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.LocalSongs
import com.hyper.music.music.Song
import com.hyper.music.ui.basic.HyperTopBarButton
import com.hyper.music.ui.basic.RightAlignedTooltipPositionProvider
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.components.SelectionAction
import com.hyper.music.ui.components.SelectionBarState
import com.hyper.music.ui.components.TrackRow
import com.hyper.music.ui.effects.miuix.LocalActiveBackdrop
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import top.yukonga.miuix.kmp.basic.RichTooltipBox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val HELP_TEXT =
    "本列表会列出所有在下载文件夹中保存的歌曲文件，所以可能会出现其他来源的歌曲。" +
        "由于 GD API 限制，不建议短时间内下载大量歌曲，否则会触发风控临时拉黑，敬请谅解。"


@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
    onPlay: (List<Song>, Int) -> Unit,
    onMore: (Song) -> Unit,
    onSelectionChange: (SelectionBarState?) -> Unit = {},
    onBatchAddToPlaylist: (List<Song>) -> Unit = {},
    onBatchAddToQueue: (List<Song>) -> Unit = {},
    onRequestDelete: (List<Song>) -> Unit = {},
    refreshSignal: Int = 0,
) {
    val context = LocalContext.current
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }



    val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshKey++ }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, readPermission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(readPermission)
        }
    }


    LaunchedEffect(refreshKey, refreshSignal) {
        loading = true
        songs = LocalSongs.scan(context)
        loading = false
    }

    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val multiSelect = selectedIds.isNotEmpty()
    BackHandler(enabled = multiSelect) { selectedIds = emptySet() }
    var confirmDelete by remember { mutableStateOf(false) }


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
                            label = "添加到歌单",
                            icon = MiuixIcons.Playlist,
                        ) { onBatchAddToPlaylist(picked) },
                        SelectionAction(
                            label = "添加到队列",
                            icon = MiuixIcons.ListView,
                        ) {
                            onBatchAddToQueue(picked)
                            selectedIds = emptySet()
                        },
                        SelectionAction(
                            label = "删除",
                            icon = MiuixIcons.Delete,
                            tint = scheme.error,
                        ) { confirmDelete = true },
                    ),
                    onExit = { selectedIds = emptySet() },
                )
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionChange(null) }
    }

    val tooltipState = rememberTooltipState(isPersistent = true)

    val spinTransition = rememberInfiniteTransition(label = "refreshSpin")
    val spin by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "refreshSpinDeg",
    )
    val endAction: @Composable (Float, Float) -> Unit = { backdropAlpha, shadowAlpha ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            HyperTopBarButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    refreshKey++
                },
                backdrop = backdrop,
                icon = MiuixIcons.Refresh,
                contentDescription = "刷新本地歌曲列表",
                iconSize = 22.dp,
                iconRotation = if (loading) spin else 0f,
                backdropAlpha = backdropAlpha,
                shadowAlpha = shadowAlpha,
            )
            Spacer(Modifier.width(10.dp))
            RichTooltipBox(
                text = HELP_TEXT,
                title = "提示",
                actionText = "我知道了",
                state = tooltipState,
                positioning = TooltipAnchorPosition.Below,
                positionProvider = remember {
                    RightAlignedTooltipPositionProvider(spacing = 8)
                },
            ) {
                HyperTopBarButton(
                    onClick = { scope.launch { tooltipState.show() } },
                    backdrop = backdrop,
                    icon = MiuixIcons.Help,
                    contentDescription = "提示",
                    iconSize = 22.dp,
                    backdropAlpha = backdropAlpha,
                    shadowAlpha = shadowAlpha,
                )
            }
        }
    }

    ConfirmDialog(
        show = confirmDelete,
        title = "删除歌曲",
        summary = "确认从下载文件夹中删除 ${selectedIds.size} 首歌曲吗？",
        confirmText = "删除",
        onConfirm = {
            val picked = songs.filter { it.id in selectedIds }
            selectedIds = emptySet()
            confirmDelete = false
            onRequestDelete(picked)
        },
        onDismiss = { confirmDelete = false },
        backdrop = LocalActiveBackdrop.current,
    )

    DetailPage(
        title = "已下载的歌曲",
        subtitle = "下载文件夹中共有${songs.size}首歌曲",
        onBack = onBack,
        backdrop = backdrop,
        listState = rememberLazyListState(),
        endAction = endAction,
        header = {

        },
    ) {
        if (songs.isEmpty()) {


            if (!loading) item {


                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            imageVector = MiuixIcons.FileDownloads,
                            contentDescription = null,
                            modifier = Modifier.size(46.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                scheme.onSurfaceSecondary.copy(alpha = 0.55f),
                            ),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "没有已下载的歌曲",
                            color = scheme.onSurfaceSecondary.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        } else {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            TrackRow(
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(200),
                    placementSpec = spring(dampingRatio = 1f, stiffness = 320f),
                    fadeOutSpec = tween(260),
                ),
                song = song,
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
                showLocalBadge = false,
                selected = song.id in selectedIds,
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedIds = selectedIds + song.id
                },
            )
        }
        }
    }
}
