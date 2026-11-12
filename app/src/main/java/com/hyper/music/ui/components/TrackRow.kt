package com.hyper.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.NetCover
import com.hyper.music.music.Song
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val TrackCoverSize = 44.dp

fun formatTime(ms: Long): String {
    if (ms <= 0) return "--:--"
    val total = ms / 1000
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

@Composable
fun TrackRow(
    song: Song,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    showCover: Boolean = true,
    highlight: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    moreAlignEnd: Boolean = false,
    showLocalBadge: Boolean = true,
) {
    val scheme = MiuixTheme.colorScheme
    val coverUrl = remember(song.id) { GdApi.coverUrl(song, CoverSize.SMALL.request).ifBlank { null } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (selected) scheme.primary.copy(alpha = 0.10f) else Color.Transparent,
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCover) {
            NetCover(
                url = coverUrl,
                modifier = Modifier.size(TrackCoverSize),
                corner = TrackCoverSize / 2,
                contentDescription = song.name,
                maxSize = CoverSize.SMALL.decode,
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(end = 32.dp)) {
            Text(
                text = song.name,
                color = if (highlight) scheme.primary else scheme.onSurface,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = song.subtitle,
                    color = scheme.onSurfaceSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (showLocalBadge && song.isLocal) {
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.foundation.Image(
                        imageVector = MiuixIcons.Download,
                        contentDescription = "已下载歌曲",
                        modifier = Modifier.size(14.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(scheme.onSurfaceSecondary),
                    )
                }
            }
        }

        Text(
            text = formatTime(song.durationMs),
            color = scheme.onSurfaceSecondary,
            fontSize = 12.sp,
        )
        Spacer(Modifier.width(10.dp))

        if (selectionMode) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Checkbox(
                    state = if (selected) ToggleableState.On else ToggleableState.Off,
                    onClick = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMore,
                    ),

                contentAlignment = if (moreAlignEnd) Alignment.CenterEnd else Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    imageVector = MiuixIcons.More,
                    contentDescription = "更多",
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(scheme.onSurfaceSecondary),
                )
            }
        }
    }
}

@Composable
fun EmptyHint(
    text: String,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    icon: ImageVector? = null,
) {
    if (centered) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyHintContent(text = text, icon = icon)
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyHintContent(text = text, icon = icon)
        }
    }
}

@Composable
private fun EmptyHintContent(text: String, icon: ImageVector?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                colorFilter = ColorFilter.tint(
                    MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.55f)
                ),
            )
            Spacer(Modifier.height(12.dp))
        }
        EmptyHintText(text)
    }
}

@Composable
private fun EmptyHintText(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.7f),
        fontSize = 13.sp,
    )
}
