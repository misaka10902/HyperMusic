package com.hyper.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RefreshIndicator(
    modifier: Modifier = Modifier,
    text: String = "正在刷新",
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.5.dp,
) {
    val scheme = MiuixTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InfiniteProgressIndicator(
            color = scheme.onSurfaceSecondary,
            size = size,
            strokeWidth = strokeWidth,
            orbitingDotSize = strokeWidth,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = text,
            color = scheme.onSurfaceSecondary,
            fontSize = 12.sp,
        )
    }
}
