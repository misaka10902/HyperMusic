


@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package top.yukonga.miuix.kmp.utils

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DragEvent
import androidx.compose.foundation.gestures.DragGestureNode
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


enum class PagerInterceptionMode(val title: String) {

    Native("Default"),


    CrossAxis("Cross-Axis"),


    TapToHalt("iOS-like"),
}


val PagerNavigationSpringSpec: SpringSpec<Float> = spring(
    stiffness = 322.2f,
    dampingRatio = 32.31f / (2f * kotlin.math.sqrt(322.2f)),
    visibilityThreshold = 0.5f,
)


suspend fun PagerState.springAnimateToPage(target: Int) {
    if (target !in 0 until pageCount) return
    scroll(MutatePriority.UserInput) {
        animateToPage(this, target)
    }
}

private suspend fun PagerState.animateToPage(
    scrollScope: ScrollScope,
    target: Int,
    initialVelocity: Float = 0f,
) = with(scrollScope) {
    if (pageCount == 0) return@with
    val destination = target.coerceIn(0, pageCount - 1)
    val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
    updateTargetPage(destination)
    if (pageSize > 0) {
        val distance = (destination - currentPage - currentPageOffsetFraction) * pageSize
        var previousValue = 0f
        animate(
            initialValue = 0f,
            targetValue = distance,
            initialVelocity = initialVelocity,
            animationSpec = PagerNavigationSpringSpec,
        ) { currentValue, _ ->
            previousValue += scrollBy(currentValue - previousValue)
        }
    }

    if (pageCount > 0) updateCurrentPage(destination.coerceAtMost(pageCount - 1))
}


object PagerGestureNestedScrollConnection : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f)
}


fun Modifier.pagerGestureOverride(
    pagerState: PagerState,
    flingBehavior: FlingBehavior,
    mode: PagerInterceptionMode = PagerInterceptionMode.CrossAxis,
    enabled: Boolean = true,
    flingTracker: PagerFlingTrackerConnection? = null,
    onTriggered: (() -> Unit)? = null,
): Modifier = when (mode) {
    PagerInterceptionMode.CrossAxis ->
        if (enabled) {
            pagerNonTouchScroll(pagerState, flingBehavior).then(PagerPageGestureElement(pagerState, onTriggered))
        } else {
            this
        }

    PagerInterceptionMode.TapToHalt ->
        if (enabled && flingTracker != null) {
            iosStyleMomentumHalt(
                flingTracker = flingTracker,
                enabled = true,
                onHalted = onTriggered,
            )
        } else {
            this
        }

    PagerInterceptionMode.Native -> this
}


fun Modifier.pagerGestureOverride(
    pagerState: PagerState,
    flingBehavior: FlingBehavior,
    mode: Int,
    enabled: Boolean = true,
    flingTracker: PagerFlingTrackerConnection? = null,
    onTriggered: (() -> Unit)? = null,
): Modifier = pagerGestureOverride(
    pagerState = pagerState,
    flingBehavior = flingBehavior,
    mode = PagerInterceptionMode.entries.getOrElse(mode) { PagerInterceptionMode.Native },
    enabled = enabled,
    flingTracker = flingTracker,
    onTriggered = onTriggered,
)


@Composable
fun Modifier.pagerGestureOverride(
    pagerState: PagerState,
    flingBehavior: FlingBehavior,
    mode: PagerInterceptionMode = PagerInterceptionMode.CrossAxis,
    enabled: Boolean = true,
    onTriggered: (() -> Unit)? = null,
): Modifier {
    val flingTracker = remember { PagerFlingTrackerConnection() }
    return pagerGestureOverride(
        pagerState = pagerState,
        flingBehavior = flingBehavior,
        mode = mode,
        enabled = enabled,
        flingTracker = flingTracker,
        onTriggered = onTriggered,
    )
}

private data class PagerPageGestureElement(
    val pagerState: PagerState,
    val onIntercepted: (() -> Unit)?,
) : ModifierNodeElement<PagerPageGestureNode>() {
    override fun create(): PagerPageGestureNode = PagerPageGestureNode(pagerState, onIntercepted)

    override fun update(node: PagerPageGestureNode) {
        node.update(pagerState, onIntercepted)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pagerGestureOverride"
        properties["pagerState"] = pagerState
    }
}


private class PagerPageGestureNode(
    private var pagerState: PagerState,
    private var onIntercepted: (() -> Unit)?,
) : DelegatingNode(),
    CompositionLocalConsumerModifierNode,
    PointerInputModifierNode {

    private val motion = PagerPageMotion(pagerState, onIntercepted)
    private val childMotion = PagerChildMotionConnection()

    init {
        motion.childMotion = childMotion


        delegate(nestedScrollModifierNode(childMotion, null))
    }



    override fun onAttach() {
        motion.nodeScope = coroutineScope
        refreshEnvironment()
    }

    override fun onLayoutDirectionChange() = refreshEnvironment()

    override fun onDensityChange() = refreshEnvironment()

    private fun refreshEnvironment() {
        if (!isAttached) return
        motion.minimumVelocity = with(requireDensity()) { 400.dp.toPx() }
        motion.touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
        motion.directionSign = if (
            (currentValueOf(LocalLayoutDirection) == LayoutDirection.Rtl) xor
            pagerState.layoutInfo.reverseLayout
        ) {
            1f
        } else {
            -1f
        }
    }

    private val dragNode = delegate(PagerPageDragNode(motion))
    private val takeoverNode = delegate(PagerPageTakeoverNode(motion, childMotion))

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pointerEvent.changes.fastAll { it.type == PointerType.Mouse }) {
            return
        }



        val takeoverWasTrackingPointer = takeoverNode.isTrackingPointer
        if (takeoverNode.willTrackPointer(pointerEvent, pass)) {
            dragNode.onCancelPointerInput()
        }
        takeoverNode.onPointerEvent(pointerEvent, pass, bounds)
        if (takeoverWasTrackingPointer && !takeoverNode.isTrackingPointer) {
            dragNode.onCancelPointerInput()
        }
        if (!takeoverWasTrackingPointer && !takeoverNode.isTrackingPointer) {
            dragNode.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    override fun onCancelPointerInput() {
        takeoverNode.onCancelPointerInput()
        dragNode.onCancelPointerInput()
    }

    fun update(state: PagerState, onIntercepted: (() -> Unit)?) {
        motion.update(state, onIntercepted)
        refreshEnvironment()
    }
}


private class PagerPageTakeoverNode(
    private val motion: PagerPageMotion,
    private val childMotion: PagerChildMotionConnection,
) : Modifier.Node(),
    PointerInputModifierNode {
    private var pointerId: PointerId? = null
    private var dragged = false
    private var accumulated = Offset.Zero
    private val velocityTracker = VelocityTracker()

    var isHandlingPointer: Boolean = false
        private set

    val isTrackingPointer: Boolean
        get() = pointerId != null

    fun willTrackPointer(pointerEvent: PointerEvent, pass: PointerEventPass): Boolean = pass == PointerEventPass.Initial &&
        pointerId == null &&
        (childMotion.isInMotion || motion.isSettling) &&
        pointerEvent.changes.fastFirstOrNull { it.changedToDownIgnoreConsumed() && it.type != PointerType.Mouse } != null
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        val tracked = pointerId
        if (tracked == null) {
            val down = pointerEvent.changes.fastFirstOrNull { it.changedToDownIgnoreConsumed() && it.type != PointerType.Mouse } ?: return
            val shouldTakeover = childMotion.isInMotion || motion.isSettling
            if (!shouldTakeover) return
            pointerId = down.id
            isHandlingPointer = false
            dragged = false
            accumulated = Offset.Zero
            velocityTracker.resetTracking()
            velocityTracker.addPointerInputChange(down)
            if (childMotion.isInMotion) {
                motion.stopChildMotion()
            }
            return
        }
        val change = pointerEvent.changes.fastFirstOrNull { it.id == tracked } ?: return
        if (!change.pressed) {
            if (!isHandlingPointer) {
                pointerId = null
                accumulated = Offset.Zero
                motion.stopChildMotion()
                return
            }
            change.consume()
            val velocity = if (dragged) velocityTracker.calculateVelocity().x else 0f
            pointerId = null
            isHandlingPointer = false
            dragged = false
            accumulated = Offset.Zero
            motion.finishTakeoverDrag(Velocity(velocity, 0f))
            return
        }
        velocityTracker.addPointerInputChange(change)
        val delta = change.position - change.previousPosition
        if (!isHandlingPointer) {
            accumulated += delta
            val x = abs(accumulated.x)
            val y = abs(accumulated.y)
            if (x > motion.touchSlop && x > y) {
                isHandlingPointer = true
                dragged = true
                motion.onDragStarted()
                motion.stopChildMotion()
                motion.startTakeoverDrag()
                change.consume()
                val overSlop = accumulated.x - if (accumulated.x >= 0f) motion.touchSlop else -motion.touchSlop
                motion.dragBy(overSlop)
            } else if (y > motion.touchSlop && y > x) {
                pointerId = null
                accumulated = Offset.Zero
                motion.stopChildMotion()
            }
            return
        }
        change.consume()
        motion.dragBy(delta.x)
    }

    override fun onCancelPointerInput() {
        pointerId = null
        isHandlingPointer = false
        dragged = false
        accumulated = Offset.Zero
        motion.cancelTakeoverDrag()
    }
}

private class PagerPageDragNode(
    private val motion: PagerPageMotion,
) : DragGestureNode(
    canDrag = { it != PointerType.Mouse },
    enabled = true,
    interactionSource = null,
    orientation = Orientation.Horizontal,
) {
    override fun startDragImmediately(): Boolean = false

    override fun onDragStarted(startedPosition: Offset) {
        motion.onDragStarted()
    }

    override suspend fun drag(forEachDelta: suspend ((DragEvent.DragDelta) -> Unit) -> Unit) {
        motion.drag(forEachDelta)
    }

    override fun onDragStopped(event: DragEvent.DragStopped) {
        motion.onDragStopped(event.velocity)
    }
}


private class PagerChildMotionConnection : NestedScrollConnection {
    var isInMotion: Boolean = false
        private set


    private var flingHadVelocity = false


    private var suppressMotionUntilUserScroll = false

    fun stop() {
        isInMotion = false
        flingHadVelocity = false
        suppressMotionUntilUserScroll = true
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

        if (source == NestedScrollSource.UserInput) {
            isInMotion = false
            flingHadVelocity = false
            suppressMotionUntilUserScroll = false
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (suppressMotionUntilUserScroll) return Velocity.Zero
        flingHadVelocity = available.x != 0f || available.y != 0f
        isInMotion = true
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (suppressMotionUntilUserScroll) return Velocity.Zero


        if (flingHadVelocity) {
            isInMotion = false
            flingHadVelocity = false
        }
        return Velocity.Zero
    }
}


private class PagerPageMotion(
    private var pagerState: PagerState,
    private var onIntercepted: (() -> Unit)?,
) {
    private var settleJob: Job? = null
    private var takeoverJob: Job? = null
    private var takeoverChannel: Channel<Float>? = null
    var childMotion: PagerChildMotionConnection? = null
    lateinit var nodeScope: CoroutineScope
    var minimumVelocity: Float = 0f
    var touchSlop: Float = 0f
    var directionSign: Float = -1f

    val isSettling: Boolean
        get() = settleJob?.isActive == true
    fun update(state: PagerState, intercepted: (() -> Unit)?) {
        pagerState = state
        onIntercepted = intercepted
    }

    fun onDragStarted() {
        settleJob?.cancel()
        settleJob = null
        onIntercepted?.invoke()
    }

    fun stopChildMotion() {
        childMotion?.stop()
    }


    fun startTakeoverDrag() {
        cancelTakeoverDrag()
        val channel = Channel<Float>(Channel.UNLIMITED)
        takeoverChannel = channel
        takeoverJob = nodeScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                pagerState.scroll(MutatePriority.UserInput) {
                    for (delta in channel) {
                        scrollBy(delta * directionSign)
                    }
                }
            } finally {
                channel.cancel()
            }
        }
    }


    fun dragBy(deltaX: Float) {
        takeoverChannel?.trySend(deltaX)
    }


    fun finishTakeoverDrag(velocity: Velocity) {
        val channel = takeoverChannel
        val job = takeoverJob
        takeoverChannel = null
        takeoverJob = null
        if (channel == null) {
            onDragStopped(velocity)
            return
        }

        channel.close()
        settleJob?.cancel()
        settleJob = nodeScope.launch {
            job?.join()
            settle(velocity)
        }
    }


    fun cancelTakeoverDrag() {
        takeoverChannel?.cancel()
        takeoverChannel = null
        takeoverJob?.cancel()
        takeoverJob = null
    }

    suspend fun drag(forEachDelta: suspend ((DragEvent.DragDelta) -> Unit) -> Unit) {
        val sign = directionSign
        pagerState.scroll(MutatePriority.UserInput) {
            forEachDelta { dragDelta ->
                scrollBy(dragDelta.delta.x * sign)
            }
        }
    }

    fun onDragStopped(velocity: Velocity) {
        settleJob?.cancel()
        settleJob = nodeScope.launch { settle(velocity) }
    }

    private suspend fun settle(velocity: Velocity) {
        val state = pagerState
        val sign = directionSign
        val minimum = minimumVelocity
        state.scroll(MutatePriority.Default) {
            val position = state.currentPage + state.currentPageOffsetFraction
            val signedVelocity = velocity.x * sign
            val target = when {
                signedVelocity > minimum -> ceil(position).toInt()
                signedVelocity < -minimum -> floor(position).toInt()
                else -> position.roundToInt()
            }
            state.animateToPage(this, target, signedVelocity)
        }
    }
}


class PagerFlingTrackerConnection : NestedScrollConnection {
    var isChildFlinging by mutableStateOf(false)
        internal set

    var haltFling by mutableStateOf(false)
        internal set

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && available.y != 0f) {

            haltFling = false
            isChildFlinging = false
        } else if (source == NestedScrollSource.SideEffect && available.y != 0f && isChildFlinging && haltFling) {
            isChildFlinging = false

            throw CancellationException("Child fling halted by pager gesture")
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {

        haltFling = false
        isChildFlinging = available.y != 0f
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {

        val haltedVelocity = if (haltFling) Velocity(0f, available.y) else Velocity.Zero
        isChildFlinging = false
        haltFling = false
        return haltedVelocity
    }
}


fun Modifier.iosStyleMomentumHalt(
    flingTracker: PagerFlingTrackerConnection,
    enabled: Boolean = true,
    onHalted: (() -> Unit)? = null,
): Modifier = if (!enabled) {
    this
} else {
    this.nestedScroll(flingTracker).pointerInput(flingTracker, enabled, onHalted) {
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val downPos = down.position
            val hadMomentum = flingTracker.isChildFlinging
            if (!hadMomentum) {
                return@awaitEachGesture
            }

            flingTracker.haltFling = true
            onHalted?.invoke()
            var isHalting = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.fastFirstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val totalDx = abs(change.position.x - downPos.x)
                val totalDy = abs(change.position.y - downPos.y)

                if (!isHalting && totalDx > touchSlop && totalDx > totalDy * 2f) {
                    isHalting = true
                }

                if (isHalting) {
                    change.consume()
                } else if (totalDy > touchSlop && totalDy > totalDx) {
                    flingTracker.haltFling = false
                    break
                }
            }
        }
    }
}
