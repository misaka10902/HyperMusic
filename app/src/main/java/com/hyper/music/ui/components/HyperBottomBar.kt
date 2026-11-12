package com.hyper.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.ui.utils.BottomFloatingHeight
import com.hyper.music.ui.utils.BottomFloatingPadding
import com.hyper.music.ui.utils.BottomFloatingWidthFraction
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.WorldClock
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.NotesFill
import top.yukonga.miuix.kmp.icon.extended.Recording

@Composable
internal fun HyperBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    hyperBackdrop: Backdrop? = null,
    widthFraction: Float = BottomFloatingWidthFraction,
) {
    if (hyperBackdrop == null) return

    val contentColor = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.8f)

    val onSelect: (Int) -> Unit = { onTabSelected(it) }

    var hyperSelectedTab by remember { mutableIntStateOf(selectedTab) }
    LaunchedEffect(selectedTab) { hyperSelectedTab = selectedTab }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 21.dp),
        contentAlignment = Alignment.Center
    ) {
        HyperBottomTabs(
            selectedTabIndex = { hyperSelectedTab },
            onTabSelected = { onSelect(it) },
            backdrop = hyperBackdrop,
            tabsCount = 4,
            containerHeight = BottomFloatingHeight,
            highlightHeight = 44.dp,
            selectorHeight = BottomFloatingHeight - BottomFloatingPadding * 2,
            contentPadding = BottomFloatingPadding,
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(BottomFloatingHeight)
        ) {
            HyperBottomTab({ onSelect(0) }) {
                Image(
                    modifier = Modifier.size(26.dp),
                    imageVector = MiuixIcons.Recording,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Text("探索", fontSize = 11.sp, color = contentColor)
            }
            HyperBottomTab({ onSelect(1) }) {
                Image(
                    modifier = Modifier.size(26.dp),
                    imageVector = MiuixIcons.WorldClock,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Text("历史", fontSize = 11.sp, color = contentColor)
            }
            HyperBottomTab({ onSelect(2) }) {
                Image(
                    modifier = Modifier.size(26.dp),
                    imageVector = MiuixIcons.NotesFill,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Text("歌单", fontSize = 11.sp, color = contentColor)
            }
            HyperBottomTab({ onSelect(3) }) {
                Image(
                    modifier = Modifier.size(26.dp),
                    imageVector = MiuixIcons.ContactsCircle,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Text("定制", fontSize = 11.sp, color = contentColor)
            }
        }
    }
}
