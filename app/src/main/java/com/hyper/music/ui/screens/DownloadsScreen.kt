package com.hyper.music.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hyper.music.music.LocalSongs
import com.hyper.music.music.Song
import com.hyper.music.ui.components.ConfirmDialog
import com.hyper.music.ui.components.SelectionAction
import com.hyper.music.ui.components.SelectionBarState
import com.hyper.music.ui.components.TrackRow
import com.hyper.music.ui.effects.miuix.rememberPopupBackdrop
import kotlinx.coroutines.launch
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

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
        refreshing = false
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
        backdrop = rememberPopupBackdrop(),
    )

    DetailPage(
        title = "本地歌曲",
        onBack = onBack,
        backdrop = backdrop,
        multiSelect = multiSelect,
        selectionCount = selectedIds.size,
        onExitMultiSelect = { selectedIds = emptySet() },
        listState = rememberLazyListState(),
        header = {

        },


        pullRefresh = PullRefreshSpec(
            isRefreshing = refreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                refreshing = true
                refreshKey++
            },
        ),
    ) {
        if (songs.isEmpty()) {

            if (!loading) item {

                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LibraryEmptyState(
                        icon = MiuixIcons.FileDownloads,
                        text = "没有本地歌曲",
                    )
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
