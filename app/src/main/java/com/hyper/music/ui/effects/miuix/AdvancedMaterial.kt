package com.hyper.music.ui.effects.miuix

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.hyper.music.music.MusicLibrary
import top.yukonga.miuix.kmp.blur.Backdrop

val LocalAdvancedMaterialEnabled = staticCompositionLocalOf { true }

/**
 * 页面**内部**玻璃（卡片、页面内 tab / 搜索栏）采样的层：只录壁纸。
 * 这些组件本身就在页面内容层里，若采样内容层会「自己采样自己」→ 显示列表自引用
 * → 渲染线程无限递归、native 崩溃。所以它们只采样壁纸层（与 miuix demo 里
 * 列表内 tab 采 wallpaperBackdrop 是同一处理）。
 */
val LocalActiveBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 当前可见页面的**内容层**（壁纸 + 页面内容），供 popup / bottomSheet / dialog 采样。
 *
 * 弹层画在页面之上、录制范围之外，所以采样内容层不会自引用；用它才能让弹层的
 * 玻璃模糊跟背后真实内容走。之前这里错用了只含壁纸的 [LocalActiveBackdrop]，
 * 二级界面的弹层就会采到「另一页的静态壁纸」。
 */
val LocalPageBackdrop = staticCompositionLocalOf<androidx.compose.runtime.State<Backdrop?>?> { null }

@Composable
fun rememberAdvancedMaterialEnabled(): Boolean {
    val settings by MusicLibrary.settings.collectAsState()
    return settings.advancedMaterial
}

/**
 * 浮层（bottomSheet / dialog / dropdown）该采样的层：优先用当前页面内容层，
 * 拿不到时退回壁纸层。
 */
@Composable
fun rememberPopupBackdrop(): Backdrop? =
    LocalPageBackdrop.current?.value ?: LocalActiveBackdrop.current
