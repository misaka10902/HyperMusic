// 移植自 miuix v0.9.5-beta01: example/shared/src/commonMain/kotlin/GlassSearchOverlay.kt
// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// 适配说明：
//  - 包名改为项目自有包，符号改为 internal（仅本项目使用）。
//  - 移除 example 自有的 LocalAppPageInsets，顶部安全区改用 WindowInsets.statusBars.only(Top)。
//  - 搜索框改用项目自研轻量 NativeTextField（Android EditText 包装），不再使用官方
//    androidx.compose.foundation.text.BasicTextField：后者会带入 CoreTextField 整套
//    selection / contextMenu / magnifier / drag 类簇（约 218 个类），R8 无法按调用路径裁剪。
package com.hyper.music.ui.search

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NativeTextField
import top.yukonga.miuix.kmp.basic.rememberNativeTextFieldHandle
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Close
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

private const val SEARCH_TOP_BAR_BLUR_RADIUS = 50f

private val SearchModeEasing: Easing = run {
    val omega = 2.0 * PI / 0.75f
    val stiffness = (omega * omega).toFloat()
    val damping = (0.98f * 2.0 * omega).toFloat()
    val decay = -damping / 2f
    val frequency = sqrt(-(damping.toDouble() * damping - stiffness.toDouble() * 4)).toFloat() / 2f
    val sineCoefficient = decay / frequency
    Easing { fraction ->
        (
            1.0 + exp((decay * fraction).toDouble()) *
                (-cos((frequency * fraction).toDouble()) + sineCoefficient * sin((frequency * fraction).toDouble()))
            ).toFloat()
    }
}

/**
 * 全屏玻璃搜索覆盖层。整个搜索模式由 [expanded] 单一布尔状态驱动，退出动画期间保持组合，
 * 使 Cancel 能逆向播放进场动画。搜索框下方的 [content] 用于放置搜索历史 / 结果 / TabRow。
 */
@Composable
internal fun GlassSearchOverlay(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    label: String = "搜索歌曲、歌手和专辑...",
    content: @Composable ColumnScope.(String) -> Unit = {},
) {
    val transition = updateTransition(expanded, label = "glassSearch")
    val progress = transition.animateFloat(
        transitionSpec = { tween(400, easing = SearchModeEasing) },
        label = "glassSearchGeometry",
    ) { if (it) 1f else 0f }
    val cancelAlpha = transition.animateFloat(
        transitionSpec = { tween(150, delayMillis = if (targetState) 100 else 0, easing = LinearEasing) },
        label = "glassSearchCancelMaterial",
    ) { if (it) 1f else 0f }
    var query by remember { mutableStateOf("") }
    val fieldHandle = rememberNativeTextFieldHandle()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val topInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top)
    val active = expanded || transition.currentState || transition.isRunning
    LaunchedEffect(expanded) {
        // 只在「打开」时清空输入。关闭时保留 query，退场动画期间搜索内容不消失，
        // 不会闪一下空的搜索结果（没有歌曲/歌手/专辑）。
        if (expanded) {
            query = ""
            // NativeTextField 是 AndroidView：首帧组合时 EditText 还没 attach 到窗口，
            // 此时 focus / showSoftInput 都会落空；而且 requestFocus 本身并不弹键盘，
            // 必须显式 showSoftInput。两个一起做，并在 attach 完成前重试几次。
            // 前 3 次无条件重试，覆盖「已拿到焦点但输入法还没起来」的情况。
            repeat(8) { attempt ->
                fieldHandle.requestFocusAndShowIme()
                delay(50)
                if (attempt >= 2 && fieldHandle.editText?.isFocused == true) return@LaunchedEffect
            }
        } else if (active) {
            focusManager.clearFocus()
            fieldHandle.clearFocus()
            keyboard?.hide()
        }
    }
    if (!active) return
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = expanded,
        onBackCompleted = onDismissRequest,
    )
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent {
                if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) {
                    onDismissRequest()
                    true
                } else {
                    false
                }
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(interactionSource = null, indication = null) { keyboard?.hide() },
        )
        // 内容层是整屏坐标（从屏幕 (0,0) 开始）：结果列表能一路滚到屏幕顶部、
        // 钻到搜索框与 TabRow 背后，顶部的模糊带也从屏幕最顶端开始渐隐。
        // alpha 跟随同一个 progress，退出时与搜索框一起淡出，不会滞留在首页上。
        // 这里不挂 imePadding：键盘弹起时这一列不能被压缩。结果页的采样底面画着壁纸，
        // 列一压缩壁纸的裁剪窗口就跟着变矮，看起来就像壁纸被键盘顶上去了。
        // 键盘避让由各形态自己处理（列表挂 imePadding），壁纸因此始终钉在屏幕上。
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .graphicsLayer { alpha = progress.value },
        ) {
            content(query)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
                .graphicsLayer { alpha = progress.value }
                .windowInsetsPadding(topInsets)
                .imePadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassTopAppBarDefaults.HorizontalPadding, vertical = 4.dp)
                    .graphicsLayer { translationY = -topInsets.getTop(this) * (1f - progress.value) }
                    // 搜索结果可以从屏幕顶端滚到搜索框背后，搜索框必须画在它上面。
                    .zIndex(2f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 给右侧独立的圆形关闭键让出位置（44dp 按钮 + 8dp 间隔），
                        // 搜索框与关闭键因此是分离的两个圆角块，而不是连成一条。
                        .padding(end = 52.dp * progress.value)
                        .height(44.dp)
                        .background(colors.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(MiuixIcons.Glass.Search, contentDescription = null, modifier = Modifier.size(28.dp), tint = colors.disabledOnSurface)
                    NativeTextField(
                        value = query,
                        onValueChange = { query = it },
                        hint = label,
                        textStyle = MiuixTheme.textStyles.body1.copy(color = colors.onSurface),
                        singleLine = true,
                        enabled = expanded,
                        autoFocus = expanded,
                        handle = fieldHandle,
                        onDone = { keyboard?.hide() },
                        modifier = Modifier
                            .weight(1f)
                            .layout { measurable, constraints ->
                                // 右侧 Cancel 按钮滑入时预留 54dp，避免文字钻到按钮底下
                                val endSpace = (54.dp.toPx() * progress.value).toInt()
                                val width = (constraints.maxWidth - endSpace).coerceAtLeast(0)
                                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                                layout(constraints.maxWidth, placeable.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            },
                    )
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                query = ""
                                fieldHandle.requestFocus()
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(MiuixIcons.Glass.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp * (7f / 6f)), tint = colors.disabledOnSurface)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        val fraction = progress.value
                        translationX = 66.dp.toPx() * (1f - fraction)
                        scaleX = fraction
                        scaleY = fraction
                        alpha = cancelAlpha.value
                        val blur = 50f * (1f - cancelAlpha.value)
                        renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
                    },
            ) {
                IconButton(
                    onClick = onDismissRequest,
                    enabled = expanded,
                    modifier = Modifier.size(44.dp).background(colors.surfaceContainerHigh, CircleShape),
                ) {
                    Icon(MiuixIcons.Glass.Close, contentDescription = "Cancel", modifier = Modifier.size(28.dp), tint = colors.onSurface)
                }
            }
            }
        }
    }
}

/** 页面整体透明度：与顶栏采样背景保持同步。 */
@Composable
internal fun rememberGlassSearchBarAlpha(expanded: Boolean): State<Float> {
    val transition = updateTransition(expanded, label = "glassSearchTopBarOpacity")
    return transition.animateFloat(
        transitionSpec = {
            tween(150, easing = if (targetState) CubicBezierEasing(0f, 0f, 0.67f, 1f) else CubicBezierEasing(0.26f, 0f, 0.6f, 0.2f))
        },
        label = "glassSearchTopBarAlpha",
    ) { if (it) 0f else 1f }
}

/** 顶栏内容退场：淡出 + 缩到 0.9 + 高斯模糊。 */
@Composable
internal fun Modifier.glassSearchBarTransition(expanded: Boolean, barAlpha: () -> Float): Modifier {
    val density = LocalDensity.current
    val layerOutset = remember(density) {
        val shadow = GlassShadows.Regular
        val shadowOutset = ((shadow.radius + maxOf(abs(shadow.offsetX), abs(shadow.offsetY))) / GlassDefaults.SourceDensity).dp
        with(density) { shadowOutset.toPx() } + SEARCH_TOP_BAR_BLUR_RADIUS * 3f
    }
    val transition = updateTransition(expanded, label = "glassSearchTopBar")
    val scale = transition.animateFloat(
        transitionSpec = { tween(400, easing = SearchModeEasing) },
        label = "glassSearchTopBarScale",
    ) { if (it) 0.9f else 1f }
    val blur = transition.animateFloat(
        transitionSpec = { tween(if (targetState) 150 else 120, easing = LinearEasing) },
        label = "glassSearchTopBarBlur",
    ) { if (it) SEARCH_TOP_BAR_BLUR_RADIUS else 0f }
    return drawWithCache {
        val layer = obtainGraphicsLayer()
        val halo = ceil(layerOutset).toInt()
        val layerSize = IntSize(size.width.toInt() + halo * 2, size.height.toInt() + halo * 2)
        onDrawWithContent {
            val opacity = barAlpha().coerceIn(0f, 1f)
            val currentScale = scale.value
            val currentBlur = blur.value
            if (opacity > 0f) {
                if (opacity == 1f && currentScale == 1f && currentBlur == 0f) {
                    drawContent()
                } else {
                    layer.record(density = this@drawWithCache, size = layerSize) {
                        translate(halo.toFloat(), halo.toFloat()) { this@record.drawContent() }
                    }
                    layer.alpha = opacity
                    layer.scaleX = currentScale
                    layer.scaleY = currentScale
                    layer.renderEffect = if (currentBlur > 0.5f) BlurEffect(currentBlur, currentBlur, TileMode.Decal) else null
                    translate(-halo.toFloat(), -halo.toFloat()) { drawLayer(layer) }
                }
            }
        }
    }
}
