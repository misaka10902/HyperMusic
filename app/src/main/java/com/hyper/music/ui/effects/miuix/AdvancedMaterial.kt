package com.hyper.music.ui.effects.miuix

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.hyper.music.music.MusicLibrary
import top.yukonga.miuix.kmp.blur.Backdrop

val LocalAdvancedMaterialEnabled = staticCompositionLocalOf { true }


val LocalActiveBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun rememberAdvancedMaterialEnabled(): Boolean {
    val settings by MusicLibrary.settings.collectAsState()
    return settings.advancedMaterial
}
