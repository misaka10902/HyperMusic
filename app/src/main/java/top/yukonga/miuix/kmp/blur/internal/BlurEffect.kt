package top.yukonga.miuix.kmp.blur.internal

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import top.yukonga.miuix.kmp.blur.BackdropEffectScopeImpl
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.buildRampBlendColorsEffect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal const val BLUR_RADIUS_TO_SIGMA = 0.45f

internal const val BLUR_KERNEL_REACH = 13

private const val WEIGHT_THRESHOLD = 0.002

private const val MAX_DOWNSCALE_EXP = 4

private val BLUR_H_KEYS: Array<Array<String>> = Array(MAX_BLUR_TAPS + 1) { n ->
    Array(MAX_DOWNSCALE_EXP + 1) { exp -> "LMGauss${n}_H_d${1 shl exp}" }
}
private val BLUR_V_KEYS: Array<Array<String>> = Array(MAX_BLUR_TAPS + 1) { n ->
    Array(MAX_DOWNSCALE_EXP + 1) { exp -> "LMGauss${n}_V_d${1 shl exp}" }
}

internal fun createBlurEffect(
    radiusX: Float,
    radiusY: Float,
    downScale: Int,
    adjustedVarianceX: Float,
    adjustedVarianceY: Float,
    size: Size,
    scope: BackdropEffectScopeImpl,
): RenderEffect? {
    if (radiusX <= 0f && radiusY <= 0f) return null

    val texW = (size.width.toInt() / downScale).coerceAtLeast(1).toFloat()
    val texH = (size.height.toInt() / downScale).coerceAtLeast(1).toFloat()

    val rawScratch = scope.blurRawWeights
    val paramOffsets = scope.blurParamOffsets
    val paramWeights = scope.blurParamWeights

    val exp = downScale.countTrailingZeroBits()

    var effect: RenderEffect? = null

    if (radiusX > 0f) {
        val n = computeBlurParamsInto(adjustedVarianceX, rawScratch, paramOffsets, paramWeights)
        if (n > 0) {
            val shaderOffsets = scope.obtainShaderOffsetsBuffer(n)
            val shaderWeights = scope.obtainShaderWeightsBuffer(n)
            for (i in 0 until n) {
                shaderOffsets[i * 2] = paramOffsets[i]
                shaderOffsets[i * 2 + 1] = 0f
                shaderWeights[i] = paramWeights[i]
            }
            val hShader = scope.obtainRuntimeShader(
                BLUR_H_KEYS[n][exp],
                BLUR_SHADER_BY_TAP[n],
            ).apply {
                setFloatUniform("in_blurOffset", shaderOffsets)
                setFloatUniform("in_blurWeight", shaderWeights)
                setFloatUniform("in_maxCoord", texW - 0.5f, texH - 0.5f)
            }
            effect = runtimeShaderEffect(hShader, "child")
        }
    }

    if (radiusY > 0f) {
        val n = computeBlurParamsInto(adjustedVarianceY, rawScratch, paramOffsets, paramWeights)
        if (n > 0) {
            val shaderOffsets = scope.obtainShaderOffsetsBuffer(n)
            val shaderWeights = scope.obtainShaderWeightsBuffer(n)
            for (i in 0 until n) {
                shaderOffsets[i * 2] = 0f
                shaderOffsets[i * 2 + 1] = paramOffsets[i]
                shaderWeights[i] = paramWeights[i]
            }
            val vShader = scope.obtainRuntimeShader(
                BLUR_V_KEYS[n][exp],
                BLUR_SHADER_BY_TAP[n],
            ).apply {
                setFloatUniform("in_blurOffset", shaderOffsets)
                setFloatUniform("in_blurWeight", shaderWeights)
                setFloatUniform("in_maxCoord", texW - 0.5f, texH - 0.5f)
            }
            effect = effect?.chain(runtimeShaderEffect(vShader, "child"))
                ?: runtimeShaderEffect(vShader, "child")
        }
    }

    return effect
}

internal fun createProgressiveBlurEffect(
    radiusX: Float,
    radiusY: Float,
    downScale: Int,
    adjustedVarianceX: Float,
    adjustedVarianceY: Float,
    size: Size,
    padding: Float,
    compW: Float,
    compH: Float,
    angleDeg: Float,
    startFraction: Float,
    endFraction: Float,
    curve: Float,
    scope: BackdropEffectScopeImpl,
): RenderEffect? {
    if (radiusX <= 0f && radiusY <= 0f) return null

    val texW = (size.width.toInt() / downScale).coerceAtLeast(1).toFloat()
    val texH = (size.height.toInt() / downScale).coerceAtLeast(1).toFloat()

    val rad = angleDeg * (PI / 180.0)
    val ax = cos(rad).toFloat()
    val ay = sin(rad).toFloat()
    val projMin = (if (ax > 0f) 0f else ax * compW) + (if (ay > 0f) 0f else ay * compH)
    val projMax = (if (ax > 0f) ax * compW else 0f) + (if (ay > 0f) ay * compH else 0f)
    val span = projMax - projMin
    val projFullComp = projMin + startFraction * span
    val projZeroComp = projMin + endFraction * span
    val padProj = padding * (ax + ay)
    val projFull = (projFullComp + padProj) / downScale
    var projZero = (projZeroComp + padProj) / downScale

    if (abs(projZero - projFull) < 1e-3f) projZero = projFull + 1e-3f

    val maxCap = PROGRESSIVE_BLUR_MAX_TAP.toFloat()
    val maxRadiusX = (2f * sqrt(adjustedVarianceX) - 1f).coerceIn(0f, maxCap)
    val maxRadiusY = (2f * sqrt(adjustedVarianceY) - 1f).coerceIn(0f, maxCap)

    var effect: RenderEffect? = null

    val clampedCurve = curve.coerceIn(0.05f, 20f)

    if (radiusX > 0f && maxRadiusX >= 0.5f) {
        val tap = progressiveLoopTapBound(maxRadiusX)
        val hShader = scope.obtainRuntimeShader(
            progressiveLoopShaderKey(axis = 0, downScale = downScale, tap = tap),
            progressiveBlurShaderForTap(tap),
        ).apply {
            setFloatUniform("in_maxCoord", texW - 0.5f, texH - 0.5f)
            setFloatUniform("in_step", 1f, 0f)
            setFloatUniform("in_maxRadius", maxRadiusX)
            setFloatUniform("in_gradAxis", ax, ay)
            setFloatUniform("in_gradBand", projFull, projZero)
            setFloatUniform("in_curve", clampedCurve)
        }
        effect = runtimeShaderEffect(hShader, "child")
    }

    if (radiusY > 0f && maxRadiusY >= 0.5f) {
        val tap = progressiveLoopTapBound(maxRadiusY)
        val vShader = scope.obtainRuntimeShader(
            progressiveLoopShaderKey(axis = 1, downScale = downScale, tap = tap),
            progressiveBlurShaderForTap(tap),
        ).apply {
            setFloatUniform("in_maxCoord", texW - 0.5f, texH - 0.5f)
            setFloatUniform("in_step", 0f, 1f)
            setFloatUniform("in_maxRadius", maxRadiusY)
            setFloatUniform("in_gradAxis", ax, ay)
            setFloatUniform("in_gradBand", projFull, projZero)
            setFloatUniform("in_curve", clampedCurve)
        }
        effect = effect?.chain(runtimeShaderEffect(vShader, "child"))
            ?: runtimeShaderEffect(vShader, "child")
    }

    return effect
}

internal fun createProgressiveStackEffect(
    radiusX: Float,
    radiusY: Float,
    exp: Int,
    padding: Float,
    compW: Float,
    compH: Float,
    angleDeg: Float,
    startFraction: Float,
    endFraction: Float,
    curve: Float,
    preEffect: RenderEffect?,
    postEffect: RenderEffect?,
    postBlend: BlurColors?,
    scope: BackdropEffectScopeImpl,
): RenderEffect? {
    val downScale = 1 shl exp
    val sigmaX = radiusX.coerceAtLeast(0f) * BLUR_RADIUS_TO_SIGMA
    val sigmaY = radiusY.coerceAtLeast(0f) * BLUR_RADIUS_TO_SIGMA

    val rad = angleDeg * (PI / 180.0)
    val ax = cos(rad).toFloat()
    val ay = sin(rad).toFloat()
    val projMin = (if (ax > 0f) 0f else ax * compW) + (if (ay > 0f) 0f else ay * compH)
    val projMax = (if (ax > 0f) ax * compW else 0f) + (if (ay > 0f) ay * compH else 0f)
    val span = projMax - projMin
    val padProj = padding * (ax + ay)
    val projFull = (projMin + startFraction * span + padProj) / downScale
    var projZero = (projMin + endFraction * span + padProj) / downScale
    if (abs(projZero - projFull) < 1e-3f) projZero = projFull + 1e-3f

    fun levelSigma(sigma: Float, fraction: Float): Float {
        val target = sigma * fraction
        if (target <= 0f) return 0f
        val residual = (target * target - IMPLIED_BOX_VARIANCE[exp]) / (downScale * downScale)
        return if (residual > 0.01f) sqrt(residual) else 0f
    }

    val clampedCurve = curve.coerceIn(0.05f, 20f)
    val stack = progressiveStackEffect(
        scope,
        levelSigma(sigmaX, PROGRESSIVE_LEVEL_FRACTION_0),
        levelSigma(sigmaY, PROGRESSIVE_LEVEL_FRACTION_0),
        levelSigma(sigmaX, PROGRESSIVE_LEVEL_FRACTION_1),
        levelSigma(sigmaY, PROGRESSIVE_LEVEL_FRACTION_1),
        levelSigma(sigmaX, PROGRESSIVE_LEVEL_FRACTION_2),
        levelSigma(sigmaY, PROGRESSIVE_LEVEL_FRACTION_2),
        ax,
        ay,
        projFull,
        projZero,
        clampedCurve,
        preEffect,
        postEffect,
        nativeSharpEnd = exp == 0,
    ) ?: return null
    return if (postBlend != null) {
        stack.chain(buildRampBlendColorsEffect(scope, postBlend, ax, ay, projFull, projZero, clampedCurve))
    } else {
        stack
    }
}

internal fun progressiveSharpLoopRadius(radius: Float, exp: Int): Float {
    val sigma = radius.coerceAtLeast(0f) * BLUR_RADIUS_TO_SIGMA * PROGRESSIVE_LEVEL_FRACTION_2
    val effSigma = sqrt(maxOf(sigma * sigma, IMPLIED_BOX_VARIANCE[exp]))
    return (2f * effSigma - 1f).coerceIn(0f, PROGRESSIVE_BLUR_MAX_TAP.toFloat())
}

private fun progressiveSharpRawThreshold(curve: Float): Float {
    val ssThr = (2.0 / 3.0).pow(1.0 / curve.coerceIn(0.05f, 20f))

    return (0.5 - sin(asin(1.0 - 2.0 * ssThr) / 3.0)).toFloat()
}

internal fun createProgressiveSharpOverlayEffect(
    radiusX: Float,
    radiusY: Float,
    exp: Int,
    compW: Float,
    compH: Float,
    originX: Float,
    originY: Float,
    bandW: Float,
    bandH: Float,
    angleDeg: Float,
    startFraction: Float,
    endFraction: Float,
    curve: Float,
    scope: BackdropEffectScopeImpl,
): RenderEffect {
    val rad = angleDeg * (PI / 180.0)
    val ax = cos(rad).toFloat()
    val ay = sin(rad).toFloat()
    val projMin = (if (ax > 0f) 0f else ax * compW) + (if (ay > 0f) 0f else ay * compH)
    val projMax = (if (ax > 0f) ax * compW else 0f) + (if (ay > 0f) ay * compH else 0f)
    val span = projMax - projMin
    val projFull = projMin + startFraction * span
    var projZero = projMin + endFraction * span
    if (abs(projZero - projFull) < 1e-3f) projZero = projFull + 1e-3f
    val originProj = originX * ax + originY * ay
    val clampedCurve = curve.coerceIn(0.05f, 20f)

    val pThr = projFull + progressiveSharpRawThreshold(curve) * (projZero - projFull)
    val loopFull = pThr - originProj
    var loopZero = projZero - originProj
    if (abs(loopZero - loopFull) < 1e-3f) loopZero = loopFull + 1e-3f

    val maxRadiusX = progressiveSharpLoopRadius(radiusX, exp)
    val maxRadiusY = progressiveSharpLoopRadius(radiusY, exp)

    val hasH = maxRadiusX >= 0.5f
    val hasV = maxRadiusY >= 0.5f

    fun loopPass(axis: Int, maxRadius: Float, stepX: Float, stepY: Float, masked: Boolean): RenderEffect {
        val tap = progressiveLoopTapBound(maxRadius)
        val shader = scope.obtainRuntimeShader(
            progressiveSharpShaderKey(axis = axis, tap = tap, masked = masked),
            progressiveBlurShaderForTap(tap, masked),
        ).apply {
            setFloatUniform("in_maxCoord", bandW - 0.5f, bandH - 0.5f)
            setFloatUniform("in_step", stepX, stepY)
            setFloatUniform("in_maxRadius", maxRadius)
            setFloatUniform("in_gradAxis", ax, ay)
            setFloatUniform("in_gradBand", loopFull, loopZero)
            setFloatUniform("in_curve", 1f)
            if (masked) {
                setFloatUniform("in_maskBand", projFull - originProj, projZero - originProj)
                setFloatUniform("in_maskCurve", clampedCurve)
            }
        }
        return runtimeShaderEffect(shader, "child")
    }

    return when {
        hasH && hasV -> loopPass(0, maxRadiusX, 1f, 0f, masked = false)
            .chain(loopPass(1, maxRadiusY, 0f, 1f, masked = true))

        hasH -> loopPass(0, maxRadiusX, 1f, 0f, masked = true)

        hasV -> loopPass(1, maxRadiusY, 0f, 1f, masked = true)

        else -> {
            val mask = scope.obtainRuntimeShader("ProgSharpRamp", PROGRESSIVE_LEVEL_MASK_SHADER).apply {
                setFloatUniform("in_gradAxis", ax, ay)
                setFloatUniform("in_gradBand", projFull - originProj, projZero - originProj)
                setFloatUniform("in_curve", clampedCurve)
                setFloatUniform("in_level", -2f)
                setFloatUniform("in_slope", -3f)
            }
            runtimeShaderEffect(mask, "child")
        }
    }
}

internal fun progressiveSharpBandBounds(
    compW: Float,
    compH: Float,
    angleDeg: Float,
    startFraction: Float,
    endFraction: Float,
    curve: Float,
    margin: Float,
): Rect? {
    val rad = angleDeg * (PI / 180.0)
    val ax = cos(rad).toFloat()
    val ay = sin(rad).toFloat()
    val projMin = (if (ax > 0f) 0f else ax * compW) + (if (ay > 0f) 0f else ay * compH)
    val projMax = (if (ax > 0f) ax * compW else 0f) + (if (ay > 0f) ay * compH else 0f)
    val span = projMax - projMin
    val projFull = projMin + startFraction * span
    var projZero = projMin + endFraction * span
    if (abs(projZero - projFull) < 1e-3f) projZero = projFull + 1e-3f

    val pThr = projFull + progressiveSharpRawThreshold(curve) * (projZero - projFull)
    val sign = if (projZero >= projFull) 1f else -1f

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    fun include(x: Float, y: Float) {
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
    }

    val xs = floatArrayOf(0f, compW, compW, 0f)
    val ys = floatArrayOf(0f, 0f, compH, compH)
    for (i in 0..3) {
        val j = (i + 1) and 3
        val di = (xs[i] * ax + ys[i] * ay - pThr) * sign
        val dj = (xs[j] * ax + ys[j] * ay - pThr) * sign
        if (di >= 0f) include(xs[i], ys[i])
        if ((di < 0f) != (dj < 0f)) {
            val t = di / (di - dj)
            include(xs[i] + (xs[j] - xs[i]) * t, ys[i] + (ys[j] - ys[i]) * t)
        }
    }
    if (minX > maxX || minY > maxY) return null

    return Rect(
        (minX - margin).coerceAtLeast(0f),
        (minY - margin).coerceAtLeast(0f),
        (maxX + margin).coerceAtMost(compW),
        (maxY + margin).coerceAtMost(compH),
    )
}

private val IMPLIED_BOX_VARIANCE = floatArrayOf(0f, 3.0265f, 7.5625f, 9.0f, 202.696f)

internal fun downScaleExpFor(sigmaSquared: Float): Int = when {
    sigmaSquared >= 1945f -> 4
    sigmaSquared > 400f -> 3
    sigmaSquared >= 90.25f -> 2
    sigmaSquared >= 12.6f -> 1
    else -> 0
}

internal fun adjustedVarianceForExp(sigmaSquared: Float, exp: Int): Float {
    val ds = (1 shl exp).toFloat()
    return ((sigmaSquared - IMPLIED_BOX_VARIANCE[exp]) / (ds * ds)).coerceAtLeast(0.1f)
}

internal data class DownScaleBlend(val expLo: Int, val expHi: Int, val blend: Float)

private val BOUNDARY_SIGMA = floatArrayOf(3.5496478f, 9.5f, 20f, 44.10215f)

private const val BLEND_BAND_FRACTION = 0.12f

internal fun computeDownScaleBlend(sigma: Float): DownScaleBlend {
    for (b in BOUNDARY_SIGMA.indices) {
        val lo = BOUNDARY_SIGMA[b] * (1f - BLEND_BAND_FRACTION)
        val hi = BOUNDARY_SIGMA[b] * (1f + BLEND_BAND_FRACTION)
        if (sigma > lo && sigma < hi) {
            val tRaw = ((sigma - lo) / (hi - lo)).coerceIn(0f, 1f)
            return DownScaleBlend(expLo = b, expHi = b + 1, blend = tRaw * tRaw * (3f - 2f * tRaw))
        }
    }
    val exp = downScaleExpFor(sigma * sigma)
    return DownScaleBlend(expLo = exp, expHi = exp, blend = 0f)
}

internal fun computeBlurParamsInto(
    variance: Float,
    rawScratch: DoubleArray,
    outOffsets: FloatArray,
    outWeights: FloatArray,
): Int {
    if (variance <= 0.25f) return 0

    val v = variance.toDouble()

    for (i in 0..13) {
        rawScratch[i] = exp(-0.5 * i.toDouble() * i.toDouble() / v)
    }

    var total = rawScratch[0]
    for (i in 1..13) total += rawScratch[i] * 2.0
    for (i in 0..13) rawScratch[i] /= total

    var tapCount = 0

    val halfCenter = rawScratch[0] * 0.5
    val w1 = rawScratch[1]
    if (halfCenter + w1 > 1e-6) {
        outOffsets[0] = (w1 / (halfCenter + w1)).toFloat()
        tapCount = 1
    }

    var i = 2
    while (i < 14 && tapCount < MAX_BLUR_TAPS) {
        val wa = rawScratch[i]
        val wb = if (i + 1 < 14) rawScratch[i + 1] else 0.0
        val combined = wa + wb
        if (combined < WEIGHT_THRESHOLD) break
        outOffsets[tapCount] = ((wa * i + wb * (i + 1)) / combined).toFloat()
        outWeights[tapCount] = combined.toFloat()
        tapCount++
        i += 2
    }

    var pairWeightSum = 0f
    for (j in 1 until tapCount) pairWeightSum += outWeights[j]
    outWeights[0] = (0.5f - pairWeightSum).coerceAtLeast(0f)

    for (j in 0 until tapCount) {
        if (outWeights[j] <= 0f || outWeights[j] >= 1f) {
            outWeights[j] = 0f
        }
    }

    return tapCount
}
