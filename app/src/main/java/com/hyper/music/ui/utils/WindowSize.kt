package com.hyper.music.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class WindowSizeDp(val width: Dp, val height: Dp)

@Composable
@ReadOnlyComposable
fun rememberWindowSizeDp(): WindowSizeDp {
    val info = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        WindowSizeDp(info.containerSize.width.toDp(), info.containerSize.height.toDp())
    }
}

fun isCompactWindow(size: WindowSizeDp): Boolean =
    size.width > size.height || size.height < 600.dp

fun isLargeScreen(size: WindowSizeDp): Boolean =
    size.width >= 600.dp && size.height >= 600.dp

/** 歌曲条目列数：大屏两列，手机单列。 */
@Composable
@ReadOnlyComposable
fun songColumns(): Int = if (isLargeScreen(rememberWindowSizeDp())) 2 else 1

/** 大屏内容区宽度占比，搜索框、推荐、搜索历史共用，保证左右边缘对齐。 */
fun contentFraction(columns: Int): Float = if (columns == 2) 2f / 3f else 1f

@Composable
fun horizontalContentPadding(): Dp = with(rememberWindowSizeDp().width) {
    val w = value.toInt()
    if (w >= 600) ((w - 600).coerceIn(0, 600) / 600f * 112f + 16f).dp else 16.dp
}
