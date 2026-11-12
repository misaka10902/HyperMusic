


package top.yukonga.miuix.kmp.blur

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import top.yukonga.miuix.kmp.blur.internal.BLEND_MODE_SHADER_EXTENDED
import top.yukonga.miuix.kmp.blur.internal.BLEND_MODE_SHADER_EXTENDED_RAMP
import top.yukonga.miuix.kmp.blur.internal.BLEND_MODE_SHADER_STANDARD
import top.yukonga.miuix.kmp.blur.internal.BLEND_MODE_SHADER_STANDARD_RAMP
import top.yukonga.miuix.kmp.blur.internal.BLUR_KERNEL_REACH
import top.yukonga.miuix.kmp.blur.internal.BLUR_RADIUS_TO_SIGMA
import top.yukonga.miuix.kmp.blur.internal.PROGRESSIVE_LEVEL_FRACTION_1
import top.yukonga.miuix.kmp.blur.internal.adjustedVarianceForExp
import top.yukonga.miuix.kmp.blur.internal.chain
import top.yukonga.miuix.kmp.blur.internal.computeDownScaleBlend
import top.yukonga.miuix.kmp.blur.internal.createBlurEffect
import top.yukonga.miuix.kmp.blur.internal.createProgressiveBlurEffect
import top.yukonga.miuix.kmp.blur.internal.downScaleExpFor
import top.yukonga.miuix.kmp.blur.internal.runtimeShaderEffect as createRuntimeShaderEffect


internal const val MAX_BLEND_LAYERS = 8


fun BackdropEffectScope.blur(radiusX: Float, radiusY: Float = radiusX) {
    if (!isRuntimeShaderSupported()) return
    val scope = impl
    val sigmaX = radiusX * BLUR_RADIUS_TO_SIGMA
    val sigmaY = radiusY * BLUR_RADIUS_TO_SIGMA



    val exp = if (scope.forcedDownscaleExp >= 0) {
        scope.forcedDownscaleExp
    } else {
        val bracket = computeDownScaleBlend(maxOf(sigmaX, sigmaY))
        scope.blurBlendExpLo = bracket.expLo
        scope.blurBlendExpHi = bracket.expHi
        scope.blurBlendFactor = bracket.blend
        bracket.expLo
    }
    val sf = 1 shl exp



    val kernelPadding = (BLUR_KERNEL_REACH * sf).toFloat()
    if (kernelPadding > padding) {
        padding = kernelPadding
    }

    val paddedW = size.width + padding * 2f
    val paddedH = size.height + padding * 2f


    scope.blurBuiltPaddedW = paddedW
    scope.blurBuiltPaddedH = paddedH

    val effect = if (scope.cachedBlurResult != null &&
        scope.cachedBlurRadiusX == radiusX &&
        scope.cachedBlurRadiusY == radiusY &&
        scope.cachedBlurSizeW == paddedW &&
        scope.cachedBlurSizeH == paddedH &&
        scope.cachedBlurExp == exp
    ) {
        scope.cachedBlurResult
    } else {
        createBlurEffect(
            radiusX,
            radiusY,
            sf,
            adjustedVarianceForExp(sigmaX * sigmaX, exp),
            adjustedVarianceForExp(sigmaY * sigmaY, exp),
            Size(paddedW, paddedH),
            scope,
        ).also {
            scope.cachedBlurRadiusX = radiusX
            scope.cachedBlurRadiusY = radiusY
            scope.cachedBlurSizeW = paddedW
            scope.cachedBlurSizeH = paddedH
            scope.cachedBlurExp = exp
            scope.cachedBlurResult = it
        }
    } ?: return

    downscaleFactor = sf
    renderEffect = renderEffect?.chain(effect) ?: effect
}


fun BackdropEffectScope.progressiveBlur(
    radiusX: Float,
    radiusY: Float = radiusX,
    gradient: ProgressiveBlur = ProgressiveBlur.Top,
) {
    if (!isRuntimeShaderSupported()) return
    val scope = impl
    if (scope.progressiveCompositeActive) {




        val rX = radiusX.coerceAtLeast(0f)
        val rY = radiusY.coerceAtLeast(0f)




        val sigma1 = maxOf(rX, rY) * BLUR_RADIUS_TO_SIGMA * PROGRESSIVE_LEVEL_FRACTION_1
        val exp = downScaleExpFor(sigma1 * sigma1)
        val sf = 1 shl exp
        val kernelPadding = (BLUR_KERNEL_REACH * sf).toFloat()
        if (kernelPadding > padding) {
            padding = kernelPadding
        }
        val paddedW = size.width + padding * 2f
        val paddedH = size.height + padding * 2f

        scope.blurBuiltPaddedW = paddedW
        scope.blurBuiltPaddedH = paddedH
        scope.cachedProgRadiusX = rX
        scope.cachedProgRadiusY = rY
        scope.cachedProgExp = exp
        scope.cachedProgSizeW = paddedW
        scope.cachedProgSizeH = paddedH
        scope.cachedProgResult = null

        scope.progressivePreEffect = renderEffect
        renderEffect = null
        downscaleFactor = sf
        return
    }
    if (radiusX <= 0f && radiusY <= 0f) return
    val sigmaX = radiusX * BLUR_RADIUS_TO_SIGMA
    val sigmaY = radiusY * BLUR_RADIUS_TO_SIGMA



    val exp = downScaleExpFor(maxOf(sigmaX * sigmaX, sigmaY * sigmaY))
    val sf = 1 shl exp

    val kernelPadding = (BLUR_KERNEL_REACH * sf).toFloat()
    if (kernelPadding > padding) {
        padding = kernelPadding
    }

    val compW = size.width
    val compH = size.height
    val paddedW = compW + padding * 2f
    val paddedH = compH + padding * 2f


    scope.blurBuiltPaddedW = paddedW
    scope.blurBuiltPaddedH = paddedH

    val effect = if (scope.cachedProgResult != null &&
        scope.cachedProgRadiusX == radiusX &&
        scope.cachedProgRadiusY == radiusY &&
        scope.cachedProgSizeW == paddedW &&
        scope.cachedProgSizeH == paddedH &&
        scope.cachedProgExp == exp &&
        scope.cachedProgAngle == gradient.angle &&
        scope.cachedProgStart == gradient.startFraction &&
        scope.cachedProgEnd == gradient.endFraction &&
        scope.cachedProgCurve == gradient.curve
    ) {
        scope.cachedProgResult
    } else {
        createProgressiveBlurEffect(
            radiusX,
            radiusY,
            sf,
            adjustedVarianceForExp(sigmaX * sigmaX, exp),
            adjustedVarianceForExp(sigmaY * sigmaY, exp),
            Size(paddedW, paddedH),
            padding,
            compW,
            compH,
            gradient.angle,
            gradient.startFraction,
            gradient.endFraction,
            gradient.curve,
            scope,
        ).also {
            scope.cachedProgRadiusX = radiusX
            scope.cachedProgRadiusY = radiusY
            scope.cachedProgSizeW = paddedW
            scope.cachedProgSizeH = paddedH
            scope.cachedProgExp = exp
            scope.cachedProgAngle = gradient.angle
            scope.cachedProgStart = gradient.startFraction
            scope.cachedProgEnd = gradient.endFraction
            scope.cachedProgCurve = gradient.curve
            scope.cachedProgResult = it
        }
    } ?: return

    downscaleFactor = sf
    renderEffect = renderEffect?.chain(effect) ?: effect
}


fun BackdropEffectScope.noiseDither(coefficient: Float) {
    if (coefficient <= 0f) return
    noiseCoefficient = coefficient
}


fun BackdropEffectScope.blendColors(colors: BlurColors) {
    if (colors.blendColors.isEmpty()) return
    if (!isRuntimeShaderSupported()) return
    val scope = impl



    if (scope.progressiveCompositeActive && !scope.cachedProgRadiusX.isNaN() &&
        renderEffect == null && scope.progressivePostBlend == null
    ) {
        scope.progressivePostBlend = colors
        return
    }

    renderEffect = renderEffect.chain(obtainBlendColorsEffect(scope, colors))
}


internal fun obtainBlendColorsEffect(scope: BackdropEffectScopeImpl, colors: BlurColors): RenderEffect {
    val cached = scope.cachedBlendResult
    if (cached != null && scope.cachedBlendColors == colors) return cached

    val needsExtended = colors.needsExtendedBlend()
    val shader = scope.obtainRuntimeShader(
        if (needsExtended) "MiBlendModesExt" else "MiBlendModesStd",
        if (needsExtended) BLEND_MODE_SHADER_EXTENDED else BLEND_MODE_SHADER_STANDARD,
    )
    shader.setBlendLayerUniforms(scope, colors, needsExtended)
    return createRuntimeShaderEffect(shader, "child").also {
        scope.cachedBlendColors = colors
        scope.cachedBlendResult = it
    }
}


internal fun buildRampBlendColorsEffect(
    scope: BackdropEffectScopeImpl,
    colors: BlurColors,
    ax: Float,
    ay: Float,
    projFull: Float,
    projZero: Float,
    curve: Float,
): RenderEffect {
    val needsExtended = colors.needsExtendedBlend()
    val shader = scope.obtainRuntimeShader(
        if (needsExtended) "MiBlendModesExtRamp" else "MiBlendModesStdRamp",
        if (needsExtended) BLEND_MODE_SHADER_EXTENDED_RAMP else BLEND_MODE_SHADER_STANDARD_RAMP,
    )
    shader.setBlendLayerUniforms(scope, colors, needsExtended)
    shader.setFloatUniform("in_gradAxis", ax, ay)
    shader.setFloatUniform("in_gradBand", projFull, projZero)
    shader.setFloatUniform("in_curve", curve)
    return createRuntimeShaderEffect(shader, "child")
}

private fun BlurColors.needsExtendedBlend(): Boolean {
    val layerCount = minOf(blendColors.size, MAX_BLEND_LAYERS)
    return (0 until layerCount).any { blendColors[it].mode.value >= 100 }
}

private fun RuntimeShader.setBlendLayerUniforms(scope: BackdropEffectScopeImpl, colors: BlurColors, needsExtended: Boolean) {
    val layerList = colors.blendColors
    val layerCount = minOf(layerList.size, MAX_BLEND_LAYERS)
    val modes = scope.blendModesBuffer
    val colorData = scope.blendColorsBuffer

    setFloatUniform("layerCount", layerCount.toFloat())



    for (i in 0 until layerCount) {
        val entry = layerList[i]
        modes[i] = entry.mode.value.toFloat()
        val c = entry.color.convert(ColorSpaces.Srgb)
        val a = c.alpha
        colorData[i * 4] = c.red * a
        colorData[i * 4 + 1] = c.green * a
        colorData[i * 4 + 2] = c.blue * a
        colorData[i * 4 + 3] = a
    }
    for (i in layerCount until MAX_BLEND_LAYERS) {
        modes[i] = 0f
        colorData[i * 4] = 0f
        colorData[i * 4 + 1] = 0f
        colorData[i * 4 + 2] = 0f
        colorData[i * 4 + 3] = 0f
    }
    setFloatUniform("blendModes", modes)
    setFloatUniform("layerColors", colorData)

    if (needsExtended) {
        setFloatUniform("uSaturation", colors.saturation)
        setFloatUniform("uBrightness", colors.brightness)
        setFloatUniform("uLuminanceAmount", 0f)
        setFloatUniform("uLuminanceValues", 0f, 0f, 0f, 0f)
    }
}


fun BackdropEffectScope.textureBlurEffect(
    blurRadiusX: Float,
    blurRadiusY: Float = blurRadiusX,
    noiseCoefficient: Float = BlurDefaults.NoiseCoefficient,
    colors: BlurColors = BlurColors(),
) {
    val clampedX = blurRadiusX.coerceIn(0f, BlurDefaults.MaxBlurRadius)
    val clampedY = blurRadiusY.coerceIn(0f, BlurDefaults.MaxBlurRadius)
    noiseDither(noiseCoefficient)
    colorControls(colors.brightness, colors.contrast, colors.saturation)
    blur(clampedX * density, clampedY * density)
    blendColors(colors)
}


fun BackdropEffectScope.progressiveTextureBlurEffect(
    blurRadiusX: Float,
    blurRadiusY: Float = blurRadiusX,
    gradient: ProgressiveBlur = ProgressiveBlur.Top,
    noiseCoefficient: Float = BlurDefaults.ProgressiveNoiseCoefficient,
    colors: BlurColors = BlurColors(),
) {
    val clampedX = blurRadiusX.coerceIn(0f, BlurDefaults.MaxBlurRadius)
    val clampedY = blurRadiusY.coerceIn(0f, BlurDefaults.MaxBlurRadius)
    noiseDither(noiseCoefficient)
    colorControls(colors.brightness, colors.contrast, colors.saturation)
    progressiveBlur(clampedX * density, clampedY * density, gradient)
    blendColors(colors)
}
