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

/**
 * 应用仅面向手机：不再对平板/大屏做专门适配，统一按手机单列布局。
 * 保留这些函数以兼容调用点，固定返回手机值。
 */
@ReadOnlyComposable
fun isLargeScreen(size: WindowSizeDp): Boolean = false

@Composable
@ReadOnlyComposable
fun songColumns(): Int = 1

fun contentFraction(columns: Int): Float = 1f

@Composable
fun horizontalContentPadding(): Dp = 16.dp
