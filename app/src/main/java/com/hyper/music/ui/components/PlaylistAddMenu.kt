package com.hyper.music.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.NativeTextField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Edit
import top.yukonga.miuix.kmp.icon.glass.Install
import top.yukonga.miuix.kmp.icon.glass.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

/**
 * 歌单页右上角「+」弹出的添加菜单。
 *
 * 锚点就是那个「+」按钮在窗口里的 bounds；面板走官方的 [GlassDropdownPopup]，
 * 因此进出场、玻璃底、点击外部收起都与其它下拉菜单一致。
 */
@Composable
internal fun PlaylistAddMenuPopup(
    expanded: Boolean,
    anchorBounds: Rect,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    onImportLink: () -> Unit,
    onImportJson: () -> Unit,
    onCreate: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val mounted = remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) mounted.value = true
    }
    // 收起动画放完再卸载宿主，避免面板在退场途中被直接移除。
    LaunchedEffect(expanded) {
        if (!expanded && mounted.value) {
            delay(260)
            mounted.value = false
        }
    }
    if (!mounted.value) return

    PopupLayout(
        visible = mounted,
        enableWindowDim = false,
        enableBackHandler = false,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        renderInRootScaffold = true,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassDropdownPopup(
                show = expanded,
                onDismissRequest = onDismiss,
                anchorBounds = anchorBounds,
                backdrop = backdrop,
            ) {
                GlassPopupItem(
                    text = "链接导入",
                    icon = MiuixIcons.Glass.Link,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                        onImportLink()
                    },
                )
                GlassPopupItem(
                    text = "JSON 导入",
                    icon = MiuixIcons.Glass.Install,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                        onImportJson()
                    },
                )
                GlassPopupItem(
                    text = "手动创建",
                    icon = MiuixIcons.Glass.Edit,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                        onCreate()
                    },
                )
            }
        }
    }
}

/**
 * 链接导入对话框。
 *
 * 布局自上而下：标题（由 [GlassDialog] 绘制）→ 状态/警告（居中）→ 输入框 → 导入/取消。
 */
@Composable
internal fun PlaylistLinkImportDialog(
    show: Boolean,
    busy: Boolean,
    status: String?,
    error: String?,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var link by remember { mutableStateOf("") }
    LaunchedEffect(show) {
        if (show) link = ""
    }

    val dismiss: () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    GlassDialog(
        title = "链接导入",
        show = show,
        onDismissRequest = { if (!busy) dismiss() },
        backdrop = backdrop?.takeIf { LocalAdvancedMaterialEnabled.current },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ImportStatusLine(status = status, error = error, busy = busy)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
            ) {
                NativeTextField(
                    value = link,
                    onValueChange = { link = it },
                    modifier = Modifier.fillMaxWidth(),
                    hint = "粘贴歌单链接…",
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = scheme.onSurface,
                    ),
                    singleLine = true,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        // 导入过程中也可以点：由调用方中断在跑的导入任务并关闭对话框。
                        dismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = if (busy) "导入中…" else "导入",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        keyboard?.hide()
                        onImport(link.trim())
                    },
                    modifier = Modifier.weight(1f),
                    enabled = link.isNotBlank() && !busy,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

/**
 * JSON 导入进度对话框：选题文件之后、结果回来之前显示，成功即自动关闭。
 */
@Composable
internal fun PlaylistJsonImportDialog(
    show: Boolean,
    busy: Boolean,
    status: String?,
    error: String?,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    GlassDialog(
        title = "JSON 导入",
        show = show,
        onDismissRequest = { if (!busy) onDismiss() },
        backdrop = backdrop?.takeIf { LocalAdvancedMaterialEnabled.current },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ImportStatusLine(status = status, error = error, busy = busy)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = if (busy) "取消" else "关闭",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 状态 / 警告行：居中显示在标题与输入框之间；没有内容时不占高度。 */
@Composable
private fun ImportStatusLine(
    status: String?,
    error: String?,
    busy: Boolean,
) {
    val scheme = MiuixTheme.colorScheme
    val hint = error ?: status
    if (hint.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (error == null && busy) {
            InfiniteProgressIndicator(
                color = scheme.onSurfaceSecondary,
                size = 16.dp,
                strokeWidth = 2.dp,
                orbitingDotSize = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = hint,
            color = if (error != null) scheme.error else scheme.onSurfaceSecondary,
            fontSize = 13.sp,
        )
    }
}
