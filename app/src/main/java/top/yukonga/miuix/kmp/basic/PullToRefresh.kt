package top.yukonga.miuix.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.utils.LocalOverScrollState
import top.yukonga.miuix.kmp.utils.OverScrollState
import top.yukonga.miuix.kmp.utils.SpringEngine
import top.yukonga.miuix.kmp.utils.SpringMath
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    topAppBarScrollBehavior: ScrollBehavior? = null,
    color: Color = PullToRefreshDefaults.color,
    circleSize: Dp = PullToRefreshDefaults.circleSize,
    refreshTexts: List<String> = PullToRefreshDefaults.refreshTexts,
    refreshTextStyle: TextStyle = PullToRefreshDefaults.refreshTextStyle,
    onPullProgress: ((Float) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val overScrollState = LocalOverScrollState.current
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val currentIsRefreshing by rememberUpdatedState(isRefreshing)

    val isRefreshingNow: () -> Boolean = remember { { currentIsRefreshing } }

    val currentOnPullProgress by rememberUpdatedState(onPullProgress)

    LaunchedEffect(pullToRefreshState) {
        snapshotFlow { pullToRefreshState.fullDragProgress }
            .collect { progress ->
                currentOnPullProgress?.invoke(progress)
            }
    }

    LaunchedEffect(isRefreshing, pullToRefreshState.refreshState) {
        if (!isRefreshing && pullToRefreshState.refreshState == RefreshState.Refreshing) {

            coroutineScope.launch {
                pullToRefreshState.finishRefreshing(isRefreshingNow)
            }
        } else if (isRefreshing && pullToRefreshState.refreshState == RefreshState.Idle) {

            coroutineScope.launch {
                pullToRefreshState.showRefreshing(isRefreshingNow)
            }
        }
    }

    val nestedScrollConnection =
        remember(pullToRefreshState, topAppBarScrollBehavior, overScrollState) {

            pullToRefreshState.cachedNestedScrollConnection = null
            createPullToRefreshConnection(
                pullToRefreshState,
                topAppBarScrollBehavior,
                overScrollState,
            )
        }

    val pointerModifier = remember(pullToRefreshState) {
        pullToRefreshState.isGestureActive = false
        Modifier.pointerInput(pullToRefreshState) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val active = when (event.type) {
                        PointerEventType.PanStart, PointerEventType.PanMove -> true
                        PointerEventType.PanEnd -> false
                        PointerEventType.Scroll -> pullToRefreshState.isGestureActive
                        else -> event.changes.fastAny { it.pressed && it.type != PointerType.Mouse }
                    }
                    if (pullToRefreshState.isGestureActive != active) {
                        pullToRefreshState.isGestureActive = active
                        if (
                            !active &&
                            (pullToRefreshState.refreshState == RefreshState.Pulling || pullToRefreshState.refreshState == RefreshState.ThresholdReached)
                        ) {
                            coroutineScope.launch {
                                pullToRefreshState.handlePointerRelease(currentOnRefresh, isRefreshingNow)
                            }
                        }
                    }
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalPullToRefreshState provides pullToRefreshState,
    ) {
        val boxModifier = remember(modifier, nestedScrollConnection, pointerModifier) {
            modifier
                .nestedScroll(nestedScrollConnection)
                .then(pointerModifier)
        }

        Box(modifier = boxModifier) {
            Column {
                RefreshHeader(
                    pullToRefreshState = pullToRefreshState,
                    circleSize = circleSize,
                    color = color,
                    refreshTexts = refreshTexts,
                    refreshTextStyle = refreshTextStyle,
                    modifier = Modifier.offset(y = contentPadding.calculateTopPadding()),
                )
                content()
            }
        }
    }
}

@Composable
fun rememberPullToRefreshState(
    refreshThreshold: Float = 0.25f,
): PullToRefreshState {
    val coroutineScope = rememberCoroutineScope()

    val state = remember {
        PullToRefreshState(coroutineScope)
    }

    val windowInfo = LocalWindowInfo.current
    state.maxDragDistancePx = windowInfo.containerSize.height.toFloat()

    state.fullDragRangePx = SpringMath.obtainDampingDistance(1f, state.maxDragDistancePx)
    state.refreshThreshold = refreshThreshold.coerceIn(0f, 1f)
    state.triggerProgressOffset = state.refreshThreshold * state.fullDragRangePx
    state.refreshThresholdOffset = windowInfo.containerSize.height.toFloat() * MAX_DRAWRATIO * THRESHOLD_RADIO

    SideEffect {

        val target = state.refreshThresholdOffset
        if (state.refreshState == RefreshState.Refreshing &&
            state.animationJob == null &&
            state.dragOffset != target
        ) {
            state.dragOffset = target
            state.currentTouch = SpringMath.obtainTouchDistance(target, state.maxDragDistancePx)
        }
    }

    return state
}

sealed interface RefreshState {
    data object Idle : RefreshState
    data object Pulling : RefreshState
    data object ThresholdReached : RefreshState
    data object Refreshing : RefreshState
    data object RefreshComplete : RefreshState
}

class PullToRefreshState(
    internal var coroutineScope: CoroutineScope,
) {
    internal var maxDragDistancePx: Float = 0f
    internal var fullDragRangePx: Float = 0f
    internal var refreshThresholdOffset: Float = 0f
    internal var triggerProgressOffset: Float = 0f

    internal val effectiveThresholdOffset: Float
        get() = max(refreshThresholdOffset, triggerProgressOffset)

    var refreshThreshold: Float = 0f
        internal set

    var dragOffset by mutableFloatStateOf(0f)

    var cachedNestedScrollConnection: NestedScrollConnection? = null

    private var internalRefreshState by mutableStateOf<RefreshState>(RefreshState.Idle)

    val refreshState: RefreshState get() = internalRefreshState

    val pullProgress: Float by derivedStateOf {
        if (effectiveThresholdOffset > 0f) {
            (dragOffset / effectiveThresholdOffset).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val fullDragProgress: Float by derivedStateOf {
        if (fullDragRangePx > 0f) {
            (dragOffset / fullDragRangePx).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val visualProgress: Float by derivedStateOf {
        val scaleEndOffset = min(refreshThresholdOffset, triggerProgressOffset)
        when {
            scaleEndOffset > 0f -> (dragOffset / scaleEndOffset).coerceIn(0f, 1f)

            dragOffset > 0f -> 1f

            else -> 0f
        }
    }

    internal var currentTouch by mutableFloatStateOf(0f)

    internal var isRefreshing by mutableStateOf(false)
    internal var isTouching by mutableStateOf(false)

    internal var isGestureActive = false
    internal var isRebounding by mutableStateOf(false)
    private var isProcessingRelease = false
    private val refreshCompleteAnimProgressState = mutableFloatStateOf(1f)
    internal val refreshCompleteAnimProgress: Float get() = refreshCompleteAnimProgressState.floatValue

    private val springEngine = SpringEngine()
    internal var animationJob: Job? = null

    internal suspend fun animateToSpring(targetValue: Float) {
        animationJob?.cancel()

        springEngine.start(
            startValue = dragOffset,
            targetValue = targetValue,
            initialVel = 0f,
        )

        val currentJob = currentCoroutineContext()[Job]
        animationJob = currentJob

        var lastFrameTimeNanos = -1L
        var isFinished = false

        try {
            while (currentCoroutineContext().isActive) {
                isFinished = withFrameNanos { frameTimeNanos ->
                    if (lastFrameTimeNanos == -1L) {
                        lastFrameTimeNanos = frameTimeNanos
                        return@withFrameNanos false
                    }
                    val dt = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                    lastFrameTimeNanos = frameTimeNanos

                    val finished = springEngine.step(dt)
                    val newOffset = springEngine.currentPos.toFloat()

                    dragOffset = newOffset

                    currentTouch = SpringMath.obtainTouchDistance(newOffset, maxDragDistancePx)

                    finished
                }
                if (isFinished) break
            }
        } finally {
            if (animationJob == currentJob) animationJob = null
            if (isFinished) {

                dragOffset = targetValue
                currentTouch = SpringMath.obtainTouchDistance(targetValue, maxDragDistancePx)
            }
        }
    }

    internal suspend fun startRefreshing(onRefresh: () -> Unit, isRefreshingNow: () -> Boolean) {
        if (!isRefreshing) {
            isRefreshing = true
            try {

                animateToSpring(refreshThresholdOffset)
            } finally {
                if (!isTouching) {

                    internalRefreshState = RefreshState.Refreshing

                    if (!isRefreshingNow()) onRefresh()
                } else {
                    isRefreshing = false
                }
            }
        }
    }

    internal suspend fun showRefreshing(isRefreshingNow: () -> Boolean) {

        if (isRefreshing || internalRefreshState != RefreshState.Idle || !isRefreshingNow()) return
        isRefreshing = true

        internalRefreshState = RefreshState.Refreshing
        animateToSpring(refreshThresholdOffset)

        while (internalRefreshState == RefreshState.Refreshing && dragOffset != refreshThresholdOffset) {
            animateToSpring(refreshThresholdOffset)
        }
    }

    internal suspend fun finishRefreshing(isRefreshingNow: () -> Boolean) {

        if (isRefreshing && !isRefreshingNow()) {

            animationJob?.cancel()

            isRefreshing = false
            internalRefreshState = RefreshState.RefreshComplete
            startManualRefreshCompleteAnimation()
        }
    }

    internal suspend fun handlePointerRelease(onRefresh: () -> Unit, isRefreshingNow: () -> Boolean) {
        if (isProcessingRelease || isRefreshing || isRebounding) return

        isTouching = false
        isProcessingRelease = true
        try {
            if (dragOffset > 0f && dragOffset >= triggerProgressOffset) {

                startRefreshing(onRefresh, isRefreshingNow)
            } else {

                try {
                    isRebounding = true
                    animateToSpring(0f)
                } finally {
                    isRebounding = false

                    if (!isTouching) resetToIdle()
                }
            }
        } finally {
            isProcessingRelease = false
        }
    }

    private suspend fun startManualRefreshCompleteAnimation() {

        val initialProgress = if (refreshThresholdOffset > 0f) {
            1f - (dragOffset / refreshThresholdOffset).coerceIn(0f, 1f)
        } else {
            1f
        }
        refreshCompleteAnimProgressState.floatValue = initialProgress
        val animatedValue = Animatable(initialProgress)
        animatedValue.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = CubicBezierEasing(0f, 0f, 0f, 0.37f)),
        ) {
            refreshCompleteAnimProgressState.floatValue = this.value

            dragOffset = refreshThresholdOffset * (1f - this.value)
            currentTouch = SpringMath.obtainTouchDistance(dragOffset, maxDragDistancePx)
        }
        internalRefreshState = RefreshState.Idle
    }

    internal fun resetToIdle() {
        if (internalRefreshState != RefreshState.Refreshing && internalRefreshState != RefreshState.RefreshComplete) {
            internalRefreshState = RefreshState.Idle
        }
    }

    internal fun getOrCreateNestedScrollConnection(
        overScrollState: OverScrollState,
    ): NestedScrollConnection {

        cachedNestedScrollConnection?.let { return it }

        return (
            object : NestedScrollConnection {
                private fun applyDrag(delta: Float) {
                    if (delta == 0f) return
                    currentTouch += delta
                    currentTouch = currentTouch.coerceIn(-maxDragDistancePx, maxDragDistancePx)

                    val normalized = min(abs(currentTouch) / maxDragDistancePx, 1.0f)
                    val dampedDist = SpringMath.obtainDampingDistance(normalized, maxDragDistancePx)
                    dragOffset = sign(currentTouch) * dampedDist

                    when {
                        dragOffset > 0f && dragOffset >= triggerProgressOffset -> RefreshState.ThresholdReached
                        dragOffset > 0 -> RefreshState.Pulling
                        else -> RefreshState.Idle
                    }.also { nextState ->
                        if (refreshState != nextState) {
                            internalRefreshState = nextState
                        }
                    }
                }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

                    if (overScrollState.isOverScrollActive && refreshState == RefreshState.Idle) return Offset.Zero

                    if (refreshState == RefreshState.Refreshing || refreshState == RefreshState.RefreshComplete) {
                        return available
                    }

                    if (source == NestedScrollSource.UserInput && isGestureActive && available.y < 0 && (dragOffset > 0f || currentTouch > 0f)) {
                        isTouching = true
                        animationJob?.cancel()
                        applyDrag(available.y)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {

                    if (refreshState == RefreshState.Refreshing || refreshState == RefreshState.RefreshComplete) {
                        return available
                    }

                    if (source == NestedScrollSource.UserInput && isGestureActive && available.y > 0f) {
                        isTouching = true
                        animationJob?.cancel()
                        applyDrag(available.y)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
            }
            ).also { cachedNestedScrollConnection = it }
    }
}

private fun createPullToRefreshConnection(
    pullToRefreshState: PullToRefreshState,
    topAppBarScrollBehavior: ScrollBehavior?,
    overScrollState: OverScrollState,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        when (pullToRefreshState.refreshState) {
            RefreshState.Idle -> {
                val consumedByAppBar = topAppBarScrollBehavior?.nestedScrollConnection?.onPreScroll(available, source) ?: Offset.Zero
                val remaining = available - consumedByAppBar
                val consumedByRefresh = pullToRefreshState
                    .getOrCreateNestedScrollConnection(overScrollState)
                    .onPreScroll(remaining, source)
                return consumedByAppBar + consumedByRefresh
            }

            RefreshState.RefreshComplete, RefreshState.Refreshing -> {
                return available
            }

            else -> {
                val consumedByRefresh = pullToRefreshState.getOrCreateNestedScrollConnection(overScrollState).onPreScroll(available, source)
                val remaining = available - consumedByRefresh
                val consumedByAppBar = topAppBarScrollBehavior?.nestedScrollConnection?.onPreScroll(remaining, source) ?: Offset.Zero
                return consumedByRefresh + consumedByAppBar
            }
        }
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        when (pullToRefreshState.refreshState) {
            RefreshState.RefreshComplete, RefreshState.Refreshing -> {
                return available
            }

            else -> {
                val consumedByAppBar =
                    topAppBarScrollBehavior?.nestedScrollConnection?.onPostScroll(consumed, available, source) ?: Offset.Zero
                val remaining = available - consumedByAppBar
                val consumedByRefresh = pullToRefreshState
                    .getOrCreateNestedScrollConnection(overScrollState)
                    .onPostScroll(consumed, remaining, source)
                return consumedByAppBar + consumedByRefresh
            }
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pullToRefreshState.refreshState != RefreshState.Idle) {
            return available
        }
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (pullToRefreshState.refreshState != RefreshState.Idle) {

            if (
                pullToRefreshState.refreshState != RefreshState.Refreshing &&
                pullToRefreshState.refreshState != RefreshState.RefreshComplete &&
                !pullToRefreshState.isRefreshing &&
                !pullToRefreshState.isRebounding &&
                pullToRefreshState.dragOffset > 0f &&
                pullToRefreshState.dragOffset < pullToRefreshState.triggerProgressOffset
            ) {
                try {
                    pullToRefreshState.isRebounding = true
                    pullToRefreshState.animateToSpring(0f)
                } finally {
                    pullToRefreshState.isRebounding = false

                    if (!pullToRefreshState.isTouching) pullToRefreshState.resetToIdle()
                }
            }
            return available
        }
        return topAppBarScrollBehavior?.nestedScrollConnection?.onPostFling(consumed, available) ?: Velocity.Zero
    }
}

@Composable
private fun RefreshHeader(
    pullToRefreshState: PullToRefreshState,
    circleSize: Dp,
    color: Color,
    refreshTexts: List<String>,
    refreshTextStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    LaunchedEffect(pullToRefreshState.refreshState) {
        if (pullToRefreshState.refreshState == RefreshState.ThresholdReached) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }

    val refreshText by remember(pullToRefreshState, refreshTexts) {
        derivedStateOf {
            when (pullToRefreshState.refreshState) {
                RefreshState.Idle -> ""

                RefreshState.Pulling -> {
                    if (pullToRefreshState.visualProgress > 0.5) refreshTexts.getOrElse(0) { "" } else ""
                }

                RefreshState.ThresholdReached -> refreshTexts.getOrElse(1) { "" }

                RefreshState.Refreshing -> refreshTexts.getOrElse(2) { "" }

                RefreshState.RefreshComplete -> refreshTexts.getOrElse(3) { "" }
            }
        }
    }

    val refreshTextAlpha by remember(pullToRefreshState) {
        derivedStateOf {
            when (pullToRefreshState.refreshState) {
                RefreshState.Idle -> 0f

                RefreshState.Pulling -> {
                    val progress = pullToRefreshState.visualProgress
                    if (progress > 0.6f) (progress - 0.5f) * 2f else 0f
                }

                RefreshState.ThresholdReached -> 1f

                RefreshState.Refreshing -> ((pullToRefreshState.visualProgress - 0.5f) * 2f).coerceIn(0f, 1f)

                RefreshState.RefreshComplete -> {
                    (1f - pullToRefreshState.refreshCompleteAnimProgress * 1.95f).coerceAtLeast(0f)
                }
            }
        }
    }

    val stretchExtraDp by remember(pullToRefreshState, density) {
        derivedStateOf {
            with(density) { (pullToRefreshState.dragOffset - pullToRefreshState.refreshThresholdOffset).toDp() }
        }
    }

    val indicatorHeight by remember(pullToRefreshState, circleSize, density) {
        derivedStateOf {
            when (pullToRefreshState.refreshState) {
                RefreshState.Idle -> 0.dp

                else -> if (stretchExtraDp <= 0.dp) {
                    circleSize * pullToRefreshState.visualProgress
                } else {
                    circleSize + stretchExtraDp
                }
            }
        }
    }

    val headerHeight by remember(pullToRefreshState, circleSize, density) {
        derivedStateOf {
            when (pullToRefreshState.refreshState) {
                RefreshState.Idle -> 0.dp

                else -> if (stretchExtraDp <= 0.dp) {
                    (circleSize + 36.dp) * pullToRefreshState.visualProgress
                } else {
                    (circleSize + 36.dp) + stretchExtraDp
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        RefreshIndicator(
            pullToRefreshState = pullToRefreshState,
            circleSize = circleSize,
            color = color,
            modifier = Modifier.height(indicatorHeight),
        )
        Text(
            text = refreshText,
            style = refreshTextStyle,
            color = color,
            modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = refreshTextAlpha },
        )
    }
}

@Composable
private fun RefreshIndicator(
    pullToRefreshState: PullToRefreshState,
    circleSize: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {

        val rotationState = if (pullToRefreshState.refreshState == RefreshState.Refreshing) {
            animateRotation()
        } else {
            null
        }
        Canvas(modifier = Modifier.size(circleSize)) {
            val ringStrokeWidthPx = circleSize.toPx() / 11
            val indicatorRadiusPx = max(size.minDimension / 2, circleSize.toPx() / 3.5f)
            val center = Offset(circleSize.toPx() / 2, circleSize.toPx() / 1.8f)

            when (pullToRefreshState.refreshState) {
                RefreshState.Idle -> return@Canvas

                RefreshState.Pulling -> {
                    if (pullToRefreshState.dragOffset > pullToRefreshState.refreshThresholdOffset) {
                        drawThresholdIndicator(
                            center,
                            indicatorRadiusPx,
                            ringStrokeWidthPx,
                            color,
                            pullToRefreshState.dragOffset,
                            pullToRefreshState.refreshThresholdOffset,
                            pullToRefreshState.maxDragDistancePx,
                        )
                    } else {
                        val alpha = (pullToRefreshState.visualProgress - 0.2f).coerceAtLeast(0f)
                        drawPullingIndicator(center, indicatorRadiusPx, ringStrokeWidthPx, color, alpha)
                    }
                }

                RefreshState.ThresholdReached -> {
                    drawThresholdIndicator(
                        center,
                        indicatorRadiusPx,
                        ringStrokeWidthPx,
                        color,
                        pullToRefreshState.dragOffset,
                        pullToRefreshState.refreshThresholdOffset,
                        pullToRefreshState.maxDragDistancePx,
                    )
                }

                RefreshState.Refreshing -> {

                    val expansionAlpha = ((pullToRefreshState.visualProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                    drawRefreshingIndicator(
                        center,
                        indicatorRadiusPx,
                        ringStrokeWidthPx,
                        color.copy(alpha = color.alpha * expansionAlpha),
                        rotationState?.value ?: 0f,
                    )
                }

                RefreshState.RefreshComplete -> {
                    drawRefreshCompleteIndicator(
                        center,
                        indicatorRadiusPx,
                        ringStrokeWidthPx,
                        color,
                        pullToRefreshState.refreshCompleteAnimProgress,
                    )
                }
            }
        }
    }
}

@Composable
private fun animateRotation(): State<Float> {
    val infiniteTransition = rememberInfiniteTransition()
    val initialRotation = remember { (0..360).random().toFloat() }
    return infiniteTransition.animateFloat(
        initialValue = initialRotation,
        targetValue = initialRotation + 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
}

private fun DrawScope.drawPullingIndicator(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawThresholdIndicator(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    color: Color,
    dragOffset: Float,
    thresholdOffset: Float,
    maxDrag: Float,
) {
    val lineLength = (dragOffset - thresholdOffset).coerceIn(0f, maxDrag - thresholdOffset)
    val topY = center.y
    val bottomY = center.y + lineLength
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius, topY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius, bottomY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
    drawLine(
        color = color,
        start = Offset(center.x - radius, topY),
        end = Offset(center.x - radius, bottomY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(center.x + radius, topY),
        end = Offset(center.x + radius, bottomY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawRefreshingIndicator(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    color: Color,
    rotation: Float,
) {
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
    val orbitRadius = radius - 2 * strokeWidth
    val angle = rotation * PI / 180.0
    val dotCenter = center + Offset(
        x = (orbitRadius * cos(angle)).toFloat(),
        y = (orbitRadius * sin(angle)).toFloat(),
    )
    drawCircle(color = color, radius = strokeWidth, center = dotCenter)
}

private fun DrawScope.drawRefreshCompleteIndicator(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    color: Color,
    progress: Float,
) {
    val animatedRadius = radius * (1f - progress).coerceAtLeast(0.9f)
    val alphaColor = color.copy(alpha = (1f - progress - 0.35f).coerceAtLeast(0f))
    val y = center.y - radius - strokeWidth + animatedRadius
    drawCircle(
        color = alphaColor,
        radius = animatedRadius,
        center = Offset(center.x, y),
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
}

private const val MAX_DRAWRATIO = 1 / 6f
private const val THRESHOLD_RADIO = 1 / 4f

internal val LocalPullToRefreshState = staticCompositionLocalOf<PullToRefreshState?> { null }

object PullToRefreshDefaults {
    val color: Color = Color.Gray
    val circleSize: Dp = 20.dp
    val refreshTexts = listOf(
        "Pull down to refresh",
        "Release to refresh",
        "Refreshing...",
        "Refreshed successfully",
    )
    val refreshTextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}
