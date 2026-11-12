


package top.yukonga.miuix.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BasicTooltipDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds


@JvmInline
value class TooltipAnchorPosition private constructor(private val value: Int) {
    override fun toString(): String = when (this) {
        Above -> "Above"
        Below -> "Below"
        Left -> "Left"
        Right -> "Right"
        Start -> "Start"
        End -> "End"
        else -> "Invalid"
    }

    companion object {
        val Above = TooltipAnchorPosition(0)
        val Below = TooltipAnchorPosition(1)
        val Left = TooltipAnchorPosition(2)
        val Right = TooltipAnchorPosition(3)
        val Start = TooltipAnchorPosition(4)
        val End = TooltipAnchorPosition(5)
    }
}


@Stable
sealed interface TooltipScope {

    val positioning: TooltipAnchorPosition


    fun obtainAnchorBounds(): LayoutCoordinates?
}

private class TooltipScopeImpl(
    override val positioning: TooltipAnchorPosition,
    private val anchorBounds: () -> LayoutCoordinates?,
) : TooltipScope {
    override fun obtainAnchorBounds(): LayoutCoordinates? = anchorBounds()
}


@Stable
interface TooltipState {

    val transition: MutableTransitionState<Boolean>


    val isVisible: Boolean


    val isPersistent: Boolean


    suspend fun show(mutatePriority: MutatePriority = MutatePriority.Default)


    fun dismiss()


    fun onDispose()
}

@OptIn(ExperimentalFoundationApi::class)
@Stable
private class TooltipStateImpl(
    initialIsVisible: Boolean,
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex,
) : TooltipState {
    override val transition: MutableTransitionState<Boolean> =
        MutableTransitionState(initialIsVisible)

    override val isVisible: Boolean




        get() = transition.targetState || !transition.isIdle


    private var job: (CancellableContinuation<Unit>)? = null


    override suspend fun show(mutatePriority: MutatePriority) {
        val cancellableShow: suspend () -> Unit = {
            suspendCancellableCoroutine { continuation ->
                transition.targetState = true
                job = continuation
            }
        }



        mutatorMutex.mutate(mutatePriority) {
            try {
                if (isPersistent || mutatePriority == MutatePriority.UserInput) {
                    cancellableShow()
                } else {
                    withTimeout(BasicTooltipDefaults.TooltipDuration.milliseconds) { cancellableShow() }
                }
            } finally {
                if (mutatePriority != MutatePriority.PreventUserInput) {

                    dismiss()
                }
            }
        }
    }


    override fun dismiss() {
        transition.targetState = false
        if (isPersistent) {
            job?.cancel()
        }
    }


    override fun onDispose() {
        job?.cancel()
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = false,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex,
): TooltipState = remember(isPersistent, mutatorMutex) {
    TooltipStateImpl(initialIsVisible, isPersistent, mutatorMutex)
}


private val TooltipExitScaleSpec: FiniteAnimationSpec<Float> =
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium, visibilityThreshold = 0.0001f)


@Composable
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    var anchorBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val positioning = (positionProvider as? TooltipPopupPositionProvider)?.positioning ?: TooltipAnchorPosition.Below
    val scope = remember(positioning) { TooltipScopeImpl(positioning) { anchorBounds } }

    TooltipBoxImpl(
        positionProvider = positionProvider,
        tooltip = {
            AnimatedVisibility(
                visibleState = state.transition,
                enter = fadeIn(ListPopupDefaults.AlphaEnterAnimationSpec) +
                    scaleIn(initialScale = 0.9f, animationSpec = ListPopupDefaults.FractionAnimationSpec),
                exit = fadeOut(ListPopupDefaults.AlphaExitAnimationSpec) +
                    scaleOut(targetScale = 0.9f, animationSpec = TooltipExitScaleSpec),
            ) {
                scope.tooltip()
            }
        },
        state = state,
        modifier = modifier.onGloballyPositioned { anchorBounds = it },
        focusable = focusable,
        enableUserInput = enableUserInput,
        content = content,
    )
}

private const val TOOLTIP_LONG_PRESS_LABEL = "Show tooltip"
private const val TOOLTIP_PANE_TITLE = "Tooltip"


@Composable
private fun TooltipBoxImpl(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable () -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .tooltipGestures(enableUserInput, state)
            .tooltipAnchorSemantics(enableUserInput, state, scope),
    ) {
        if (state.isVisible) {
            TooltipPopup(
                positionProvider = positionProvider,
                state = state,
                focusable = focusable,
                content = tooltip,
            )
        }
        content()
    }

    DisposableEffect(state) { onDispose { state.onDispose() } }
}

@Composable
private fun TooltipPopup(
    positionProvider: PopupPositionProvider,
    state: TooltipState,
    focusable: Boolean,
    content: @Composable () -> Unit,
) {
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = { if (state.isVisible) state.dismiss() },
        properties = PopupProperties(focusable = focusable),
    ) {
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Assertive
                paneTitle = TOOLTIP_PANE_TITLE
            },
        ) {
            content()
        }
    }
}


private fun Modifier.tooltipGestures(enabled: Boolean, state: TooltipState): Modifier = if (enabled) {
    this
        .pointerInput(state) {
            coroutineScope {
                awaitEachGesture {
                    val pass = PointerEventPass.Initial
                    val inputType = awaitFirstDown(pass = pass).type
                    if (inputType == PointerType.Touch || inputType == PointerType.Stylus) {
                        try {
                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                waitForUpOrCancellation(pass = pass)
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {


                            launch { state.show(MutatePriority.UserInput) }
                            val changes = awaitPointerEvent(pass = pass).changes
                            for (i in changes.indices) {
                                changes[i].consume()
                            }
                        }
                    }
                }
            }
        }
        .pointerInput(state) {
            coroutineScope {
                awaitPointerEventScope {
                    val pass = PointerEventPass.Main
                    while (true) {
                        val event = awaitPointerEvent(pass)
                        if (event.changes[0].type == PointerType.Mouse) {
                            when (event.type) {
                                PointerEventType.Enter ->
                                    launch { state.show(MutatePriority.UserInput) }

                                PointerEventType.Exit ->
                                    if (!state.isPersistent) state.dismiss()
                            }
                        }
                    }
                }
            }
        }
} else {
    this
}

private fun Modifier.tooltipAnchorSemantics(
    enabled: Boolean,
    state: TooltipState,
    scope: CoroutineScope,
): Modifier = if (enabled) {
    this.semantics(mergeDescendants = true) {
        onLongClick(
            label = TOOLTIP_LONG_PRESS_LABEL,
            action = {
                scope.launch { state.show() }
                true
            },
        )
    }
} else {
    this
}


@Composable
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretShape: Shape? = null,
    maxWidth: Dp = TooltipDefaults.PlainTooltipMaxWidth,
    cornerRadius: Dp = TooltipDefaults.PlainTooltipCornerRadius,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    insideMargin: PaddingValues = TooltipDefaults.PlainTooltipInsideMargin,
    content: @Composable () -> Unit,
) {
    TooltipSurface(
        caretShape = caretShape,
        maxWidth = maxWidth,
        cornerRadius = cornerRadius,
        containerColor = containerColor,
        insideMargin = insideMargin,
        modifier = modifier.semantics(mergeDescendants = false) {
            isTraversalGroup = true
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}


@Composable
fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretShape: Shape? = null,
    maxWidth: Dp = TooltipDefaults.RichTooltipMaxWidth,
    cornerRadius: Dp = TooltipDefaults.RichTooltipCornerRadius,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    insideMargin: PaddingValues = TooltipDefaults.RichTooltipInsideMargin,
    text: @Composable () -> Unit,
) {
    TooltipSurface(
        caretShape = caretShape,
        maxWidth = maxWidth,
        cornerRadius = cornerRadius,
        containerColor = colors.containerColor,
        insideMargin = insideMargin,
        modifier = modifier.semantics(mergeDescendants = false) {
            isTraversalGroup = true
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                CompositionLocalProvider(LocalContentColor provides colors.titleContentColor) {
                    title()
                }
            }
            CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                text()
            }
            if (action != null) {
                Box(modifier = Modifier.align(Alignment.End)) {
                    CompositionLocalProvider(LocalContentColor provides colors.actionContentColor) {
                        action()
                    }
                }
            }
        }
    }
}

@Composable
private fun TooltipScope.TooltipSurface(
    caretShape: Shape?,
    maxWidth: Dp,
    cornerRadius: Dp,
    containerColor: Color,
    insideMargin: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val drawCaret = caretShape != null &&
        (positioning == TooltipAnchorPosition.Above || positioning == TooltipAnchorPosition.Below)
    val caretOnTop = positioning == TooltipAnchorPosition.Below
    val caretSize = TooltipDefaults.caretSize
    var selfBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .widthIn(max = maxWidth)
            .onGloballyPositioned { selfBounds = it }
            .then(
                if (drawCaret) {
                    Modifier.drawBehind {
                        val anchor = obtainAnchorBounds() ?: return@drawBehind
                        val self = selfBounds ?: return@drawBehind
                        if (!anchor.isAttached || !self.isAttached) return@drawBehind
                        val anchorCenterX = anchor.positionInWindow().x + anchor.size.width / 2f
                        val selfX = self.positionInWindow().x
                        val caretHalf = caretSize.width.toPx() / 2f
                        val cr = cornerRadius.toPx()
                        val centerX = (anchorCenterX - selfX)
                            .coerceIn(cr + caretHalf, size.width - cr - caretHalf)
                        val caretHeight = caretSize.height.toPx()
                        val path = Path()
                        if (caretOnTop) {
                            path.moveTo(centerX, 0f)
                            path.lineTo(centerX - caretHalf, caretHeight)
                            path.lineTo(centerX + caretHalf, caretHeight)
                        } else {
                            path.moveTo(centerX, size.height)
                            path.lineTo(centerX - caretHalf, size.height - caretHeight)
                            path.lineTo(centerX + caretHalf, size.height - caretHeight)
                        }
                        path.close()
                        drawPath(path, color = containerColor)
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(
                    top = if (drawCaret && caretOnTop) caretSize.height else 0.dp,
                    bottom = if (drawCaret && !caretOnTop) caretSize.height else 0.dp,
                )
                .dropShadow(
                    shape = RoundedCornerShape(cornerRadius),
                    shadow = Shadow(radius = 10.dp, color = Color.Black, alpha = 0.1f),
                )
                .squircleBackground(color = containerColor, cornerRadius = cornerRadius)
                .padding(insideMargin),
        ) {
            content()
        }
    }
}


@Composable
fun TooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = false),
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
        tooltip = {
            PlainTooltip(containerColor = containerColor, contentColor = contentColor) {
                Text(
                    text = text,
                    color = contentColor,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        state = state,
        modifier = modifier,
        focusable = false,
        enableUserInput = enabled,
        content = content,
    )
}


@Composable
fun RichTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    title: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    positionProvider: PopupPositionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = positionProvider,
        tooltip = {
            RichTooltip(
                title = title?.let {
                    {
                        Text(
                            text = it,
                            color = colors.titleContentColor,
                            style = MiuixTheme.textStyles.subtitle,
                        )
                    }
                },
                action = actionText?.let {
                    {
                        TextButton(
                            text = it,
                            onClick = {
                                onActionClick?.invoke()
                                state.dismiss()
                            },
                            minWidth = 0.dp,
                            minHeight = 0.dp,
                            cornerRadius = TooltipDefaults.RichTooltipActionCornerRadius,
                            colors = ButtonDefaults.textButtonColors(
                                color = Color.Transparent,
                                textColor = colors.actionContentColor,
                            ),
                            insideMargin = TooltipDefaults.RichTooltipActionInsideMargin,
                        )
                    }
                },
                colors = colors,
            ) {
                Text(
                    text = text,
                    color = colors.contentColor,
                    style = MiuixTheme.textStyles.body2,
                )
            }
        },
        state = state,
        modifier = modifier,
        focusable = true,
        enableUserInput = enabled,
        content = content,
    )
}


@Immutable
data class RichTooltipColors(
    val containerColor: Color,
    val contentColor: Color,
    val titleContentColor: Color,
    val actionContentColor: Color,
)


@Immutable
private class TooltipPopupPositionProvider(
    val positioning: TooltipAnchorPosition,
    private val spacing: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val resolved = positioning.resolve(layoutDirection)
        val centerX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val centerY = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2

        var x = centerX
        var y = centerY
        when (resolved) {
            TooltipAnchorPosition.Below -> {
                val below = anchorBounds.bottom + spacing
                val above = anchorBounds.top - popupContentSize.height - spacing
                y = if (below + popupContentSize.height <= windowSize.height || above < 0) below else above
            }

            TooltipAnchorPosition.Above -> {
                val above = anchorBounds.top - popupContentSize.height - spacing
                val below = anchorBounds.bottom + spacing
                y = if (above >= 0 || below + popupContentSize.height > windowSize.height) above else below
            }

            TooltipAnchorPosition.Left -> {
                val left = anchorBounds.left - popupContentSize.width - spacing
                val right = anchorBounds.right + spacing
                x = if (left >= 0 || right + popupContentSize.width > windowSize.width) left else right
            }

            TooltipAnchorPosition.Right -> {
                val right = anchorBounds.right + spacing
                val left = anchorBounds.left - popupContentSize.width - spacing
                x = if (right + popupContentSize.width <= windowSize.width || left < 0) right else left
            }

            else -> {
                y = anchorBounds.bottom + spacing
            }
        }

        x = x.coerceIn(0, max(0, windowSize.width - popupContentSize.width))
        y = y.coerceIn(0, max(0, windowSize.height - popupContentSize.height))
        return IntOffset(x, y)
    }
}

private fun TooltipAnchorPosition.resolve(layoutDirection: LayoutDirection): TooltipAnchorPosition = when (this) {
    TooltipAnchorPosition.Start ->
        if (layoutDirection == LayoutDirection.Ltr) TooltipAnchorPosition.Left else TooltipAnchorPosition.Right

    TooltipAnchorPosition.End ->
        if (layoutDirection == LayoutDirection.Ltr) TooltipAnchorPosition.Right else TooltipAnchorPosition.Left

    else -> this
}


object TooltipDefaults {

    val SpacingBetweenTooltipAndAnchor = 8.dp


    val caretSize = DpSize(16.dp, 8.dp)


    val PlainTooltipMaxWidth = 200.dp


    val PlainTooltipCornerRadius = 12.dp


    val PlainTooltipInsideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)


    val plainTooltipContainerColor: Color
        @Composable get() = MiuixTheme.colorScheme.onSecondaryVariant


    val plainTooltipContentColor: Color
        @Composable get() = MiuixTheme.colorScheme.secondaryVariant


    val RichTooltipMaxWidth = 320.dp


    val RichTooltipCornerRadius = 16.dp


    val RichTooltipInsideMargin = PaddingValues(all = 16.dp)


    val RichTooltipActionCornerRadius = 8.dp


    val RichTooltipActionInsideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp)


    fun caretShape(): Shape = TooltipCaretShape

    @Composable
    fun richTooltipColors(
        containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
        contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        titleContentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
        actionContentColor: Color = MiuixTheme.colorScheme.primary,
    ): RichTooltipColors = remember(containerColor, contentColor, titleContentColor, actionContentColor) {
        RichTooltipColors(
            containerColor = containerColor,
            contentColor = contentColor,
            titleContentColor = titleContentColor,
            actionContentColor = actionContentColor,
        )
    }


    @Composable
    fun rememberTooltipPositionProvider(
        positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
        spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor,
    ): PopupPositionProvider {
        val spacingPx = with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
        return remember(positioning, spacingPx) {
            TooltipPopupPositionProvider(positioning, spacingPx)
        }
    }
}

private object TooltipCaretShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(0f, size.height)
            lineTo(size.width, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
