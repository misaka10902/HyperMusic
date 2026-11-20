package com.hyper.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

    /**
     * 拖动中的条目相对**当前布局位置**的位移。
     *
     * 不用「累计手指位移」是因为列表会自动滚动：布局位置一变，累计位移就会让卡片
     * 从手指底下滑走。这里每帧按「指针位置 - 条目当前中心」重算，滚动时天然补偿。
     */
    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    /** 指针在**列表视口坐标系**里的位置（与 [ReorderGeom] 同一套坐标）。 */
    var pointer by mutableStateOf(Offset.Zero)
        private set

    var settling by mutableStateOf(false)
        private set

    /**
     * 刚刚完成一次拖拽排序。列表用它临时关掉 `animateItem` 的位移动画：
     * 拖拽期间条目是靠 graphicsLayer 平移的，数据一换位置，`animateItem` 会把条目
     * 从旧布局位置再滑一遍，看起来就是「先弹回去、再滑过来」。等布局稳定后恢复。
     */
    var suppressPlacement by mutableStateOf(false)
        private set

    val settleX = Animatable(0f)
    val settleY = Animatable(0f)

    private var snapshotProvider: () -> List<ReorderGeom> = { emptyList() }
    private var geoms: List<ReorderGeom> = emptyList()
    private var pendingFrom = -1
    private var pendingTo = -1

    val isDragging: Boolean get() = draggingIndex >= 0

    /**
     * @param snapshot 每次调用都返回**当前**可见条目的几何（视口坐标）。
     *   拖动过程中列表可能滚动，必须每帧重新取，不能只在按下时抓一次快照。
     */
    fun start(index: Int, snapshot: () -> List<ReorderGeom>) {
        if (settling) return
        snapshotProvider = snapshot
        val initial = snapshot()
        if (initial.isEmpty()) return
        geoms = initial.sortedBy { it.index }
        val origin = geoms.firstOrNull { it.index == index } ?: return
        draggingIndex = index
        targetIndex = index
        pointer = Offset(origin.centerX, origin.centerY)
        dragOffset = Offset.Zero
        settling = false
    }

    fun drag(delta: Offset) {
        if (!isDragging || settling) return
        pointer += delta
        refresh()
    }

    /** 自动滚动之后调用：手指没动但布局动了，需要重算位移与目标位。 */
    fun onViewportScrolled() {
        if (!isDragging || settling) return
        refresh()
    }

    private fun refresh() {
        val snap = snapshotProvider()
        if (snap.isNotEmpty()) geoms = snap.sortedBy { it.index }
        val origin = geoms.firstOrNull { it.index == draggingIndex } ?: return
        dragOffset = Offset(pointer.x - origin.centerX, pointer.y - origin.centerY)

        val adx = if (dragOffset.x < 0) -dragOffset.x else dragOffset.x
        val ady = if (dragOffset.y < 0) -dragOffset.y else dragOffset.y
        val useY = ady >= adx
        val originC = if (useY) origin.centerY else origin.centerX
        val draggedC = if (useY) pointer.y else pointer.x
        var passed = 0
        for (g in geoms) {
            if (g.index == draggingIndex) continue
            val c = if (useY) g.centerY else g.centerX
            if (draggedC >= originC) {
                if (c > originC && c <= draggedC) passed++
            } else {
                if (c < originC && c >= draggedC) passed--
            }
        }
        val last = geoms.maxOfOrNull { it.index } ?: draggingIndex
        targetIndex = (draggingIndex + passed).coerceIn(0, last)
    }

    fun end() {
        if (!isDragging || settling) return
        val from = draggingIndex
        val to = targetIndex
        val origin = geoms.firstOrNull { it.index == from }
        val dest = geoms.firstOrNull { it.index == to }
        pendingFrom = from
        val targetX: Float
        val targetY: Float
        if (origin != null && dest != null) {
            pendingTo = to
            targetX = dest.centerX - origin.centerX
            targetY = dest.centerY - origin.centerY
        } else {
            pendingTo = from
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

    // 让位动画：离拖动点越远的卡片越晚起步，让出一格时是一串依次挪动，
    // 而不是整片同时抽一下。延迟上限压得比较小，连续跨格时不会显得迟钝。
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

    val currentIndex = rememberUpdatedState(index)
    val currentSnapshot = rememberUpdatedState(snapshot)

    val gestureModifier = if (enabled) {
        Modifier.pointerInput(id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { state.start(currentIndex.value, currentSnapshot.value) },
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
