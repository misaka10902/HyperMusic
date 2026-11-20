package com.hyper.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.BottomFloatingHeight
import com.hyper.music.ui.utils.BottomFloatingWidthFraction
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Recording
import top.yukonga.miuix.kmp.icon.extended.WorldClock
import top.yukonga.miuix.kmp.icon.glass.Item
import top.yukonga.miuix.kmp.icon.glass.SettingsFill
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun HyperBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    hyperBackdrop: Backdrop? = null,
    widthFraction: Float = BottomFloatingWidthFraction,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    if (hyperBackdrop == null) return

    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val barBackdrop = if (advancedMaterial) hyperBackdrop else null

    val items = remember {
        listOf(
            GlassNavigationItem(icon = MiuixIcons.Recording, label = "探索", contentDescription = "探索"),
            GlassNavigationItem(icon = MiuixIcons.WorldClock, label = "历史", contentDescription = "历史"),
            GlassNavigationItem(icon = MiuixIcons.Glass.Item, label = "歌单", contentDescription = "歌单"),
            GlassNavigationItem(icon = MiuixIcons.Glass.SettingsFill, label = "设置", contentDescription = "设置"),
        )
    }

    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 21.dp),
        contentAlignment = Alignment.Center,
    ) {
        key(
            colors.onBackground.toArgb(),
            colors.primary.toArgb(),
            colors.background.toArgb(),
        ) {
            GlassNavigationBar(
                items = items,
                selectedIndex = selectedTab,
                onSelect = onTabSelected,
                backdrop = barBackdrop,
                modifier = Modifier.fillMaxWidth(widthFraction),
                defaultWindowInsetsPadding = false,
                height = BottomFloatingHeight,
                trailingAction = trailingAction,
            )
        }
    }
}
