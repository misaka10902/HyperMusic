package com.hyper.music.ui.effects.miuix

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.hyper.music.music.WallpaperManager
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun cardGlassEnabled(): Boolean {
    val wallpaper by WallpaperManager.state.collectAsState()
    return wallpaper.hasImage
}

@Composable
fun cardGlassAlpha(): Float {
    val state by WallpaperManager.state.collectAsState()
    return if (cardGlassEnabled()) state.cardOpacity.coerceIn(0f, 1f) else 1f
}

@Composable
fun Modifier.cardGlass(shape: Shape): Modifier {
    val alpha = cardGlassAlpha()
    val scheme = MiuixTheme.colorScheme
    return this.background(scheme.surfaceContainer.copy(alpha = alpha), shape)
}

@Composable
fun cardGlassColors(): CardColors = if (cardGlassEnabled()) {
    CardDefaults.defaultColors(
        color = Color.Transparent,
        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
    )
} else {
    CardDefaults.defaultColors()
}
