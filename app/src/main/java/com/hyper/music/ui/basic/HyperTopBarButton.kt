package com.hyper.music.ui.basic

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HyperTopBarButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    iconOffset: DpOffset = DpOffset.Zero,
    buttonHeight: Dp = 42.dp,
    backdropAlpha: Float = 1f,
    shadowAlpha: Float = 1f,
    iconTint: Color = Color.Unspecified,
    iconRotation: Float = 0f,
) {
    val tint = if (iconTint == Color.Unspecified) {
        MiuixTheme.colorScheme.onSurface
    } else {
        iconTint
    }

    GlassIconButton(
        onClick = onClick,
        backdrop = backdrop,
        size = buttonHeight,
        surfaceAlpha = backdropAlpha,
        modifier = modifier,
        content = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(iconSize)
                    .offset(iconOffset.x, iconOffset.y)
                    .graphicsLayer { rotationZ = iconRotation },
                tint = tint,
            )
        },
    )
}
