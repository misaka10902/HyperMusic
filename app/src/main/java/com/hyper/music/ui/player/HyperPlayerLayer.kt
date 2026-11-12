package com.hyper.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
fun HyperPlayerLayer(
    state: HyperPlayerState,
    backdrop: Backdrop,
    pageBackdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    onOpenQueue: () -> Unit = {},
    onMore: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
) {
    BackHandler(enabled = state.isExpanded) {
        state.collapse()
    }





    val progress = state.expandProgress
    Box(
        modifier = modifier
            .fillMaxSize()
            .layerBackdrop(pageBackdrop)
            .graphicsLayer {
                val p = state.expandProgress.coerceIn(0f, 1f)
                translationY = (1f - p) * size.height
                alpha = if (p <= 0.001f) 0f else 1f
            }
            .pointerInput(state) {
                detectVerticalDragGestures(
                    onDragEnd = { state.endDrag(0f) },
                    onDragCancel = { state.endDrag(0f) },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        state.dragBy(dragAmount, size.height.coerceAtLeast(1).toFloat())
                    },
                )
            },
    ) {
        if (progress > 0.001f) {
            HyperPlayerPageContent(
                state = state,
                onOpenQueue = onOpenQueue,
                onMore = onMore,
                onOpenSleepTimer = onOpenSleepTimer,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
