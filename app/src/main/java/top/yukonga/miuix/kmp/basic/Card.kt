package top.yukonga.miuix.kmp.basic

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.interfaces.HoldDownObserver
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.TiltFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
@NonRestartableComposable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardDefaults.CornerRadius,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        colors = colors,
    ) {
        Column(
            modifier = Modifier.padding(insideMargin),
            content = content,
        )
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardDefaults.CornerRadius,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    HoldDownObserver(holdDownState, interactionSource)

    val pressFeedback = remember(pressFeedbackType) {
        when (pressFeedbackType) {
            PressFeedbackType.None -> null
            PressFeedbackType.Sink -> SinkFeedback()
            PressFeedbackType.Tilt -> TiltFeedback()
        }
    }

    val usedInteractionSource = if (pressFeedback != null) interactionSource else null
    val indicationToUse = if (showIndication) LocalIndication.current else null

    val hasOnClick = onClick != null
    val hasLongPress = onLongPress != null
    val isClickable = hasOnClick || hasLongPress
    val clickableModifier = remember(isClickable, hasLongPress, interactionSource, indicationToUse) {
        if (isClickable) {
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = indicationToUse,
                onClick = { currentOnClick?.invoke() },
                onLongClick = if (hasLongPress) {
                    { currentOnLongPress?.invoke() }
                } else {
                    null
                },
            )
        } else {
            Modifier
        }
    }

    BasicCard(
        modifier = modifier.pressable(
            interactionSource = usedInteractionSource,
            indication = pressFeedback,
            delay = null,
        ),
        cornerRadius = cornerRadius,
        colors = colors,
    ) {
        Column(
            modifier = Modifier
                .then(clickableModifier)
                .padding(insideMargin),
            content = content,
        )
    }
}

@Composable
private fun BasicCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.defaultColors(),
    cornerRadius: Dp = CardDefaults.CornerRadius,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides colors.contentColor,
    ) {
        Box(
            modifier = modifier
                .semantics(mergeDescendants = false) {
                    isTraversalGroup = true
                }
                .squircleSurface(color = colors.color, cornerRadius = cornerRadius),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}

object CardDefaults {

    val CornerRadius = 16.dp

    val InsideMargin = PaddingValues(0.dp)

    @Composable
    fun defaultColors(
        color: Color = MiuixTheme.colorScheme.surfaceContainer,
        contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    ): CardColors = remember(color, contentColor) {
        CardColors(
            color = color,
            contentColor = contentColor,
        )
    }
}

@Immutable
data class CardColors(
    val color: Color,
    val contentColor: Color,
)
