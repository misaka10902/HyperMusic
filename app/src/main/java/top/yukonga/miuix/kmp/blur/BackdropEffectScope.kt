package top.yukonga.miuix.kmp.blur

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.blur.internal.COLOR_CONTROLS_SHADER
import top.yukonga.miuix.kmp.blur.internal.MAX_BLUR_TAPS
import top.yukonga.miuix.kmp.blur.internal.chain
import top.yukonga.miuix.kmp.blur.internal.runtimeShaderEffect as createRuntimeShaderEffect

sealed interface BackdropEffectScope :
    Density,
    RuntimeShaderCache {

    val size: Size
    val layoutDirection: LayoutDirection
    val shape: Shape

    var padding: Float

    var renderEffect: RenderEffect?

    var downscaleFactor: Int

    var noiseCoefficient: Float
}

fun BackdropEffectScope.colorControls(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f,
) {
    if (brightness == 0f && contrast == 1f && saturation == 1f) return
    if (!isRuntimeShaderSupported()) return
    val scope = impl

    val cached = scope.cachedColorResult
    val effect = if (cached != null &&
        scope.cachedColorBrightness == brightness &&
        scope.cachedColorContrast == contrast &&
        scope.cachedColorSaturation == saturation
    ) {
        cached
    } else {
        val shader = obtainRuntimeShader("ColorControls", COLOR_CONTROLS_SHADER).apply {
            setFloatUniform("in_brightness", brightness)
            setFloatUniform("in_contrast", contrast)
            setFloatUniform("in_saturation", saturation)
        }
        createRuntimeShaderEffect(shader, "child").also {
            scope.cachedColorBrightness = brightness
            scope.cachedColorContrast = contrast
            scope.cachedColorSaturation = saturation
            scope.cachedColorResult = it
        }
    }

    renderEffect = renderEffect.chain(effect)
}

fun BackdropEffectScope.effect(effect: RenderEffect) {
    renderEffect = renderEffect.chain(effect)
}

fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    shaderString: String,
    uniformShaderName: String,
    block: RuntimeShader.() -> Unit,
) {
    if (!isRuntimeShaderSupported()) return
    val effect = createRuntimeShaderEffect(
        runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
        uniformShaderName = uniformShaderName,
    )
    renderEffect = renderEffect.chain(effect)
}

internal abstract class BackdropEffectScopeImpl :
    BackdropEffectScope,
    RuntimeShaderCache {

    override var density: Float = 1f
    override var fontScale: Float = 1f
    override var size: Size = Size.Unspecified
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override var padding: Float = 0f
    override var renderEffect: RenderEffect? = null
    override var downscaleFactor: Int = 1
    override var noiseCoefficient: Float = 0f

    var runtimeShaderCache: RuntimeShaderCache = RuntimeShaderCacheImpl()

    internal val blurRawWeights: DoubleArray = DoubleArray(14)
    internal val blurParamOffsets: FloatArray = FloatArray(MAX_BLUR_TAPS)
    internal val blurParamWeights: FloatArray = FloatArray(MAX_BLUR_TAPS)

    private val shaderOffsetsByTaps: Array<FloatArray?> = arrayOfNulls(MAX_BLUR_TAPS + 1)
    private val shaderWeightsByTaps: Array<FloatArray?> = arrayOfNulls(MAX_BLUR_TAPS + 1)

    internal fun obtainShaderOffsetsBuffer(tapCount: Int): FloatArray = shaderOffsetsByTaps[tapCount]
        ?: FloatArray(tapCount * 2).also { shaderOffsetsByTaps[tapCount] = it }

    internal fun obtainShaderWeightsBuffer(tapCount: Int): FloatArray = shaderWeightsByTaps[tapCount]
        ?: FloatArray(tapCount).also { shaderWeightsByTaps[tapCount] = it }

    internal val blendModesBuffer: FloatArray = FloatArray(MAX_BLEND_LAYERS)
    internal val blendColorsBuffer: FloatArray = FloatArray(MAX_BLEND_LAYERS * 4)

    internal var blurBuiltPaddedW: Float = Float.NaN
    internal var blurBuiltPaddedH: Float = Float.NaN

    internal var cachedBlurRadiusX: Float = Float.NaN
    internal var cachedBlurRadiusY: Float = Float.NaN
    internal var cachedBlurSizeW: Float = Float.NaN
    internal var cachedBlurSizeH: Float = Float.NaN
    internal var cachedBlurExp: Int = -1
    internal var cachedBlurResult: RenderEffect? = null

    internal var cachedBlendColors: BlurColors? = null
    internal var cachedBlendResult: RenderEffect? = null
    internal var cachedColorBrightness: Float = Float.NaN
    internal var cachedColorContrast: Float = Float.NaN
    internal var cachedColorSaturation: Float = Float.NaN
    internal var cachedColorResult: RenderEffect? = null

    internal var cachedProgRadiusX: Float = Float.NaN
    internal var cachedProgRadiusY: Float = Float.NaN
    internal var cachedProgSizeW: Float = Float.NaN
    internal var cachedProgSizeH: Float = Float.NaN
    internal var cachedProgExp: Int = -1
    internal var cachedProgAngle: Float = Float.NaN
    internal var cachedProgStart: Float = Float.NaN
    internal var cachedProgEnd: Float = Float.NaN
    internal var cachedProgCurve: Float = Float.NaN
    internal var cachedProgResult: RenderEffect? = null

    internal var progressiveCompositeActive: Boolean = false

    internal var progressivePreEffect: RenderEffect? = null

    internal var progressivePostBlend: BlurColors? = null

    internal var progressiveMaskScratch: Any? = null

    internal var cachedNoiseChainInput: RenderEffect? = null
    internal var cachedNoiseChainCoeff: Float = Float.NaN
    internal var cachedNoiseChainResult: RenderEffect? = null

    internal var forcedDownscaleExp: Int = -1

    internal var blurBlendExpLo: Int = 0
    internal var blurBlendExpHi: Int = 0
    internal var blurBlendFactor: Float = 0f

    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader = runtimeShaderCache.obtainRuntimeShader(key, string)

    fun update(scope: DrawScope): Boolean {
        val newDensity = scope.density
        val newFontScale = scope.fontScale
        val newSize = scope.size
        val newLayoutDirection = scope.layoutDirection

        val changed = newDensity != density ||
            newFontScale != fontScale ||
            newSize != size ||
            newLayoutDirection != layoutDirection

        if (changed) {
            density = newDensity
            fontScale = newFontScale
            size = newSize
            layoutDirection = newLayoutDirection
        }

        return changed
    }

    fun apply(effects: BackdropEffectScope.() -> Unit) {
        padding = 0f
        renderEffect = null
        downscaleFactor = 1
        noiseCoefficient = 0f
        blurBuiltPaddedW = Float.NaN
        blurBuiltPaddedH = Float.NaN

        blurBlendExpLo = 0
        blurBlendExpHi = 0
        blurBlendFactor = 0f

        progressivePreEffect = null
        progressivePostBlend = null

        if (progressiveCompositeActive) {
            cachedProgRadiusX = Float.NaN
            cachedProgRadiusY = Float.NaN
        }
        effects()

        if (!blurBuiltPaddedW.isNaN() &&
            (blurBuiltPaddedW != size.width + padding * 2f || blurBuiltPaddedH != size.height + padding * 2f)
        ) {
            val finalPadding = padding
            renderEffect = null
            downscaleFactor = 1
            noiseCoefficient = 0f
            blurBuiltPaddedW = Float.NaN
            blurBuiltPaddedH = Float.NaN

            blurBlendExpLo = 0
            blurBlendExpHi = 0
            blurBlendFactor = 0f
            progressivePreEffect = null
            progressivePostBlend = null
            if (progressiveCompositeActive) {
                cachedProgRadiusX = Float.NaN
                cachedProgRadiusY = Float.NaN
            }
            padding = finalPadding
            effects()
        }
    }

    fun reset() {
        density = 1f
        fontScale = 1f
        size = Size.Unspecified
        layoutDirection = LayoutDirection.Ltr
        padding = 0f
        renderEffect = null
        downscaleFactor = 1
        noiseCoefficient = 0f
        blurBuiltPaddedW = Float.NaN
        blurBuiltPaddedH = Float.NaN
        cachedBlurRadiusX = Float.NaN
        cachedBlurRadiusY = Float.NaN
        cachedBlurSizeW = Float.NaN
        cachedBlurSizeH = Float.NaN
        cachedBlurExp = -1
        cachedBlurResult = null
        cachedBlendColors = null
        cachedBlendResult = null
        cachedColorBrightness = Float.NaN
        cachedColorContrast = Float.NaN
        cachedColorSaturation = Float.NaN
        cachedColorResult = null
        cachedProgRadiusX = Float.NaN
        cachedProgRadiusY = Float.NaN
        cachedProgSizeW = Float.NaN
        cachedProgSizeH = Float.NaN
        cachedProgExp = -1
        cachedProgAngle = Float.NaN
        cachedProgStart = Float.NaN
        cachedProgEnd = Float.NaN
        cachedProgCurve = Float.NaN
        cachedProgResult = null
        progressiveCompositeActive = false
        progressivePreEffect = null
        progressivePostBlend = null
        progressiveMaskScratch = null
        cachedNoiseChainInput = null
        cachedNoiseChainCoeff = Float.NaN
        cachedNoiseChainResult = null
        forcedDownscaleExp = -1
        blurBlendExpLo = 0
        blurBlendExpHi = 0
        blurBlendFactor = 0f
    }
}

internal val BackdropEffectScope.impl: BackdropEffectScopeImpl
    get() = this as BackdropEffectScopeImpl
