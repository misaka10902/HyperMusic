package top.yukonga.miuix.kmp.blur

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.drawHighlight
import top.yukonga.miuix.kmp.blur.internal.DOWNSAMPLE_2X_SHADER
import top.yukonga.miuix.kmp.blur.internal.DOWNSAMPLE_4X_SHADER
import top.yukonga.miuix.kmp.blur.internal.NOISE_DITHER_SHADER
import top.yukonga.miuix.kmp.blur.internal.ShapeProvider
import top.yukonga.miuix.kmp.blur.internal.chain
import top.yukonga.miuix.kmp.blur.internal.createProgressiveSharpOverlayEffect
import top.yukonga.miuix.kmp.blur.internal.createProgressiveStackEffect
import top.yukonga.miuix.kmp.blur.internal.progressiveLoopTapBound
import top.yukonga.miuix.kmp.blur.internal.progressiveSharpBandBounds
import top.yukonga.miuix.kmp.blur.internal.progressiveSharpLoopRadius
import top.yukonga.miuix.kmp.blur.internal.recordLayer
import top.yukonga.miuix.kmp.blur.internal.runtimeShaderEffect
import kotlin.math.ceil
import kotlin.math.floor

private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }

private const val STACK_DITHER_MAX = 0.009f

private const val NOISE_GRAIN_THRESHOLD = 0.005f

fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (BackdropEffectScope.() -> Highlight?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
    contentBlendMode: BlendMode = BlendMode.SrcOver,
    progressiveGradient: ProgressiveBlur? = null,
    enabled: Boolean = true,
): Modifier {

    val effectiveEnabled = enabled && isRuntimeShaderSupported()
    return this
        .then(
            if (layerBlock != null) Modifier.graphicsLayer(layerBlock) else Modifier,
        )
        .then(
            DrawBackdropElement(
                backdrop = backdrop,
                shape = shape,
                effects = effects,
                highlight = highlight,
                layerBlock = layerBlock,
                onDrawBehind = onDrawBehind,
                onDrawBackdrop = onDrawBackdrop,
                onDrawSurface = onDrawSurface,
                onDrawFront = onDrawFront,
                contentBlendMode = contentBlendMode,
                progressiveGradient = progressiveGradient,
                enabled = effectiveEnabled,
            ),
        )
}

private class DrawBackdropElement(
    val backdrop: Backdrop,
    val shape: () -> Shape,
    val effects: BackdropEffectScope.() -> Unit,
    val highlight: (BackdropEffectScope.() -> Highlight?)?,
    val layerBlock: (GraphicsLayerScope.() -> Unit)?,
    val onDrawBehind: (DrawScope.() -> Unit)?,
    val onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    val onDrawSurface: (DrawScope.() -> Unit)?,
    val onDrawFront: (DrawScope.() -> Unit)?,
    val contentBlendMode: BlendMode = BlendMode.SrcOver,
    val progressiveGradient: ProgressiveBlur? = null,
    val enabled: Boolean = true,
) : ModifierNodeElement<DrawBackdropNode>() {

    override fun create(): DrawBackdropNode = DrawBackdropNode(
        backdrop = backdrop,
        shape = shape,
        effects = effects,
        highlight = highlight,
        layerBlock = layerBlock,
        onDrawBehind = onDrawBehind,
        onDrawBackdrop = onDrawBackdrop,
        onDrawSurface = onDrawSurface,
        onDrawFront = onDrawFront,
        contentBlendMode = contentBlendMode,
        progressiveGradient = progressiveGradient,
        enabled = enabled,
    )

    override fun update(node: DrawBackdropNode) {
        val enabledChanged = node.enabled != enabled
        val gradientCleared = node.progressiveGradient != null && progressiveGradient == null
        node.backdrop = backdrop
        node.updateShape(shape)
        node.effects = effects
        node.highlight = highlight
        node.layerBlock = layerBlock
        node.onDrawBehind = onDrawBehind
        node.onDrawBackdrop = onDrawBackdrop
        node.onDrawSurface = onDrawSurface
        node.onDrawFront = onDrawFront
        node.contentBlendMode = contentBlendMode
        node.progressiveGradient = progressiveGradient
        node.enabled = enabled
        if (gradientCleared) {
            node.releaseCompositeResources()
        }
        if (enabledChanged) {
            if (!enabled) {
                node.releaseGraphicsLayers()
            }
            node.invalidateMeasurement()
        }
        node.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBackdrop"
        properties["backdrop"] = backdrop
        properties["enabled"] = enabled
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackdropElement) return false
        if (backdrop != other.backdrop) return false
        if (shape != other.shape) return false
        if (effects != other.effects) return false
        if (highlight != other.highlight) return false
        if (layerBlock != other.layerBlock) return false
        if (onDrawBehind != other.onDrawBehind) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false
        if (onDrawSurface != other.onDrawSurface) return false
        if (onDrawFront != other.onDrawFront) return false
        if (contentBlendMode != other.contentBlendMode) return false
        if (progressiveGradient != other.progressiveGradient) return false
        if (enabled != other.enabled) return false
        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + (highlight?.hashCode() ?: 0)
        result = 31 * result + (layerBlock?.hashCode() ?: 0)
        result = 31 * result + (onDrawBehind?.hashCode() ?: 0)
        result = 31 * result + onDrawBackdrop.hashCode()
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        result = 31 * result + (onDrawFront?.hashCode() ?: 0)
        result = 31 * result + contentBlendMode.hashCode()
        result = 31 * result + (progressiveGradient?.hashCode() ?: 0)
        result = 31 * result + enabled.hashCode()
        return result
    }
}

private class DrawBackdropNode(
    var backdrop: Backdrop,
    val shape: () -> Shape,
    var effects: BackdropEffectScope.() -> Unit,
    var highlight: (BackdropEffectScope.() -> Highlight?)?,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var onDrawBehind: (DrawScope.() -> Unit)?,
    var onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    var onDrawSurface: (DrawScope.() -> Unit)?,
    var onDrawFront: (DrawScope.() -> Unit)?,
    var contentBlendMode: BlendMode = BlendMode.SrcOver,
    var progressiveGradient: ProgressiveBlur? = null,
    var enabled: Boolean = true,
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    ObserverModifierNode,
    CompositionLocalConsumerModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    private val shapeProvider: ShapeProvider = ShapeProvider(shape)

    fun updateShape(shape: () -> Shape) {
        shapeProvider.shapeBlock = shape
    }

    private val effectScope = object : BackdropEffectScopeImpl() {
        override val shape: Shape get() = shapeProvider.innerShape
    }

    private val graphicsContext get() = requireGraphicsContext()

    private inner class LevelTarget {
        var mainLayer: GraphicsLayer? = null
        val cascadeLayers: MutableList<GraphicsLayer> = mutableListOf()
        var noiseLayer: GraphicsLayer? = null
        var downscaleFactor: Int = 1
        var padding: Float by mutableFloatStateOf(0f)

        var cascadeFirstStepScale: Int = 1

        val dsKeys = arrayOf("", "")
        val dsW = intArrayOf(-1, -1)
        val dsH = intArrayOf(-1, -1)
        val dsEffects = arrayOfNulls<RenderEffect>(2)
        var noiseEffect: RenderEffect? = null
        var noiseEffectCoeff: Float = Float.NaN

        fun ensureMain(): GraphicsLayer = mainLayer ?: graphicsContext.createGraphicsLayer().also { mainLayer = it }

        fun obtainCascade(index: Int): GraphicsLayer {
            val ctx = graphicsContext
            while (cascadeLayers.size <= index) {
                cascadeLayers.add(ctx.createGraphicsLayer())
            }
            return cascadeLayers[index]
        }

        inline fun downsampleEffect(slot: Int, key: String, w: Int, h: Int, build: () -> RenderEffect): RenderEffect {
            val cached = dsEffects[slot]
            if (cached != null && dsKeys[slot] == key && dsW[slot] == w && dsH[slot] == h) return cached
            return build().also {
                dsEffects[slot] = it
                dsKeys[slot] = key
                dsW[slot] = w
                dsH[slot] = h
            }
        }

        inline fun noiseRenderEffect(coeff: Float, build: () -> RenderEffect): RenderEffect {
            val cached = noiseEffect
            if (cached != null && noiseEffectCoeff == coeff) return cached
            return build().also {
                noiseEffect = it
                noiseEffectCoeff = coeff
            }
        }

        fun release() {
            val ctx = graphicsContext
            mainLayer?.let { ctx.releaseGraphicsLayer(it) }
            mainLayer = null
            for (cascade in cascadeLayers) ctx.releaseGraphicsLayer(cascade)
            cascadeLayers.clear()
            noiseLayer?.let { ctx.releaseGraphicsLayer(it) }
            noiseLayer = null
            dsEffects.fill(null)
            dsKeys.fill("")
            dsW.fill(-1)
            dsH.fill(-1)
            noiseEffect = null
            noiseEffectCoeff = Float.NaN
        }
    }

    private val primary = LevelTarget()
    private val secondary = LevelTarget()
    private var crossfadeResultLayer: GraphicsLayer? = null

    private var blending: Boolean by mutableStateOf(false)

    private var blendFactor: Float by mutableFloatStateOf(0f)

    private val layoutLayerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = shapeProvider.shape
        compositingStrategy = CompositingStrategy.Offscreen
    }

    private var layoutCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    private fun DrawScope.recordBackdrop(target: LevelTarget) {
        val currentPadding = target.padding
        val scaleFactor = target.cascadeFirstStepScale
        val scaledPadding = if (currentPadding == 0f) {
            0f
        } else if (scaleFactor > 1) {
            (currentPadding / scaleFactor).toInt().toFloat()
        } else {
            currentPadding
        }
        translate(scaledPadding, scaledPadding) {
            onDrawBackdrop {
                with(backdrop) {
                    drawBackdrop(
                        density = effectScope,
                        coordinates = layoutCoordinates,
                        layerBlock = layerBlock,
                        downscaleFactor = scaleFactor,
                    )
                }
            }
        }
    }

    private fun DrawScope.renderBlurInto(target: LevelTarget) {
        val layer = target.mainLayer ?: return
        val currentPadding = target.padding
        val scaleFactor = target.downscaleFactor
        val fullWidth = (size.width + currentPadding * 2).toInt()
        val fullHeight = (size.height + currentPadding * 2).toInt()

        if (scaleFactor <= 1) {
            target.cascadeFirstStepScale = 1
            recordLayer(layer, size = IntSize(fullWidth, fullHeight)) { recordBackdrop(target) }
            layer.topLeft =
                if (currentPadding != 0f) {
                    IntOffset(-currentPadding.toInt(), -currentPadding.toInt())
                } else {
                    IntOffset.Zero
                }
            drawLayer(layer)
        } else if (scaleFactor <= 2) {
            target.cascadeFirstStepScale = 2
            val w = (fullWidth / 2).coerceAtLeast(1)
            val h = (fullHeight / 2).coerceAtLeast(1)
            recordLayer(layer, size = IntSize(w, h)) { recordBackdrop(target) }
            drawUpscaledLayer(
                target,
                layer,
                scaleFactor.toFloat(),
                currentPadding,
                scaleFactor,
                backdrop.offsetResidualX,
                backdrop.offsetResidualY,
                fullWidth,
                fullHeight,
            )
        } else {

            target.cascadeFirstStepScale = 2
            val firstW = (fullWidth / 2).coerceAtLeast(1)
            val firstH = (fullHeight / 2).coerceAtLeast(1)

            val firstCascade = target.obtainCascade(0)
            recordLayer(firstCascade, size = IntSize(firstW, firstH)) { recordBackdrop(target) }

            when (scaleFactor) {
                4 -> {

                    applyDownsampleStep(
                        target = target,
                        slot = 0,
                        source = firstCascade,
                        sourceW = firstW,
                        sourceH = firstH,
                        dest = layer,
                        destW = (firstW / 2).coerceAtLeast(1),
                        destH = (firstH / 2).coerceAtLeast(1),
                        scale = 0.5f,
                        shaderKey = "Downsample2x",
                        shaderSrc = DOWNSAMPLE_2X_SHADER,
                    )
                }

                8 -> {

                    applyDownsampleStep(
                        target = target,
                        slot = 0,
                        source = firstCascade,
                        sourceW = firstW,
                        sourceH = firstH,
                        dest = layer,
                        destW = (firstW / 4).coerceAtLeast(1),
                        destH = (firstH / 4).coerceAtLeast(1),
                        scale = 0.25f,
                        shaderKey = "Downsample4x",
                        shaderSrc = DOWNSAMPLE_4X_SHADER,
                    )
                }

                16 -> {

                    val midW = (firstW / 4).coerceAtLeast(1)
                    val midH = (firstH / 4).coerceAtLeast(1)
                    val midCascade = target.obtainCascade(1)
                    applyDownsampleStep(
                        target = target,
                        slot = 0,
                        source = firstCascade,
                        sourceW = firstW,
                        sourceH = firstH,
                        dest = midCascade,
                        destW = midW,
                        destH = midH,
                        scale = 0.25f,
                        shaderKey = "Downsample4x",
                        shaderSrc = DOWNSAMPLE_4X_SHADER,
                    )
                    applyDownsampleStep(
                        target = target,
                        slot = 1,
                        source = midCascade,
                        sourceW = midW,
                        sourceH = midH,
                        dest = layer,
                        destW = (midW / 2).coerceAtLeast(1),
                        destH = (midH / 2).coerceAtLeast(1),
                        scale = 0.5f,
                        shaderKey = "Downsample2x",
                        shaderSrc = DOWNSAMPLE_2X_SHADER,
                    )
                }

                else -> error("Unsupported scaleFactor: $scaleFactor (must be 1/2/4/8/16)")
            }

            drawUpscaledLayer(
                target,
                layer,
                scaleFactor.toFloat(),
                currentPadding,
                scaleFactor,
                backdrop.offsetResidualX,
                backdrop.offsetResidualY,
                fullWidth,
                fullHeight,
            )
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            if (enabled) {
                placeable.placeWithLayer(IntOffset.Zero, layerBlock = layoutLayerBlock)
            } else {
                placeable.place(IntOffset.Zero)
            }
        }
    }

    private val contentPaint = Paint()
    private val highlightPaint = Paint()

    private var contentBoundsRect: Rect? = null

    private var sharpLayer: GraphicsLayer? = null
    private var compositeAttempted = false
    private var compositeSupported = false
    private var sharpOverlayActive = false

    private var cachedStackEffect: RenderEffect? = null
    private var cachedStackRadiusX = Float.NaN
    private var cachedStackRadiusY = Float.NaN
    private var cachedStackW = Float.NaN
    private var cachedStackH = Float.NaN
    private var cachedStackGradient: ProgressiveBlur? = null
    private var cachedStackPre: RenderEffect? = null
    private var cachedStackPost: RenderEffect? = null
    private var cachedStackPostBlend: BlurColors? = null
    private var cachedStackDither = Float.NaN

    private var stackDitherEffect: RenderEffect? = null
    private var stackDitherCoeff = Float.NaN

    private var stackDitherChained = false

    private fun obtainStackDitherEffect(coeff: Float): RenderEffect {
        val cached = stackDitherEffect
        if (cached != null && stackDitherCoeff == coeff) return cached
        val shader = effectScope.obtainRuntimeShader("NoiseDither", NOISE_DITHER_SHADER)
        shader.setFloatUniform("noise_coeff", coeff)
        return runtimeShaderEffect(shader, "child").also {
            stackDitherEffect = it
            stackDitherCoeff = coeff
        }
    }

    private var sharpOverlayEffect: RenderEffect? = null
    private var sharpOverlayGradient: ProgressiveBlur? = null
    private var sharpOverlayW = Float.NaN
    private var sharpOverlayH = Float.NaN
    private var sharpRadiusX = Float.NaN
    private var sharpRadiusY = Float.NaN
    private var sharpExp = -1
    private var sharpBandOffset = IntOffset.Zero
    private var sharpBandSize = IntSize.Zero

    private fun DrawScope.drawBlurredBackdrop() {
        renderBlurInto(primary)
        if (blending) {

            val result = crossfadeResultLayer
                ?: graphicsContext.createGraphicsLayer().also { crossfadeResultLayer = it }
            recordLayer(
                result,
                size = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
            ) {
                renderBlurInto(secondary)
            }
            result.alpha = blendFactor
            drawLayer(result)
        }
    }

    private fun DrawScope.drawSharpOverlay() {
        val bandSize = sharpBandSize
        if (bandSize.width <= 0 || bandSize.height <= 0) return
        val bandOffset = sharpBandOffset
        val layer = sharpLayer ?: graphicsContext.createGraphicsLayer().also { sharpLayer = it }
        recordLayer(layer, size = bandSize) {
            translate(-bandOffset.x.toFloat(), -bandOffset.y.toFloat()) {
                onDrawBackdrop {
                    with(backdrop) {
                        drawBackdrop(
                            density = effectScope,
                            coordinates = layoutCoordinates,
                            layerBlock = layerBlock,
                            downscaleFactor = 1,
                        )
                    }
                }
            }
        }
        layer.renderEffect = sharpOverlayEffect
        layer.topLeft = bandOffset
        drawLayer(layer)
    }

    override fun ContentDrawScope.draw() {
        if (!enabled) {
            drawContent()
            return
        }
        if (effectScope.update(this)) {
            updateEffects()
        }
        onDrawBehind?.invoke(this)

        drawBlurredBackdrop()
        if (sharpOverlayActive) {
            drawSharpOverlay()
        }

        onDrawSurface?.invoke(this)

        if (contentBlendMode == BlendMode.SrcOver) {
            drawContent()
        } else {
            val w = size.width
            val h = size.height
            val bounds = contentBoundsRect?.takeIf { it.right == w && it.bottom == h } ?: Rect(0f, 0f, w, h).also { contentBoundsRect = it }
            contentPaint.blendMode = contentBlendMode
            drawContext.canvas.saveLayer(bounds, contentPaint)
            drawContent()
            drawContext.canvas.restore()
        }

        highlight?.invoke(effectScope)?.let { resolved ->
            drawHighlight(
                highlight = resolved,
                shape = effectScope.shape,

                runtimeShaderCache = effectScope.runtimeShaderCache,
                paint = highlightPaint,
            )
        }

        onDrawFront?.invoke(this)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            if (backdrop.isCoordinatesDependent) {
                layoutCoordinates = coordinates
            } else {
                if (layoutCoordinates != null) {
                    layoutCoordinates = null
                }
            }
        }
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    fun invalidateDrawCache() {
        observeEffects()
    }

    private fun observeEffects() {
        observeReads { updateEffects() }
    }

    private fun updateEffects() {
        if (!enabled) return
        primary.ensureMain()

        effectScope.progressiveCompositeActive =
            progressiveGradient != null && !(compositeAttempted && !compositeSupported)

        effectScope.forcedDownscaleExp = -1
        effectScope.apply(effects)
        if (effectScope.progressiveCompositeActive) {

            finalizeProgressiveComposite()
        } else {
            chainFullResNoiseIfNeeded()
            sharpOverlayActive = false
            stackDitherChained = false
        }
        primary.mainLayer?.renderEffect = effectScope.renderEffect
        primary.padding = effectScope.padding
        primary.downscaleFactor = effectScope.downscaleFactor.coerceAtLeast(1)

        val expLo = effectScope.blurBlendExpLo
        val expHi = effectScope.blurBlendExpHi
        val factor = effectScope.blurBlendFactor

        if (expLo == expHi || factor <= 0.001f) {
            blending = false
            return
        }

        secondary.ensureMain()
        effectScope.forcedDownscaleExp = expHi
        effectScope.apply(effects)
        chainFullResNoiseIfNeeded()
        secondary.mainLayer?.renderEffect = effectScope.renderEffect
        secondary.padding = effectScope.padding
        secondary.downscaleFactor = effectScope.downscaleFactor.coerceAtLeast(1)
        effectScope.forcedDownscaleExp = -1

        blendFactor = factor
        blending = true
    }

    private fun finalizeProgressiveComposite() {
        val scope = effectScope
        val gradient = progressiveGradient
        val rX = scope.cachedProgRadiusX
        val rY = scope.cachedProgRadiusY
        if (gradient == null || rX.isNaN() || rY.isNaN()) {

            chainFullResNoiseIfNeeded()
            sharpOverlayActive = false
            stackDitherChained = false
            return
        }
        val paddedW = scope.cachedProgSizeW
        val paddedH = scope.cachedProgSizeH
        val pre = scope.progressivePreEffect
        var post = scope.renderEffect
        var postBlend = scope.progressivePostBlend
        if (postBlend != null && post != null) {

            post = obtainBlendColorsEffect(scope, postBlend).chain(post)
            postBlend = null
        }

        val noiseCoeff = scope.noiseCoefficient
        val ditherCoeff = when {
            noiseCoeff <= 0f -> 0f
            scope.cachedProgExp > 0 -> minOf(noiseCoeff * 2f, STACK_DITHER_MAX)
            else -> noiseCoeff
        }
        val stack = if (cachedStackEffect != null &&
            cachedStackRadiusX == rX &&
            cachedStackRadiusY == rY &&
            cachedStackW == paddedW &&
            cachedStackH == paddedH &&
            cachedStackGradient == gradient &&
            cachedStackPre === pre &&
            cachedStackPost === post &&
            cachedStackPostBlend == postBlend &&
            cachedStackDither == ditherCoeff
        ) {
            cachedStackEffect
        } else {
            createProgressiveStackEffect(
                rX,
                rY,
                scope.cachedProgExp,
                scope.padding,
                scope.size.width,
                scope.size.height,
                gradient.angle,
                gradient.startFraction,
                gradient.endFraction,
                gradient.curve,
                pre,
                post,
                postBlend,
                scope,
            )?.let { built ->
                if (ditherCoeff > 0f) built.chain(obtainStackDitherEffect(ditherCoeff)) else built
            }?.also {
                cachedStackEffect = it
                cachedStackRadiusX = rX
                cachedStackRadiusY = rY
                cachedStackW = paddedW
                cachedStackH = paddedH
                cachedStackGradient = gradient
                cachedStackPre = pre
                cachedStackPost = post
                cachedStackPostBlend = postBlend
                cachedStackDither = ditherCoeff
            }
        }
        compositeAttempted = true
        compositeSupported = stack != null
        if (stack == null) {

            scope.progressiveCompositeActive = false
            scope.apply(effects)
            chainFullResNoiseIfNeeded()
            sharpOverlayActive = false
            stackDitherChained = false
            return
        }
        scope.renderEffect = stack
        stackDitherChained = ditherCoeff > 0f && noiseCoeff <= NOISE_GRAIN_THRESHOLD

        val sw = scope.size.width
        val sh = scope.size.height
        val exp = scope.cachedProgExp
        if (exp == 0) {

            sharpOverlayActive = false
            return
        }
        if (sharpOverlayGradient != gradient || sharpOverlayW != sw || sharpOverlayH != sh ||
            sharpRadiusX != rX || sharpRadiusY != rY || sharpExp != exp
        ) {

            val loopReach =
                (progressiveLoopTapBound(maxOf(progressiveSharpLoopRadius(rX, exp), progressiveSharpLoopRadius(rY, exp))) + 1).toFloat()
            val band = progressiveSharpBandBounds(
                sw,
                sh,
                gradient.angle,
                gradient.startFraction,
                gradient.endFraction,
                gradient.curve,
                margin = loopReach,
            )
            if (band == null || band.width < 1f || band.height < 1f) {
                sharpOverlayEffect = null
                sharpBandOffset = IntOffset.Zero
                sharpBandSize = IntSize.Zero
            } else {
                val left = floor(band.left).toInt()
                val top = floor(band.top).toInt()
                sharpBandOffset = IntOffset(left, top)
                sharpBandSize = IntSize(
                    (ceil(band.right).toInt() - left).coerceAtLeast(1),
                    (ceil(band.bottom).toInt() - top).coerceAtLeast(1),
                )
                sharpOverlayEffect = createProgressiveSharpOverlayEffect(
                    rX,
                    rY,
                    exp,
                    sw,
                    sh,
                    left.toFloat(),
                    top.toFloat(),
                    sharpBandSize.width.toFloat(),
                    sharpBandSize.height.toFloat(),
                    gradient.angle,
                    gradient.startFraction,
                    gradient.endFraction,
                    gradient.curve,
                    scope,
                )
            }
            sharpOverlayGradient = gradient
            sharpOverlayW = sw
            sharpOverlayH = sh
            sharpRadiusX = rX
            sharpRadiusY = rY
            sharpExp = exp
        }
        sharpOverlayActive = sharpOverlayEffect != null
    }

    private fun chainFullResNoiseIfNeeded() {
        val noiseCoeff = effectScope.noiseCoefficient
        if (noiseCoeff > 0f && effectScope.downscaleFactor <= 1) {
            val input = effectScope.renderEffect
            if (effectScope.cachedNoiseChainResult != null &&
                effectScope.cachedNoiseChainInput === input &&
                effectScope.cachedNoiseChainCoeff == noiseCoeff
            ) {
                effectScope.renderEffect = effectScope.cachedNoiseChainResult
                return
            }
            effectScope.runtimeShaderEffect(
                key = "NoiseDither",
                shaderString = NOISE_DITHER_SHADER,
                uniformShaderName = "child",
            ) {
                setFloatUniform("noise_coeff", noiseCoeff)
            }
            effectScope.cachedNoiseChainInput = input
            effectScope.cachedNoiseChainCoeff = noiseCoeff
            effectScope.cachedNoiseChainResult = effectScope.renderEffect
        }
    }

    override fun onAttach() {
        effectScope.runtimeShaderCache = currentValueOf(LocalRuntimeShaderCache)
        if (enabled) {
            primary.ensureMain()
            observeEffects()
        }
    }

    private fun DrawScope.applyDownsampleStep(
        target: LevelTarget,
        slot: Int,
        source: GraphicsLayer,
        sourceW: Int,
        sourceH: Int,
        dest: GraphicsLayer,
        destW: Int,
        destH: Int,
        scale: Float,
        shaderKey: String,
        shaderSrc: String,
    ) {
        source.renderEffect = target.downsampleEffect(slot, shaderKey, sourceW, sourceH) {
            runtimeShaderEffect(
                runtimeShader = effectScope.obtainRuntimeShader(shaderKey, shaderSrc).apply {
                    setFloatUniform("maxCoord", sourceW - 0.5f, sourceH - 0.5f)
                },
                uniformShaderName = "child",
            )
        }
        recordLayer(dest, size = IntSize(destW, destH)) {
            scale(scale, scale, Offset.Zero) { drawLayer(source) }
        }

    }

    private fun DrawScope.drawUpscaledLayer(
        target: LevelTarget,
        layer: GraphicsLayer,
        scaleUp: Float,
        currentPadding: Float,
        scaleFactor: Int,
        residualX: Float,
        residualY: Float,
        fullWidth: Int,
        fullHeight: Int,
    ) {
        val noiseCoeff = if (stackDitherChained) 0f else effectScope.noiseCoefficient
        if (noiseCoeff > 0f) {
            layer.topLeft = IntOffset.Zero
            val noiseL = target.noiseLayer
                ?: graphicsContext.createGraphicsLayer().also { target.noiseLayer = it }
            recordLayer(noiseL, size = IntSize(fullWidth, fullHeight)) {
                scale(scaleUp, scaleUp, Offset.Zero) { drawLayer(layer) }
            }
            noiseL.renderEffect = target.noiseRenderEffect(noiseCoeff) {
                val noiseShader = effectScope.obtainRuntimeShader("NoiseDither", NOISE_DITHER_SHADER)
                noiseShader.setFloatUniform("noise_coeff", noiseCoeff)
                runtimeShaderEffect(noiseShader, "child")
            }
            noiseL.topLeft =
                if (currentPadding != 0f) {
                    IntOffset(-currentPadding.toInt(), -currentPadding.toInt())
                } else {
                    IntOffset.Zero
                }
            translate(-residualX, -residualY) { drawLayer(noiseL) }
            noiseL.renderEffect = null
        } else {
            layer.topLeft =
                if (currentPadding != 0f) {
                    IntOffset(-(currentPadding / scaleFactor).toInt(), -(currentPadding / scaleFactor).toInt())
                } else {
                    IntOffset.Zero
                }
            translate(-residualX, -residualY) {
                scale(scaleUp, scaleUp, Offset.Zero) { drawLayer(layer) }
            }
        }
    }

    fun releaseCompositeResources() {
        sharpLayer?.let { graphicsContext.releaseGraphicsLayer(it) }
        sharpLayer = null
        sharpOverlayActive = false
        cachedStackEffect = null
        cachedStackRadiusX = Float.NaN
        cachedStackRadiusY = Float.NaN
        cachedStackW = Float.NaN
        cachedStackH = Float.NaN
        cachedStackGradient = null
        cachedStackPre = null
        cachedStackPost = null
        cachedStackPostBlend = null
        cachedStackDither = Float.NaN
        stackDitherEffect = null
        stackDitherCoeff = Float.NaN
        stackDitherChained = false
        sharpOverlayEffect = null
        sharpOverlayGradient = null
        sharpOverlayW = Float.NaN
        sharpOverlayH = Float.NaN
        sharpRadiusX = Float.NaN
        sharpRadiusY = Float.NaN
        sharpExp = -1
        sharpBandOffset = IntOffset.Zero
        sharpBandSize = IntSize.Zero
    }

    fun releaseGraphicsLayers() {
        primary.release()
        secondary.release()
        crossfadeResultLayer?.let { graphicsContext.releaseGraphicsLayer(it) }
        crossfadeResultLayer = null
        releaseCompositeResources()
        blending = false
        effectScope.reset()
    }

    override fun onDetach() {
        releaseGraphicsLayers()
        layoutCoordinates = null
    }
}
