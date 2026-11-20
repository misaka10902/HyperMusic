package com.hyper.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.delay
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


    var dragOffset by mutableStateOf(Offset.Zero)
        private set


    var pointer by mutableStateOf(Offset.Zero)
        private set

    var settling by mutableStateOf(false)
        private set


    var suppressPlacement by mutableStateOf(false)
        private set

    val settleX = Animatable(0f)
    val settleY = Animatable(0f)

    private var snapshotProvider: () -> List<ReorderGeom> = { emptyList() }
    private var geoms: List<ReorderGeom> = emptyList()
    private var pendingFrom = -1
    private var pendingTo = -1


    private var armedIndex = -1
    fun arm(index: Int) { armedIndex = index }

    val isDragging: Boolean get() = draggingIndex >= 0


    fun start(index: Int, snapshot: () -> List<ReorderGeom>): Boolean {
        if (settling) return false
        snapshotProvider = snapshot
        val initial = snapshot()
        if (initial.isEmpty()) return false
        geoms = initial.sortedBy { it.index }
        val origin = geoms.firstOrNull { it.index == index } ?: return false
        draggingIndex = index
        targetIndex = index
        pointer = Offset(origin.centerX, origin.centerY)
        dragOffset = Offset.Zero
        settling = false
        return true
    }


    fun startArmed(snapshot: () -> List<ReorderGeom>): Boolean {
        if (settling) return false
        val idx = armedIndex
        armedIndex = -1
        if (idx >= 0 && start(idx, snapshot)) return true
        return false
    }


    fun startFromPoint(position: Offset, snapshot: () -> List<ReorderGeom>): Boolean {
        if (settling) return false
        val snap = snapshot()
        if (snap.isEmpty()) return false
        geoms = snap.sortedBy { it.index }


        val exact = geoms.firstOrNull { g ->
            val inX = g.width <= 0f || position.x in g.left..(g.left + g.width)
            inX && position.y in g.top..(g.top + g.height)
        }
        val hit = exact ?: geoms.minByOrNull { g -> kotlin.math.abs(position.y - g.centerY) }
            ?: return false

        return start(hit.index, snapshot)
    }

    fun drag(delta: Offset) {
        if (!isDragging || settling) return
        pointer += delta
        refresh()
    }


    fun onViewportScrolled() {
        if (!isDragging || settling) return
        refresh()
    }

    private fun refresh() {
        val snap = snapshotProvider()
        if (snap.isNotEmpty()) geoms = snap.sortedBy { it.index }


        val origin = geoms.firstOrNull { it.index == draggingIndex }
        if (origin != null) {
            dragOffset = Offset(pointer.x - origin.centerX, pointer.y - origin.centerY)
        }


        var best = draggingIndex
        var bestDist = Float.MAX_VALUE
        for (g in geoms) {
            val dx = if (g.width > 0f) pointer.x - g.centerX else 0f
            val dy = pointer.y - g.centerY
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                bestDist = dist
                best = g.index
            }
        }
        targetIndex = best
    }

    fun end() {
        if (!isDragging || settling) return
        val from = draggingIndex
        val to = targetIndex
        val origin = geoms.firstOrNull { it.index == from }
        val dest = geoms.firstOrNull { it.index == to }
        pendingFrom = from


        pendingTo = to
        val targetX: Float
        val targetY: Float
        if (origin != null && dest != null) {
            targetX = dest.centerX - origin.centerX
            targetY = dest.centerY - origin.centerY
        } else {


            targetX = 0f
            targetY = 0f
        }
        val startX = dragOffset.x
        val startY = dragOffset.y
        settling = true
        scope.launch {
            settleX.snapTo(startX)
            settleY.snapTo(startY)
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
        dragOffset = Offset.Zero
        pointer = Offset.Zero
        settling = false
        geoms = emptyList()
        snapshotProvider = { emptyList() }
        pendingFrom = -1
        pendingTo = -1
        if (from >= 0 && to >= 0 && from != to) {
            suppressPlacement = true
            onCommit(from, to)
            scope.launch {
                delay(180)
                suppressPlacement = false
            }
        }
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


    val shiftSpec = remember(index, state.draggingIndex) {
        val distance = kotlin.math.abs(index - state.draggingIndex)
        tween<Float>(
            durationMillis = 230,
            delayMillis = (distance * 16).coerceAtMost(72),
            easing = FastOutSlowInEasing,
        )
    }
    val shiftAnimX by animateFloatAsState(
        targetValue = shift.x,
        animationSpec = shiftSpec,
        label = "reorderShiftX",
    )
    val shiftAnimY by animateFloatAsState(
        targetValue = shift.y,
        animationSpec = shiftSpec,
        label = "reorderShiftY",
    )
    val shiftX = if (dragging) shiftAnimX else 0f
    val shiftY = if (dragging) shiftAnimY else 0f
    val lift by animateFloatAsState(
        targetValue = if (dragged) 1f else 0f,
        animationSpec = reorderSpring(),
        label = "reorderLift",
    )


    val currentIndex by rememberUpdatedState(index)
    val reportIndex = if (enabled) Modifier.pointerInput(id) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            state.arm(currentIndex)
        }
    } else Modifier

    return this
        .then(reportIndex)
        .zIndex(if (dragged) 1f else 0f)
        .graphicsLayer {

            translationX = when {
                dragged && state.settling -> state.settleX.value
                dragged -> state.dragOffset.x
                else -> shiftX
            }
            translationY = when {
                dragged && state.settling -> state.settleY.value
                dragged -> state.dragOffset.y
                else -> shiftY
            }
            val s = 1f + 0.05f * lift
            scaleX = s
            scaleY = s
            alpha = 1f
        }
}


@Composable
fun Modifier.dragReorderSurface(
    state: DragReorderState,
    enabled: Boolean,
    snapshot: () -> List<ReorderGeom>,
): Modifier {
    val currentSnapshot = rememberUpdatedState(snapshot)
    if (!enabled) return this
    return this.pointerInput(state) {
        detectDragGesturesAfterLongPress(
            onDragStart = { startPos ->
                if (!state.startArmed(currentSnapshot.value)) {
                    state.startFromPoint(startPos, currentSnapshot.value)
                }
            },
            onDrag = { change, delta ->
                change.consume()
                state.drag(delta)
            },
            onDragEnd = { state.end() },
            onDragCancel = { state.end() },
        )
    }
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
