package com.hyper.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.ui.utils.isAppDarkTheme
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val TabletTabHeight = 46.dp
private val TabletTabPadding = 3.dp
private val TabletTabWidth = 92.dp

@Composable
fun TabletTopBar(
    titles: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    progressiveBlur: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
) {
    val rawBackdrop = backdrop.takeIf { isRuntimeShaderSupported() }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val progressiveBackdrop = if (progressiveBlur && rawBackdrop != null) {
        rememberLayerBackdrop {
            val c = coordinates?.takeIf { it.isAttached }
            with(rawBackdrop) { drawBackdrop(this@rememberLayerBackdrop, c) }
            drawContent()
        }
    } else {
        null
    }
    val density = LocalDensity.current
    val rampPx = with(density) { GlassTopAppBarDefaults.ProgressiveBlurRampDistance.toPx() }
    
    val bandAlpha = {
        (-(scrollBehavior?.state?.contentOffset ?: 0f) / rampPx).coerceIn(0f, 1f)
    }
    val bandBrush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface)
    
    val blurTint = GlassTopAppBarDefaults.blurTintBrush(MiuixTheme.colorScheme.surface)

    Box(modifier = modifier.fillMaxWidth()) {
        
        if (progressiveBackdrop != null && rawBackdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .onGloballyPositioned { coordinates = it }
                    .graphicsLayer { alpha = bandAlpha() }
                    .layerBackdrop(progressiveBackdrop)
                    .progressiveTextureBlur(
                        backdrop = rawBackdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
                        blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value,
                        colors = BlurDefaults.blurColors(),
                    )
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = blurTint)
                    },
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = bandAlpha() }
                    .background(bandBrush),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, bottom = 10.dp),
        ) {
            TabletTopTabs(
                titles = titles,
                selectedIndex = selectedTab,
                onSelected = onTabSelected,
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.Center),
            )
            if (actions != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    actions()
                }
            }
        }
    }
}

@Composable
private fun TabletTopTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val dark = isAppDarkTheme()

    HyperBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = onSelected,
        backdrop = backdrop,
        tabsCount = titles.size,
        modifier = modifier
            .width(TabletTabWidth * titles.size)
            .height(TabletTabHeight),
        containerHeight = TabletTabHeight,
        highlightHeight = TabletTabHeight - TabletTabPadding * 2,
        selectorHeight = TabletTabHeight - TabletTabPadding * 2,
        contentPadding = TabletTabPadding,
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            HyperBottomTab({ onSelected(index) }) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = scheme.onSurface.copy(
                        alpha = if (selected) (if (dark) 1f else 0.92f) else 0.58f
                    ),
                )
            }
        }
    }
}
