package com.hyper.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConfirmDialog(
    show: Boolean,
    title: String,
    summary: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "删除",
    backdrop: Backdrop? = null,
) {
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current



    GlassDialog(
        show = show,
        onDismissRequest = onDismiss,
        backdrop = backdrop?.takeIf { LocalAdvancedMaterialEnabled.current },
        title = title,
        summary = summary,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = "取消",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = confirmText,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    color = scheme.error,
                    disabledColor = scheme.error,
                    textColor = Color.White,
                    disabledTextColor = Color.White,
                ),
            )
        }
    }
}
