package com.hyper.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.ui.effects.edgelight.edgeLight
import com.hyper.music.ui.effects.edgelight.rememberDefaultEdgeLight
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.BottomFloatingHeight
import com.hyper.music.ui.utils.isAppDarkTheme
import com.hyper.music.ui.utils.BottomFloatingWidthFraction
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassCapsule
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.hyper.music.ui.utils.BottomFloatingPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import android.os.Build

private val SelectionBarBottomPadding = 21.dp - 0.5.dp

data class SelectionAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color = Color.Unspecified,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

data class SelectionBarState(
    val count: Int,
    val actions: List<SelectionAction>,
    val singleAction: SelectionAction? = null,
    val selectAllAction: SelectionAction? = null,
    val onExit: () -> Unit,
)

@Composable
fun SelectionActionBar(
    state: SelectionBarState,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = SelectionBarBottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        val maxBarWidth = maxWidth * BottomFloatingWidthFraction
        val pitch = (maxBarWidth - BottomFloatingPadding * 2) / 4
        val buttonCount = 1 + state.actions.size
        val targetWidth = (pitch * buttonCount)
            .coerceIn(BottomFloatingHeight, maxBarWidth)
        val barWidth by animateFloatAsState(
            targetValue = targetWidth.value,
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 260f),
            label = "selectionBarWidth",
        )

        Box(
            modifier = Modifier
                .width(barWidth.dp)
                .height(BottomFloatingHeight)
                .floatingBarShadow(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .width(barWidth.dp)
                    .height(BottomFloatingHeight)
                    .then(selectionBarSurface(backdrop)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectionBarButton(
                    icon = MiuixIcons.Close,
                    label = "取消",
                    tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                    onClick = state.onExit,
                    modifier = Modifier.width(pitch),
                )
                state.actions.forEach { action ->
                    SelectionBarButton(
                        icon = action.icon,
                        label = action.label,
                        tint = if (action.tint == Color.Unspecified) {
                            MiuixTheme.colorScheme.onSurface
                        } else {
                            action.tint
                        },
                        enabled = action.enabled,
                        onClick = action.onClick,
                        modifier = Modifier.width(pitch),
                    )
                }
            }
        }
    }
}

@Composable
private fun selectionBarSurface(backdrop: Backdrop?): Modifier {
    val advanced = LocalAdvancedMaterialEnabled.current
    val useBlur = advanced && backdrop != null && Build.VERSION.SDK_INT >= 33
    val surfaceColor = if (useBlur) {
        MiuixTheme.colorScheme.surfaceContainer.copy(
            alpha = if (isAppDarkTheme()) 0.8f else 0.72f,
        )
    } else {
        hyperSurfaceColor().copy(alpha = 1f)
    }

    val edgeLight = rememberDefaultEdgeLight()
    if (useBlur) {
        return Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { GlassCapsule() },
            effects = { blur(28.dp.toPx()) },
            onDrawSurface = { drawRect(color = surfaceColor) },
        ).edgeLight(shape = GlassCapsule(), edgeLight = edgeLight)
    }
    return Modifier.background(
        color = surfaceColor,
        shape = GlassCapsule(),
    ).edgeLight(shape = GlassCapsule(), edgeLight = edgeLight)
}

@Composable
private fun SelectionBarButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {

    val displayTint = if (enabled) {
        tint.copy(alpha = 0.92f)
    } else {
        MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.38f)
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(displayTint),
        )
        Text(
            text = label,
            color = displayTint,
            fontSize = 11.sp,
        )
    }
}
