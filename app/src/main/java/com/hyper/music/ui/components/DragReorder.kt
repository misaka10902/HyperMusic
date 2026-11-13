package com.hyper.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class ReorderGeom(
    val index: Int,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
}

class DragReorderState internal constructor(
    private val scope: CoroutineScope,
    private val onCommit: (Int, Int) -> Unit,
) {
    var draggingIndex by mutableIntStateOf(-1)
        private set
    var targetIndex by mutableIntStateOf(-1)
        private set
    var dragX by mutableFloatStateOf(0f)
        private set
    var dragY by mutableFloatStateOf(0f)
        private set

    var settling by mutableStateOf(false)
        private set

    val settleX = Animatable(0f)
    val settleY = Animatable(0f)

    private var geoms: List<ReorderGeom> = emptyList()
    private var fromGeom: ReorderGeom? = null
    private var pendingFrom = -1
    private var pendingTo = -1

    val isDragging: Boolean get() = draggingIndex >= 0

    fun start(index: Int, snapshot: List<ReorderGeom>) {

        if (settling || snapshot.isEmpty()) return
        val sorted = snapshot.sortedBy { it.index }
        val origin = sorted.firstOrNull { it.index == index } ?: return
        geoms = sorted
        fromGeom = origin
        draggingIndex = index
        targetIndex = index
        dragX = 0f
        dragY = 0f
        settling = false
    }

    fun drag(delta: Offset) {
        if (!isDragging || settling) return
        val origin = fromGeom ?: return
        dragX += delta.x
        dragY += delta.y

        val adx = if (dragX < 0) -dragX else dragX
        val ady = if (dragY < 0) -dragY else dragY
        val useY = ady >= adx
        val originCoord = if (useY) origin.centerY else origin.centerX
        val draggedCoord = if (useY) origin.centerY + dragY else origin.centerX + dragX
        var passed = 0
        for (g in geoms) {
            if (g.index == draggingIndex) continue
            val c = if (useY) g.centerY else g.centerX
            if (draggedCoord >= originCoord) {
                if (c > originCoord && c <= draggedCoord) passed++
            } else {
                if (c < originCoord && c >= draggedCoord) passed--
            }
        }
        val last = geoms.maxOfOrNull { it.index } ?: draggingIndex
        targetIndex = (draggingIndex + passed).coerceIn(0, last)
    }

    fun end() {
        if (!isDragging || settling) return
        val from = draggingIndex
        val to = targetIndex
        val origin = fromGeom
        val dest = geoms.firstOrNull { it.index == to }
        pendingFrom = from
        val targetX: Float
        val targetY: Float
        if (origin != null && dest != null) {

            pendingTo = to
            targetX = dest.left - origin.left
            targetY = dest.top - origin.top
        } else {

            pendingTo = from
            targetX = 0f
            targetY = 0f
        }
        settling = true
        scope.launch {
            settleX.snapTo(dragX)
            settleY.snapTo(dragY)
            coroutineScope {
                launch { settleX.animateTo(targetX, reorderSpring()) }
                launch { settleY.animateTo(targetY, reorderSpring()) }
            }
            commit()
        }
    }

    private fun commit() {
        val from = pendingFrom
        val to = pendingTo
        draggingIndex = -1
        targetIndex = -1
        dragX = 0f
        dragY = 0f
        settling = false
        geoms = emptyList()
        fromGeom = null
        pendingFrom = -1
        pendingTo = -1
        if (from >= 0 && to >= 0 && from != to) onCommit(from, to)
    }

    fun offsetFor(index: Int): Offset {
        if (!isDragging || index == draggingIndex) return Offset.Zero
        val from = draggingIndex
        val to = targetIndex
        if (from == to) return Offset.Zero
        val destIndex = when {
            from < to && index in (from + 1)..to -> index - 1
            from > to && index in to until from -> index + 1
            else -> return Offset.Zero
        }
        val cur = geoms.firstOrNull { it.index == index } ?: return Offset.Zero
        val dest = geoms.firstOrNull { it.index == destIndex } ?: return Offset.Zero
        return Offset(dest.left - cur.left, dest.top - cur.top)
    }
}

@Composable
fun rememberDragReorderState(onCommit: (Int, Int) -> Unit): DragReorderState {
    val currentOnCommit by rememberUpdatedState(onCommit)
    val scope = rememberCoroutineScope()
    return remember { DragReorderState(scope) { from, to -> currentOnCommit(from, to) } }
}

private fun reorderSpring(): AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

@Composable
fun Modifier.dragReorderItem(
    id: Any,
    index: Int,
    state: DragReorderState,
    enabled: Boolean,
    snapshot: () -> List<ReorderGeom>,
): Modifier {
    val dragged = state.draggingIndex == index
    val dragging = state.isDragging
    val shift = state.offsetFor(index)

    val shiftAnimX by animateFloatAsState(
        targetValue = shift.x,
        animationSpec = reorderSpring(),
        label = "reorderShiftX",
    )
    val shiftAnimY by animateFloatAsState(
        targetValue = shift.y,
        animationSpec = reorderSpring(),
        label = "reorderShiftY",
    )
    val shiftX = if (dragging) shiftAnimX else 0f
    val shiftY = if (dragging) shiftAnimY else 0f
    val lift by animateFloatAsState(
        targetValue = if (dragged) 1f else 0f,
        animationSpec = reorderSpring(),
        label = "reorderLift",
    )

    val currentIndex = rememberUpdatedState(index)
    val currentSnapshot = rememberUpdatedState(snapshot)

    val gestureModifier = if (enabled) {
        Modifier.pointerInput(id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { state.start(currentIndex.value, currentSnapshot.value()) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.drag(dragAmount)
                },
                onDragEnd = { state.end() },
                onDragCancel = { state.end() },
            )
        }
    } else {
        Modifier
    }

    return this
        .zIndex(if (dragged) 1f else 0f)
        .graphicsLayer {

            translationX = when {
                dragged && state.settling -> state.settleX.value
                dragged -> state.dragX
                else -> shiftX
            }
            translationY = when {
                dragged && state.settling -> state.settleY.value
                dragged -> state.dragY
                else -> shiftY
            }
            val s = 1f + 0.05f * lift
            scaleX = s
            scaleY = s
            alpha = 1f
        }
        .then(gestureModifier)
}

class ReorderGeometryRegistry {
    private val geoms = mutableMapOf<Int, ReorderGeom>()

    fun update(index: Int, geom: ReorderGeom) {
        geoms[index] = geom
    }

    fun snapshot(count: Int): List<ReorderGeom> =
        geoms.values.filter { it.index in 0 until count }.sortedBy { it.index }
}

fun Modifier.reorderGeometry(registry: ReorderGeometryRegistry, index: Int): Modifier =
    this.onGloballyPositioned { coords ->
        val origin = coords.localToRoot(Offset.Zero)
        registry.update(
            index,
            ReorderGeom(
                index,
                origin.x,
                origin.y,
                coords.size.width.toFloat(),
                coords.size.height.toFloat(),
            ),
        )
    }
