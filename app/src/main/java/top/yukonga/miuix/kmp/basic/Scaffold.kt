package top.yukonga.miuix.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.LocalDialogStates
import top.yukonga.miuix.kmp.utils.LocalPopupStates
import top.yukonga.miuix.kmp.utils.LocalRootDialogStates
import top.yukonga.miuix.kmp.utils.LocalRootPopupStates
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    floatingToolbar: @Composable () -> Unit = {},
    floatingToolbarPosition: ToolbarPosition = ToolbarPosition.BottomCenter,
    snackbarHost: @Composable () -> Unit = {},
    popupHost: @Composable () -> Unit = { MiuixPopupHost() },
    containerColor: Color = MiuixTheme.colorScheme.surface,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
) {
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(contentWindowInsets) }
    val popupStates = remember { mutableStateListOf<MiuixPopupUtils.PopupState>() }
    val dialogStates = remember { mutableStateListOf<MiuixPopupUtils.DialogState>() }
    val parentRootDialogStates = LocalRootDialogStates.current
    val rootDialogStates = parentRootDialogStates ?: dialogStates
    val parentRootPopupStates = LocalRootPopupStates.current
    val rootPopupStates = parentRootPopupStates ?: popupStates
    Surface(
        modifier = modifier.onConsumedWindowInsetsChanged { consumedWindowInsets ->

            safeInsets.insets = contentWindowInsets.exclude(consumedWindowInsets)
        },
        color = containerColor,
    ) {
        CompositionLocalProvider(
            LocalPopupStates provides popupStates,
            LocalDialogStates provides dialogStates,
            LocalRootDialogStates provides rootDialogStates,
            LocalRootPopupStates provides rootPopupStates,
        ) {
            ScaffoldLayout(
                topBar = topBar,
                bottomBar = bottomBar,
                content = content,
                snackbar = snackbarHost,
                floatingActionButton = floatingActionButton,
                floatingActionButtonPosition = floatingActionButtonPosition,
                floatingToolbar = floatingToolbar,
                floatingToolbarPosition = floatingToolbarPosition,
                popup = popupHost,
                contentWindowInsets = safeInsets,
            )
        }
    }
}

@Composable
private fun ScaffoldLayout(
    topBar: @Composable () -> Unit,
    snackbar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    floatingActionButtonPosition: FabPosition,
    floatingToolbar: @Composable () -> Unit,
    floatingToolbarPosition: ToolbarPosition,
    popup: @Composable () -> Unit,
    contentWindowInsets: WindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {

    val contentPadding = remember {
        object : PaddingValues {
            var paddingHolder by mutableStateOf(PaddingValues(0.dp))

            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = paddingHolder.calculateLeftPadding(layoutDirection)

            override fun calculateTopPadding(): Dp = paddingHolder.calculateTopPadding()

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = paddingHolder.calculateRightPadding(layoutDirection)

            override fun calculateBottomPadding(): Dp = paddingHolder.calculateBottomPadding()
        }
    }
    val popupContent: @Composable () -> Unit = remember(popup) { { Box { popup() } } }
    val topBarContent: @Composable () -> Unit = remember(topBar) { { Box { topBar() } } }
    val snackbarContent: @Composable () -> Unit = remember(snackbar) { { Box { snackbar() } } }
    val floatingActionButtonContent: @Composable () -> Unit =
        remember(floatingActionButton) { { Box { floatingActionButton() } } }
    val floatingToolbarContent: @Composable () -> Unit = remember(floatingToolbar) { { Box { floatingToolbar() } } }
    val bodyContent: @Composable () -> Unit = remember(content, contentPadding) { { Box { content(contentPadding) } } }
    val bottomBarContent: @Composable () -> Unit = remember(bottomBar) { { Box { bottomBar() } } }
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val topInset = contentWindowInsets.getTop(this@SubcomposeLayout)
        val leftInset = contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection)
        val rightInset = contentWindowInsets.getRight(this@SubcomposeLayout, layoutDirection)
        val bottomInset = contentWindowInsets.getBottom(this@SubcomposeLayout)

        val popupPlaceables =
            subcompose(ScaffoldLayoutContent.Popup, popupContent)
                .first()
                .measure(looseConstraints)

        val topBarPlaceable =
            subcompose(ScaffoldLayoutContent.TopBar, topBarContent)
                .first()
                .measure(looseConstraints)

        val snackbarPlaceable =
            subcompose(ScaffoldLayoutContent.Snackbar, snackbarContent)
                .first()
                .measure(looseConstraints.offset(-leftInset - rightInset, -bottomInset))

        val fabPlaceable =
            subcompose(ScaffoldLayoutContent.Fab, floatingActionButtonContent)
                .first()
                .measure(looseConstraints.offset(-leftInset - rightInset, -bottomInset))
        val isFabEmpty = fabPlaceable.width == 0 && fabPlaceable.height == 0
        val fabPlacement = if (!isFabEmpty) {
            val fabWidth = fabPlaceable.width
            val fabHeight = fabPlaceable.height

            val fabLeftOffset =
                when (floatingActionButtonPosition) {
                    FabPosition.Start -> {
                        if (layoutDirection == LayoutDirection.Ltr) {
                            FabSpacing.roundToPx() + leftInset
                        } else {
                            layoutWidth - FabSpacing.roundToPx() - fabWidth - rightInset
                        }
                    }

                    FabPosition.End,
                    FabPosition.EndOverlay,
                    -> {
                        if (layoutDirection == LayoutDirection.Ltr) {
                            layoutWidth - FabSpacing.roundToPx() - fabWidth - rightInset
                        } else {
                            FabSpacing.roundToPx() + leftInset
                        }
                    }

                    else -> (layoutWidth - fabWidth + leftInset - rightInset) / 2
                }

            FabPlacement(left = fabLeftOffset, width = fabWidth, height = fabHeight)
        } else {
            null
        }

        val bottomBarPlaceable =
            subcompose(ScaffoldLayoutContent.BottomBar, bottomBarContent)
                .first()
                .measure(looseConstraints)
        val isBottomBarEmpty = bottomBarPlaceable.width == 0 && bottomBarPlaceable.height == 0
        val fabOffsetFromBottom = fabPlacement?.let {
            if (isBottomBarEmpty || floatingActionButtonPosition == FabPosition.EndOverlay) {
                it.height + FabSpacing.roundToPx() + contentWindowInsets.getBottom(this@SubcomposeLayout)
            } else {

                bottomBarPlaceable.height + it.height + FabSpacing.roundToPx()
            }
        }

        val snackbarHeight = snackbarPlaceable.height

        val floatingToolbarPlaceable =
            subcompose(ScaffoldLayoutContent.FloatingToolbar, floatingToolbarContent)
                .first()
                .measure(looseConstraints.offset(-leftInset - rightInset, -bottomInset))

        val isFloatingToolbarEmpty = floatingToolbarPlaceable.width == 0 && floatingToolbarPlaceable.height == 0

        val floatingToolbarOffset = if (!isFloatingToolbarEmpty) {
            val alignment = floatingToolbarPosition.toAlignment()
            val availableWidth = layoutWidth - leftInset - rightInset
            val availableHeight = layoutHeight - topBarPlaceable.height - topInset - bottomInset
            val position = alignment.align(
                IntSize(floatingToolbarPlaceable.width, floatingToolbarPlaceable.height),
                IntSize(availableWidth, availableHeight),
                layoutDirection,
            )
            IntOffset(
                x = leftInset + position.x,
                y = topBarPlaceable.height + topInset + position.y - FloatingToolbarSpacing.roundToPx(),
            )
        } else {
            null
        }

        val isFloatingToolbarAtBottom =
            floatingToolbarOffset != null &&
                (
                    floatingToolbarPosition == ToolbarPosition.BottomStart ||
                        floatingToolbarPosition == ToolbarPosition.BottomCenter ||
                        floatingToolbarPosition == ToolbarPosition.BottomEnd
                    )

        val snackbarOffsetFromBottom =
            if (snackbarHeight != 0) {
                val toolbarOffsetFromBottom =
                    if (isFloatingToolbarAtBottom) {
                        (layoutHeight - floatingToolbarOffset.y + FloatingToolbarSpacing.roundToPx())
                            .coerceAtLeast(0)
                    } else {
                        0
                    }
                snackbarHeight +
                    (
                        fabOffsetFromBottom
                            ?: bottomBarPlaceable.height.takeIf { !isBottomBarEmpty }
                            ?: contentWindowInsets.getBottom(this@SubcomposeLayout)
                        ).coerceAtLeast(toolbarOffsetFromBottom)
            } else {
                0
            }

        val insets = contentWindowInsets.asPaddingValues(this)
        contentPadding.paddingHolder =
            PaddingValues(
                top =
                if (topBarPlaceable.width == 0 && topBarPlaceable.height == 0) {
                    insets.calculateTopPadding()
                } else {
                    topBarPlaceable.height.toDp()
                },
                bottom =
                if (isBottomBarEmpty) {
                    insets.calculateBottomPadding()
                } else {
                    bottomBarPlaceable.height.toDp()
                },
                start = insets.calculateStartPadding(layoutDirection),
                end = insets.calculateEndPadding(layoutDirection),
            )

        val bodyContentPlaceable =
            subcompose(ScaffoldLayoutContent.MainContent, bodyContent)
                .first()
                .measure(looseConstraints)

        layout(layoutWidth, layoutHeight) {

            bodyContentPlaceable.place(0, 0)

            topBarPlaceable.place(0, 0)

            snackbarPlaceable.place(
                (
                    layoutWidth - snackbarPlaceable.width +
                        contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection) -
                        contentWindowInsets.getRight(this@SubcomposeLayout, layoutDirection)
                    ) / 2,
                layoutHeight - snackbarOffsetFromBottom,
            )

            bottomBarPlaceable.place(0, layoutHeight - bottomBarPlaceable.height)

            floatingToolbarOffset?.let { offset ->
                floatingToolbarPlaceable.place(offset.x, offset.y)
            }

            fabPlacement?.let { placement ->
                fabPlaceable.place(placement.left, layoutHeight - fabOffsetFromBottom!!)
            }

            popupPlaceables.place(0, 0)
        }
    }
}

private enum class ScaffoldLayoutContent {
    TopBar,
    BottomBar,
    Snackbar,
    FloatingToolbar,
    Fab,
    Popup,
    MainContent,
}

internal class MutableWindowInsets(initialInsets: WindowInsets = WindowInsets(0, 0, 0, 0)) : WindowInsets {

    var insets by mutableStateOf(initialInsets)

    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = insets.getLeft(density, layoutDirection)

    override fun getTop(density: Density): Int = insets.getTop(density)

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = insets.getRight(density, layoutDirection)

    override fun getBottom(density: Density): Int = insets.getBottom(density)
}

@kotlin.jvm.JvmInline
value class FabPosition internal constructor(@Suppress("unused") private val value: Int) {
    companion object {

        val Start = FabPosition(0)

        val Center = FabPosition(1)

        val End = FabPosition(2)

        val EndOverlay = FabPosition(3)
    }

    override fun toString(): String = when (this) {
        Start -> "FabPosition.Start"
        Center -> "FabPosition.Center"
        End -> "FabPosition.End"
        else -> "FabPosition.EndOverlay"
    }
}

@Immutable
internal class FabPlacement(val left: Int, val width: Int, val height: Int)

private val FabSpacing = 12.dp

private val FloatingToolbarSpacing = 4.dp

internal val Alignment.vertical: Alignment.Vertical
    get() = when (this) {
        Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd -> Alignment.Top
        Alignment.CenterStart, Alignment.Center, Alignment.CenterEnd -> Alignment.CenterVertically
        Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd -> Alignment.Bottom
        else -> Alignment.CenterVertically
    }

internal val Alignment.horizontal: Alignment.Horizontal
    get() = when (this) {
        Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> Alignment.Start
        Alignment.TopCenter, Alignment.Center, Alignment.BottomCenter -> Alignment.CenterHorizontally
        Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

internal fun ToolbarPosition.toAlignment(): Alignment = when (this) {
    ToolbarPosition.TopStart -> Alignment.TopStart

    ToolbarPosition.CenterStart -> Alignment.CenterStart

    ToolbarPosition.BottomStart -> Alignment.BottomStart

    ToolbarPosition.TopEnd -> Alignment.TopEnd

    ToolbarPosition.CenterEnd -> Alignment.CenterEnd

    ToolbarPosition.BottomEnd -> Alignment.BottomEnd

    ToolbarPosition.TopCenter -> Alignment.TopCenter

    ToolbarPosition.BottomCenter -> Alignment.BottomCenter

    else -> Alignment.BottomCenter
}

@kotlin.jvm.JvmInline
value class ToolbarPosition internal constructor(@Suppress("unused") private val value: Int) {
    companion object {

        val TopStart = ToolbarPosition(0)

        val CenterStart = ToolbarPosition(1)

        val BottomStart = ToolbarPosition(2)

        val TopEnd = ToolbarPosition(3)

        val CenterEnd = ToolbarPosition(4)

        val BottomEnd = ToolbarPosition(5)

        val TopCenter = ToolbarPosition(6)

        val BottomCenter = ToolbarPosition(7)
    }

    override fun toString(): String = when (this) {
        TopStart -> "ToolbarPosition.TopStart"
        CenterStart -> "ToolbarPosition.CenterStart"
        BottomStart -> "ToolbarPosition.BottomStart"
        TopEnd -> "ToolbarPosition.TopEnd"
        CenterEnd -> "ToolbarPosition.CenterEnd"
        BottomEnd -> "ToolbarPosition.BottomEnd"
        TopCenter -> "ToolbarPosition.TopCenter"
        else -> "ToolbarPosition.BottomCenter"
    }
}
