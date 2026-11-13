package com.hyper.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.MusicLibrary
import com.hyper.music.music.NetCover
import com.hyper.music.music.Playlist
import com.hyper.music.music.Song
import com.hyper.music.ui.effects.miuix.LocalActiveBackdrop
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.ReplyAll
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.offset

@Composable
fun HyperSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    immersive: Boolean = true,
    followIme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val navBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    val hyperBackdrop = LocalActiveBackdrop.current

    val advancedMaterial = LocalAdvancedMaterialEnabled.current

    GlassBottomSheet(
        show = visible,
        onDismissRequest = onDismiss,
        backdrop = if (advancedMaterial) hyperBackdrop else null,
        modifier = modifier,
        title = title,
        cornerRadius = 32.dp,
        insideMargin = DpSize(12.dp, 0.dp),
        outsideMargin = DpSize(0.dp, 0.dp),

        defaultWindowInsetsPadding = followIme,
        content = {
            val boundaryGap = if (immersive) {
                maxOf(navBottom, 12.dp) + 12.dp
            } else {
                maxOf(navBottom, 16.dp)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = boundaryGap),
            ) {
                content()
            }
        },
    )
}

@Composable
fun SheetHeader(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 6.dp)
            .offset(y = (-10).dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun SheetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        content = content,
    )
}

@Composable
fun TrackMoreSheet(
    song: Song?,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onAddToFavorite: (Song) -> Unit,
    onOpenAlbum: (Song) -> Unit,
    onOpenArtist: (Song) -> Unit,
    onChangeQuality: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onShare: (Song) -> Unit,
    onDelete: ((Song) -> Unit)? = null,
    deleteLabel: String = "删除歌曲",
    showFavoriteOption: Boolean = true,
) {
    var lastSong by remember { mutableStateOf<Song?>(null) }
    if (song != null) lastSong = song
    val scheme = MiuixTheme.colorScheme
    val context = LocalContext.current
    val quality = MusicLibrary.settings.value.quality
    val favorites by MusicLibrary.favorites.collectAsState()
    var showComments by remember { mutableStateOf(false) }
    LaunchedEffect(song) { if (song != null) showComments = false }

    HyperSheet(visible = song != null, onDismiss = onDismiss) {
        val target = lastSong ?: return@HyperSheet
        val isFavorite = favorites.any { it.id == target.id }
        Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = showComments,
            transitionSpec = {
                ContentTransform(
                    initialContentExit = fadeOut(animationSpec = tween(150)),
                    targetContentEnter = fadeIn(animationSpec = tween(210)),
                    sizeTransform = SizeTransform(
                        clip = true,
                        sizeAnimationSpec = { _, _ -> tween(240, easing = FastOutSlowInEasing) },
                    ),
                )
            },
            label = "trackMoreComments",
        ) { inComments ->
        if (inComments) {
            CommentsContent(song = target, onBack = { showComments = false })
        } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainer, RoundedCornerShape(20.dp))
                    .pointerInput(target.name) {
                        detectTapGestures(
                            onLongPress = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("歌曲名称", target.name))
                                Toast.makeText(context, "已复制歌曲名称", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NetCover(
                        url = GdApi.coverUrl(target, CoverSize.SMALL.request).ifBlank { null },
                        modifier = Modifier.size(56.dp),
                        maxSize = CoverSize.SMALL.decode,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.name,
                            color = scheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = target.subtitle,
                                color = scheme.onSurfaceSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainer, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                MenuEntry(MiuixIcons.Add, "下一首播放") { onDismiss(); onPlayNext(target) }
                MenuEntry(MiuixIcons.Playlist, "添加到队列") { onDismiss(); onAddToQueue(target) }
                MenuEntry(MiuixIcons.AddCircle, "添加到歌单") { onDismiss(); onAddToPlaylist(target) }
                if (showFavoriteOption) {
                    MenuEntry(
                        icon = if (isFavorite) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                        text = if (isFavorite) "取消收藏" else "添加到收藏",
                    ) { onDismiss(); onAddToFavorite(target) }
                }

                if (!target.isLocal) {
                    MenuEntry(MiuixIcons.Album, "专辑：${target.albumText}") { onDismiss(); onOpenAlbum(target) }
                    MenuEntry(MiuixIcons.Contacts, "艺术家：${target.artistText}") { onDismiss(); onOpenArtist(target) }
                    MenuEntry(MiuixIcons.Music, "音质：${quality.label} · ${quality.bitrateText}") { onDismiss(); onChangeQuality(target) }
                    MenuEntry(MiuixIcons.ReplyAll, "查看评论") { showComments = true }
                    MenuEntry(MiuixIcons.Download, "下载到本地") { onDismiss(); onDownload(target) }
                    MenuEntry(MiuixIcons.Share, "分享") { onDismiss(); onShare(target) }
                }
                if (onDelete != null) {
                    MenuEntry(MiuixIcons.Delete, deleteLabel, tint = scheme.error) {
                        onDismiss()
                        onDelete(target)
                    }
                }
            }
        }
        }
        }
        }
    }
}

@Composable
private fun MenuEntry(
    icon: ImageVector,
    text: String,
    tint: Color = MiuixTheme.colorScheme.onSurface,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(23.dp),
            colorFilter = ColorFilter.tint(tint.copy(alpha = 0.92f)),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = tint,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PlaylistPickerSheet(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onPick: (List<Song>, Playlist) -> Unit,
    onCreateNew: (List<Song>) -> Unit,
) {
    var lastSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    if (songs.isNotEmpty()) lastSongs = songs
    val list by MusicLibrary.playlists.collectAsState()
    val scheme = MiuixTheme.colorScheme

    HyperSheet(visible = songs.isNotEmpty(), onDismiss = onDismiss) {
        val target = lastSongs
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader("添加到歌单")
            SheetCard {
                MenuEntry(MiuixIcons.Add, "新建歌单", fontSize = 17.sp) {
                    onDismiss(); onCreateNew(target)
                }
                if (list.isEmpty()) {
                    Text(
                        text = "还没有歌单，先新建一个吧",
                        color = scheme.onSurfaceSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    )
                } else {
                    list.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onPick(target, playlist); onDismiss() }
                                .padding(horizontal = 4.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = playlist.name,
                                color = scheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${playlist.songs.size} 首",
                                color = scheme.onSurfaceSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistPickerSheet(
    song: Song?,
    onDismiss: () -> Unit,
    onPick: (Song, String) -> Unit,
) {
    var lastSong by remember { mutableStateOf<Song?>(null) }
    if (song != null) lastSong = song
    val scheme = MiuixTheme.colorScheme

    HyperSheet(visible = song != null, onDismiss = onDismiss) {
        val target = lastSong ?: return@HyperSheet
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader("选择歌手")
            SheetCard {
                target.artists.forEach { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onPick(target, artist); onDismiss() }
                            .padding(horizontal = 4.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.Image(
                            imageVector = MiuixIcons.Contacts,
                            contentDescription = null,
                            modifier = Modifier.size(23.dp),
                            colorFilter = ColorFilter.tint(scheme.onSurface.copy(alpha = 0.92f)),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = artist,
                            color = scheme.onSurface,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
