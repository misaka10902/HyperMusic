


package top.yukonga.miuix.kmp.squircle

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.shader.RuntimeShader
import top.yukonga.miuix.kmp.shader.asComposeShader
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.squircle.internal.BakedSquircleSdf
import top.yukonga.miuix.kmp.squircle.internal.makeAlphaImageBitmap
import top.yukonga.miuix.kmp.squircle.internal.makeLinearShader
import kotlin.math.ceil
import kotlin.math.floor


@Composable
fun Modifier.squircleBackground(
    color: Color,
    cornerRadius: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier = squircleBackground(color, cornerRadius, cornerRadius, cornerRadius, cornerRadius, extension)


@Composable
fun Modifier.squircleBackground(
    color: Color,
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier {
    val brush = rememberSquircleBrush(color, topStart, topEnd, bottomEnd, bottomStart, extension)
        ?: return this.background(
            color = color,
            shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart),
        )
    return this.background(brush = brush, shape = RectangleShape)
}


@Composable
fun Modifier.squircleClip(
    cornerRadius: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier = squircleClip(cornerRadius, cornerRadius, cornerRadius, cornerRadius, extension)


@Composable
fun Modifier.squircleClip(
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier {
    val brush = rememberSquircleBrush(Color.White, topStart, topEnd, bottomEnd, bottomStart, extension)
        ?: return this.clip(RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart))
    return this.squircleShaderMask(brush, fillColor = null)
}


@Composable
fun Modifier.squircleSurface(
    color: Color,
    cornerRadius: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier = squircleSurface(color, cornerRadius, cornerRadius, cornerRadius, cornerRadius, extension)


@Composable
fun Modifier.squircleSurface(
    color: Color,
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier {
    val brush = rememberSquircleBrush(Color.White, topStart, topEnd, bottomEnd, bottomStart, extension)
        ?: return this
            .clip(RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart))
            .background(color)
    return this.squircleShaderMask(brush, fillColor = color)
}


@Composable
fun Modifier.absoluteSquircleBackground(
    color: Color,
    topLeft: Dp,
    topRight: Dp,
    bottomRight: Dp,
    bottomLeft: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier {
    val brush = rememberSquircleBrush(color, topLeft, topRight, bottomRight, bottomLeft, extension)
        ?: return this.background(
            color = color,
            shape = AbsoluteRoundedCornerShape(topLeft, topRight, bottomRight, bottomLeft),
        )
    return this.background(brush = brush, shape = RectangleShape)
}


@Composable
fun Modifier.absoluteSquircleClip(
    topLeft: Dp,
    topRight: Dp,
    bottomRight: Dp,
    bottomLeft: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier {
    val brush = rememberSquircleBrush(Color.White, topLeft, topRight, bottomRight, bottomLeft, extension)
        ?: return this.clip(AbsoluteRoundedCornerShape(topLeft, topRight, bottomRight, bottomLeft))
    return this.squircleShaderMask(brush, fillColor = null)
}


@Composable
fun Modifier.absoluteSquircleSurface(
    color: Color,
    topLeft: Dp,
    topRight: Dp,
    bottomRight: Dp,
    bottomLeft: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier {
    val brush = rememberSquircleBrush(Color.White, topLeft, topRight, bottomRight, bottomLeft, extension)
        ?: return this
            .clip(AbsoluteRoundedCornerShape(topLeft, topRight, bottomRight, bottomLeft))
            .background(color)
    return this.squircleShaderMask(brush, fillColor = color)
}


private fun Modifier.squircleShaderMask(
    brush: SquircleShaderBrush,
    fillColor: Color?,
): Modifier = this
    .graphicsLayer {
        compositingStrategy =
            if (maxOf(size.width, size.height) <= MAX_OFFSCREEN_PX) {
                CompositingStrategy.Offscreen
            } else {
                CompositingStrategy.Auto
            }
    }
    .drawWithCache {
        val layerPaint = Paint()
        val contentLayer = obtainGraphicsLayer()
        onDrawWithContent {
            if (maxOf(size.width, size.height) <= MAX_OFFSCREEN_PX) {
                drawSquircleFast(brush, fillColor)
            } else {
                contentLayer.record { this@onDrawWithContent.drawContent() }
                drawSquircleBanded(brush, fillColor, layerPaint, contentLayer)
            }
        }
    }


private fun ContentDrawScope.drawSquircleFast(
    brush: SquircleShaderBrush,
    fillColor: Color?,
) {
    if (fillColor != null) drawRect(fillColor)
    drawContent()
    maskSquircleCorners(brush, top = true, bottom = true)
}



private fun ContentDrawScope.drawSquircleBanded(
    brush: SquircleShaderBrush,
    fillColor: Color?,
    layerPaint: Paint,
    contentLayer: GraphicsLayer,
) {
    val width = size.width
    val height = size.height
    val canvas = drawContext.canvas
    val topBand = brush.topBandHeightPx(size)
    val bottomBand = brush.bottomBandHeightPx(size)


    fun maskedBand(top: Float, bottom: Float, maskTop: Boolean, maskBottom: Boolean, clip: Boolean) {
        canvas.saveLayer(Rect(0f, top, width, bottom), layerPaint)
        if (clip) canvas.clipRect(0f, top, width, bottom)
        if (fillColor != null) drawRect(fillColor)
        drawLayer(contentLayer)
        maskSquircleCorners(brush, top = maskTop, bottom = maskBottom)
        canvas.restore()
    }


    if (height <= topBand + bottomBand) {
        maskedBand(0f, height, maskTop = true, maskBottom = true, clip = false)
        return
    }


    val midTop = ceil(topBand)
    val midBottom = floor(height - bottomBand)
    if (topBand > 0f) maskedBand(0f, midTop, maskTop = true, maskBottom = false, clip = true)
    clipRect(top = midTop, bottom = midBottom) {
        if (fillColor != null) drawRect(fillColor)
        drawLayer(contentLayer)
    }
    if (bottomBand > 0f) maskedBand(midBottom, height, maskTop = false, maskBottom = true, clip = true)
}



private fun ContentDrawScope.maskSquircleCorners(
    brush: SquircleShaderBrush,
    top: Boolean,
    bottom: Boolean,
) {
    val width = size.width
    val height = size.height
    val canvas = drawContext.canvas
    fun corner(left: Float, topPx: Float, side: Float) {
        if (side <= 0f) return
        canvas.save()
        canvas.clipRect(left, topPx, left + side, topPx + side)
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
        canvas.restore()
    }
    if (top) {
        corner(0f, 0f, brush.effectiveCornerPx(0, size))
        val tr = brush.effectiveCornerPx(1, size)
        corner(width - tr, 0f, tr)
    }
    if (bottom) {
        val br = brush.effectiveCornerPx(2, size)
        corner(width - br, height - br, br)
        val bl = brush.effectiveCornerPx(3, size)
        corner(0f, height - bl, bl)
    }
}

@Composable
private fun rememberSquircleBrush(
    color: Color,
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    extension: Float,
): SquircleShaderBrush? {
    if (!LocalSquircleEnabled.current || !isRuntimeShaderSupported()) return null
    val density = LocalDensity.current
    val extClamped = extension.coerceIn(SquircleDefaults.ExtensionMin, SquircleDefaults.ExtensionMax)
    val tiles = remember(topStart, topEnd, bottomEnd, bottomStart, extClamped, density) {
        with(density) {
            floatArrayOf(
                topStart.toPx().coerceAtLeast(0f) * extClamped,
                topEnd.toPx().coerceAtLeast(0f) * extClamped,
                bottomEnd.toPx().coerceAtLeast(0f) * extClamped,
                bottomStart.toPx().coerceAtLeast(0f) * extClamped,
            )
        }
    }
    return remember(color, tiles) {
        SquircleShaderBrush(color, tiles)
    }
}

private class SquircleShaderBrush(
    private val color: Color,
    private val cornerTilesPx: FloatArray,
) : ShaderBrush() {
    private val runtimeShader = RuntimeShader(SQUIRCLE_SHADER)
    private val effectiveSizes = FloatArray(4)
    private val halfRanges = FloatArray(4)
    private val weights = FloatArray(4)

    override fun createShader(size: Size): Shader {
        val halfMin = minOf(size.width, size.height) * 0.5f
        val threshold = halfMin * BLEND_THRESHOLD_RATIO
        val range = halfMin - threshold
        for (i in 0..3) {
            val tile = cornerTilesPx[i]
            val effective = effectiveCornerPx(i, size)
            effectiveSizes[i] = effective
            halfRanges[i] = BakedSquircleSdf.HALF_RANGE * effective
            weights[i] = if (range <= 0f) 1f else ((tile - threshold) / range).coerceIn(0f, 1f)
        }
        runtimeShader.setColorUniform("color", color)
        runtimeShader.setFloatUniform("size", size.width, size.height)
        runtimeShader.setFloatUniform("cornerSizes", effectiveSizes)
        runtimeShader.setFloatUniform("halfRangesPx", halfRanges)
        runtimeShader.setFloatUniform("blendWeights", weights)
        runtimeShader.setFloatUniform("bitmapSize", BakedSquircleSdf.SIZE.toFloat())
        runtimeShader.setInputShader("cornerSdf", bakedSdfShader)
        return runtimeShader.asComposeShader()
    }


    fun topBandHeightPx(size: Size): Float = maxOf(effectiveCornerPx(0, size), effectiveCornerPx(1, size))

    fun bottomBandHeightPx(size: Size): Float = maxOf(effectiveCornerPx(2, size), effectiveCornerPx(3, size))


    fun effectiveCornerPx(index: Int, size: Size): Float {
        val halfMin = minOf(size.width, size.height) * 0.5f
        return cornerTilesPx[index].coerceIn(0f, halfMin)
    }
}



private const val MAX_OFFSCREEN_PX = 2048f



private const val BLEND_THRESHOLD_RATIO = 0.7853982f


private val bakedSdfShader: Shader by lazy {
    makeLinearShader(makeAlphaImageBitmap(BakedSquircleSdf.SIZE, BakedSquircleSdf.bytes))
}

private const val SQUIRCLE_SHADER = """
uniform shader cornerSdf;
uniform float2 size;
uniform float4 cornerSizes;
uniform float4 halfRangesPx;
uniform float4 blendWeights;
uniform float bitmapSize;
layout(color) uniform half4 color;

half4 main(float2 coord) {
    float dxL = coord.x;
    float dxR = size.x - coord.x;
    float dyT = coord.y;
    float dyB = size.y - coord.y;
    bool left = dxL < dxR;
    bool top = dyT < dyB;
    float dx = left ? dxL : dxR;
    float dy = top ? dyT : dyB;
    // vec4 swizzle: AGSL forbids dynamic indexing. Channel order: x=topStart, y=topEnd, z=bottomEnd, w=bottomStart.
    float cornerSize = top
        ? (left ? cornerSizes.x : cornerSizes.y)
        : (left ? cornerSizes.w : cornerSizes.z);
    float halfRangePx = top
        ? (left ? halfRangesPx.x : halfRangesPx.y)
        : (left ? halfRangesPx.w : halfRangesPx.z);
    float blendWeight = top
        ? (left ? blendWeights.x : blendWeights.y)
        : (left ? blendWeights.w : blendWeights.z);
    if (dx >= cornerSize || dy >= cornerSize) {
        return color;
    }
    float2 uv = float2(dx, dy) / cornerSize;
    // LINEAR-sampled child: one tap == old 4-tap. Coord is uv*bitmapSize (NO -0.5; sampler adds half-texel).
    half sdfSample = cornerSdf.eval(uv * bitmapSize).a;
    float squircleSdf = (1.0 - 2.0 * float(sdfSample)) * halfRangePx;
    float qx = cornerSize - dx;
    float qy = cornerSize - dy;
    float circleSdf = sqrt(qx * qx + qy * qy) - cornerSize;
    float dist = mix(squircleSdf, circleSdf, blendWeight);
    return color * half(smoothstep(0.5, -0.5, dist));
}
"""
