package top.yukonga.miuix.kmp.utils

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

@Stable
fun Modifier.pressable(
    interactionSource: MutableInteractionSource?,
    indication: Indication? = null,
    enabled: Boolean = true,
    role: Role? = null,
    delay: Long? = TAP_INDICATION_DELAY,
) = pressableWithIndicationIfNeeded(
    interactionSource = interactionSource,
    indication = indication,
) { intSource, indicationNodeFactory ->
    PressableElement(
        interactionSource = intSource,
        indicationNodeFactory = indicationNodeFactory,
        enabled = enabled,
        role = role,
        delay = delay,
    )
}

@Stable
internal inline fun Modifier.pressableWithIndicationIfNeeded(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    crossinline createPressable: (MutableInteractionSource?, IndicationNodeFactory?) -> Modifier,
): Modifier = this.then(
    when {

        indication is IndicationNodeFactory -> createPressable(
            interactionSource,
            indication,
        )

        indication == null -> createPressable(interactionSource, null)

        interactionSource != null -> Modifier.indication(interactionSource, indication)
            .then(createPressable(interactionSource, null))

        else -> Modifier.composed {
            val newInteractionSource = remember { MutableInteractionSource() }
            Modifier.indication(newInteractionSource, indication).then(createPressable(newInteractionSource, null))
        }
    },
)

internal const val TAP_INDICATION_DELAY: Long = 150L

private data class PressableElement(
    private val interactionSource: MutableInteractionSource?,
    private val indicationNodeFactory: IndicationNodeFactory?,
    private val enabled: Boolean,
    private val role: Role?,
    private val delay: Long?,
) : ModifierNodeElement<PressableNode>() {
    override fun create() = PressableNode(
        interactionSource,
        indicationNodeFactory,
        enabled,
        role,
        delay,
    )

    override fun update(node: PressableNode) {
        node.update(interactionSource, indicationNodeFactory, enabled, role, delay)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pressable"
        properties["enabled"] = enabled
        properties["delay"] = delay
        properties["role"] = role
        properties["interactionSource"] = interactionSource
        properties["indicationNodeFactory"] = indicationNodeFactory
    }
}

internal open class PressableNode(
    interactionSource: MutableInteractionSource?,
    indicationNodeFactory: IndicationNodeFactory?,
    enabled: Boolean,
    role: Role?,
    delay: Long?,
) : AbstractPressableNode(
    interactionSource,
    indicationNodeFactory,
    enabled,
    role,
    delay,
) {
    fun update(
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        enabled: Boolean,
        role: Role?,
        delay: Long?,
    ) {
        updateCommon(interactionSource, indicationNodeFactory, enabled, role, delay)
    }
}

internal abstract class AbstractPressableNode(
    private var interactionSource: MutableInteractionSource?,
    private var indicationNodeFactory: IndicationNodeFactory?,
    enabled: Boolean,
    private var role: Role?,
    private var delay: Long?,
) : DelegatingNode(),
    PointerInputModifierNode,
    SemanticsModifierNode,
    TraversableNode {
    protected var enabled = enabled
        private set

    final override val shouldAutoInvalidate: Boolean = false

    private var pointerInputNode: SuspendingPointerInputModifierNode? = null
    private var indicationNode: DelegatableNode? = null

    private var pressInteraction: PressInteraction.Press? = null
    private var centerOffset: Offset = Offset.Zero

    private var userProvidedInteractionSource: MutableInteractionSource? = interactionSource

    private var lazilyCreateIndication = shouldLazilyCreateIndication()

    private fun shouldLazilyCreateIndication() = userProvidedInteractionSource == null && indicationNodeFactory != null

    open fun SemanticsPropertyReceiver.applyAdditionalSemantics() {}

    protected fun updateCommon(
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        enabled: Boolean,
        role: Role?,
        delay: Long?,
    ) {
        var isIndicationNodeDirty = false

        if (userProvidedInteractionSource != interactionSource) {
            disposeInteractions()
            userProvidedInteractionSource = interactionSource
            this.interactionSource = interactionSource
            isIndicationNodeDirty = true
        }
        if (this.indicationNodeFactory != indicationNodeFactory) {
            this.indicationNodeFactory = indicationNodeFactory
            isIndicationNodeDirty = true
        }
        if (this.enabled != enabled) {
            if (!enabled) {
                disposeInteractions()
            }
            invalidateSemantics()
            this.enabled = enabled
        }
        if (this.role != role) {
            this.role = role
            invalidateSemantics()
        }
        if (this.delay != delay) {
            this.delay = delay
            invalidateSemantics()
        }
        if (lazilyCreateIndication != shouldLazilyCreateIndication()) {
            lazilyCreateIndication = shouldLazilyCreateIndication()

            if (!lazilyCreateIndication && indicationNode == null) isIndicationNodeDirty = true
        }

        if (isIndicationNodeDirty) {

            if (indicationNode != null || !lazilyCreateIndication) {
                indicationNode?.let { undelegate(it) }
                indicationNode = null
                initializeIndicationAndInteractionSourceIfNeeded()
            }
        }
    }

    final override fun onAttach() {
        if (!lazilyCreateIndication) {
            initializeIndicationAndInteractionSourceIfNeeded()
        }
    }

    final override fun onDetach() {
        disposeInteractions()

        if (userProvidedInteractionSource == null) {
            interactionSource = null
        }

        indicationNode?.let { undelegate(it) }
        indicationNode = null
    }

    protected fun disposeInteractions() {
        interactionSource?.let { interactionSource ->
            pressInteraction?.let { oldValue ->
                val interaction = PressInteraction.Cancel(oldValue)
                interactionSource.tryEmit(interaction)
            }
        }
        pressInteraction = null
    }

    private fun initializeIndicationAndInteractionSourceIfNeeded() {

        if (indicationNode != null) return
        indicationNodeFactory?.let { indicationNodeFactory ->
            if (interactionSource == null) {
                interactionSource = MutableInteractionSource()
            }
            val node = indicationNodeFactory.create(interactionSource!!)
            delegate(node)
            indicationNode = node
        }
    }

    final override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        centerOffset = bounds.center.toOffset()
        initializeIndicationAndInteractionSourceIfNeeded()
        if (pointerInputNode == null) {
            pointerInputNode = delegate(SuspendingPointerInputModifierNode { clickPointerInput() })
        }
        pointerInputNode?.onPointerEvent(pointerEvent, pass, bounds)
        if (indicationNode is PointerInputModifierNode) {
            (indicationNode as? PointerInputModifierNode)?.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    final override fun onCancelPointerInput() {

        pointerInputNode?.onCancelPointerInput()
        if (indicationNode is PointerInputModifierNode) {
            (indicationNode as? PointerInputModifierNode)?.onCancelPointerInput()
        }
    }

    final override val shouldMergeDescendantSemantics: Boolean
        get() = true

    final override fun SemanticsPropertyReceiver.applySemantics() {
        if (this@AbstractPressableNode.role != null) {
            role = this@AbstractPressableNode.role!!
        }
        if (!enabled) {
            disabled()
        }
        applyAdditionalSemantics()
    }

    protected suspend fun PointerInputScope.clickPointerInput() {
        val onPress: suspend PressGestureScope.(Offset) -> Unit = { offset ->
            if (enabled) {
                handlePressInteraction(offset)
            }
        }
        coroutineScope {
            val pressScope = PressGestureScopeImpl(this@clickPointerInput)
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val resetJob = launch { pressScope.reset() }
                launchAwaitingReset(resetJob) { pressScope.onPress(down.position) }
                val upOrCancel = waitForUpOrCancellation()
                if (upOrCancel == null) {
                    launchAwaitingReset(resetJob) { pressScope.cancel() }
                } else {
                    launchAwaitingReset(resetJob) { pressScope.release() }
                }
            }
        }
    }

    private fun CoroutineScope.launchAwaitingReset(
        resetJob: Job,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = launch {
        resetJob.join()
        block()
    }

    protected suspend fun PressGestureScope.handlePressInteraction(offset: Offset) {
        interactionSource?.let { interactionSource ->
            coroutineScope {
                val delayJob = launch {
                    delay?.let { delay(it) }
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    pressInteraction = press
                }
                val success = tryAwaitRelease()
                if (delayJob.isActive) {
                    delayJob.cancelAndJoin()

                    if (success) {
                        val press = PressInteraction.Press(offset)
                        val release = PressInteraction.Release(press)
                        interactionSource.emit(press)
                        interactionSource.emit(release)
                    }
                } else {
                    pressInteraction?.let { pressInteraction ->
                        val endInteraction = if (success) {
                            PressInteraction.Release(pressInteraction)
                        } else {
                            PressInteraction.Cancel(pressInteraction)
                        }
                        interactionSource.emit(endInteraction)
                    }
                }
                pressInteraction = null
            }
        }
    }

    override val traverseKey: Any = TraverseKey

    companion object TraverseKey
}

internal class PressGestureScopeImpl(density: Density) :
    PressGestureScope,
    Density by density {
    private var isReleased = false
    private var isCanceled = false
    private val mutex = Mutex(locked = false)

    fun cancel() {
        isCanceled = true
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    fun release() {
        isReleased = true
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    suspend fun reset() {
        mutex.lock()
        isReleased = false
        isCanceled = false
    }

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw GestureCancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        if (!isReleased && !isCanceled) {
            mutex.lock()
            mutex.unlock()
        }
        return isReleased
    }
}
