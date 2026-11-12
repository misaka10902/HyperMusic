package com.hyper.music.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
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

@Composable
fun horizontalContentPadding(): Dp = with(rememberWindowSizeDp().width) {
    val w = value.toInt()
    if (w >= 600) ((w - 600).coerceIn(0, 600) / 600f * 112f + 16f).dp else 16.dp
}

@Composable
fun fallbackTopBarHeight(showLargeTitle: Boolean = true): Dp {
    val statusBarInset = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topInset = if (statusBarInset > 0.dp) statusBarInset else 64.dp
    return topInset + 52.dp + if (showLargeTitle) 56.dp else 0.dp
}
