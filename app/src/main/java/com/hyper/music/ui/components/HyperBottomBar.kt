package com.hyper.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

/**
 * 底部导航条。
 *
 * 直接使用 miuix 官方的 [GlassNavigationBar]：
 * - 选中胶囊用左右两个独立弹簧（`navIndicator` / `navIndicatorTrail`）驱动，跟手且能拉伸；
 * - 尾部动作（搜索按钮）的宽度、间距、缩放、透明度由 [GlassNavigationBar] 的布局统一动画，
 *   不需要在外面再手搓 `animateDpAsState`，因此不会有一帧延迟；
 * - 胶囊体与阴影走 `glassPanel`，阴影沿轮廓绘制，不会出现方形切边。
 */
@Composable
internal fun HyperBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    hyperBackdrop: Backdrop? = null,
    widthFraction: Float = BottomFloatingWidthFraction,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    if (hyperBackdrop == null) return

    val items = remember {
        listOf(
            GlassNavigationItem(icon = MiuixIcons.Recording, label = "探索", contentDescription = "探索"),
            GlassNavigationItem(icon = MiuixIcons.WorldClock, label = "历史", contentDescription = "历史"),
            GlassNavigationItem(icon = MiuixIcons.Glass.Item, label = "歌单", contentDescription = "歌单"),
            GlassNavigationItem(icon = MiuixIcons.Glass.SettingsFill, label = "设置", contentDescription = "设置"),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 21.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassNavigationBar(
            items = items,
            selectedIndex = selectedTab,
            onSelect = onTabSelected,
            backdrop = hyperBackdrop,
            modifier = Modifier.fillMaxWidth(widthFraction),
            defaultWindowInsetsPadding = false,
            height = BottomFloatingHeight,
            trailingAction = trailingAction,
        )
    }
}
