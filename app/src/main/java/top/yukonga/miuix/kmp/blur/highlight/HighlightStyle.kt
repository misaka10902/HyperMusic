


package top.yukonga.miuix.kmp.blur.highlight

import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import top.yukonga.miuix.kmp.blur.RuntimeShader
import top.yukonga.miuix.kmp.blur.RuntimeShaderCache
import top.yukonga.miuix.kmp.blur.asComposeShader
import top.yukonga.miuix.kmp.blur.internal.BLOOM_STROKE_SHADER_DUAL
import top.yukonga.miuix.kmp.blur.internal.BLOOM_STROKE_SHADER_SINGLE
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import kotlin.math.floor
import kotlin.math.sqrt


private const val LIGHT_REF_X = 0.5f
private const val LIGHT_REF_Y = 0.7f


@Immutable
interface HighlightStyle {


    val color: Color


    val blendMode: BlendMode


    fun DrawScope.createShader(
        shape: Shape,
        strokeWidthPx: Float,
        highlightAlpha: Float,
        runtimeShaderCache: RuntimeShaderCache,
    ): Shader?

    companion object {


        @Stable
        val Default: BloomStroke = BloomStroke.GlassStrokeMiddleLight
    }
}


@Immutable
data class LightPosition(
    val x: Float,
    val y: Float,
    val z: Float,
)


@Immutable
data class LightSource(
    val position: LightPosition,
    val color: Color = Color.White,
    @param:FloatRange(from = 0.0) val intensity: Float = 1f,
)


@Immutable
data class BloomStroke(
    override val color: Color = Color.White.copy(alpha = 0.05f),
    override val blendMode: BlendMode = BlendMode.Plus,
    val innerBlurRadius: Dp = 2.8.dp,
    val primaryLight: LightSource = LightSource(
        position = LightPosition(0.5f, 0.5f, -0.5f),
        intensity = 0.4f,
    ),
    val secondaryLight: LightSource = LightSource(
        position = LightPosition(0.5f, 0.8f, -0.5f),
        intensity = 0.25f,
    ),
    val dualPeak: Boolean = false,
) : HighlightStyle {

    override fun DrawScope.createShader(
        shape: Shape,
        strokeWidthPx: Float,
        highlightAlpha: Float,
        runtimeShaderCache: RuntimeShaderCache,
    ): Shader? {
        if (!isRuntimeShaderSupported()) return null
        val sizePx = size
        val shaderKey = if (dualPeak) "BloomStrokeDual" else "BloomStrokeSingle"
        val shaderSource = if (dualPeak) BLOOM_STROKE_SHADER_DUAL else BLOOM_STROKE_SHADER_SINGLE
        val shader = runtimeShaderCache.obtainRuntimeShader(shaderKey, shaderSource)
        val halfW = sizePx.width * 0.5f
        val halfH = sizePx.height * 0.5f
        shader.setFloatUniform("halfView", halfW, halfH)
        shader.setFloatUniform("halfViewFloor", floor(halfW), floor(halfH))
        setCornerRadiiUniform(shader, shape)
        shader.setFloatUniform("strokeWidth", strokeWidthPx)
        val innerR = innerBlurRadius.toPx()
        shader.setFloatUniform("innerBlurRadius", innerR)
        shader.setFloatUniform("innerBlurRadiusSq", innerR * innerR)
        shader.setFloatUniform("highlightAlpha", highlightAlpha)
        shader.setColorUniform("strokeColor", color.copy(alpha = 1f))
        shader.setFloatUniform("strokeAlphaMul", color.alpha)
        applyLightUniforms(shader, "1", primaryLight, includeAxis = !dualPeak)
        applyLightUniforms(shader, "2", secondaryLight, includeAxis = !dualPeak)
        return shader.asComposeShader()
    }

    companion object {


        @Stable
        val GlassStrokeBigLight: BloomStroke = BloomStroke(
            color = Color.White.copy(alpha = 0.05f),
            innerBlurRadius = 3.5.dp,
            primaryLight = LightSource(
                position = LightPosition(0.5f, 0.5f, -0.5f),
                intensity = 0.3f,
            ),
            secondaryLight = LightSource(
                position = LightPosition(0.5f, 0.6f, -0.5f),
                intensity = 0.2f,
            ),
        )


        @Stable
        val GlassStrokeMiddleLight: BloomStroke = BloomStroke(
            color = Color.White.copy(alpha = 0.05f),
            innerBlurRadius = 2.8.dp,
            primaryLight = LightSource(
                position = LightPosition(0.5f, 0.5f, -0.5f),
                intensity = 0.4f,
            ),
            secondaryLight = LightSource(
                position = LightPosition(0.5f, 0.8f, -0.5f),
                intensity = 0.25f,
            ),
        )


        @Stable
        val GlassStrokeSmallLight: BloomStroke = BloomStroke(
            color = Color.White.copy(alpha = 0.05f),
            innerBlurRadius = 2.6.dp,
            primaryLight = LightSource(
                position = LightPosition(0.5f, 0.5f, -0.5f),
                intensity = 0.6f,
            ),
            secondaryLight = LightSource(
                position = LightPosition(0.5f, 0.95f, -0.5f),
                intensity = 0.35f,
            ),
        )


        @Stable
        val GlassStrokeBigDark: BloomStroke = BloomStroke(
            color = Color.White.copy(alpha = 0.05f),
            innerBlurRadius = 1.7.dp,
            primaryLight = LightSource(
                position = LightPosition(0.5f, 0.5f, -0.5f),
                intensity = 0.4f,
            ),
            secondaryLight = LightSource(
                position = LightPosition(0.5f, 0.6f, -0.5f),
                intensity = 0.25f,
            ),
        )


        @Stable
        val GlassStrokeMiddleDark: BloomStroke = BloomStroke(
            color = Color.White.copy(alpha = 0.06f),
            innerBlurRadius = 2.0.dp,
            primaryLight = LightSource(
                position = LightPosition(0.5f, 0.5f, -0.5f),
                intensity = 0.5f,
            ),
            secondaryLight = LightSource(
                position = LightPosition(0.5f, 0.8f, -0.5f),
                intensity = 0.25f,
            ),
        )


        @Stable
        val GlassStrokeSmallDark: BloomStroke = BloomStroke(
            color = Color.White.copy(alpha = 0.08f),
            innerBlurRadius = 2.3.dp,
            primaryLight = LightSource(
                position = LightPosition(0.5f, 0.5f, -0.5f),
                intensity = 0.6f,
            ),
            secondaryLight = LightSource(
                position = LightPosition(0.5f, 0.95f, -0.36f),
                intensity = 0.25f,
            ),
        )
    }
}

internal fun applyLightUniforms(
    shader: RuntimeShader,
    suffix: String,
    light: LightSource,
    includeAxis: Boolean = true,
) {
    val dx = light.position.x - LIGHT_REF_X
    val dy = light.position.y - LIGHT_REF_Y
    val dz = light.position.z
    val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-6f)
    val nx = dx / len
    val ny = dy / len
    shader.setFloatUniform("lightDir$suffix", nx, ny, dz / len)
    shader.setColorUniform("lightColor$suffix", light.color.copy(alpha = 1f))
    shader.setFloatUniform("lightIntensity$suffix", light.color.alpha * light.intensity)
    if (includeAxis) {
        val xyLen = sqrt(nx * nx + ny * ny)
        if (xyLen > 1e-3f) {
            shader.setFloatUniform("axis$suffix", nx / xyLen, ny / xyLen)
        } else {

            val fallbackY = if (suffix == "1") -1f else 1f
            shader.setFloatUniform("axis$suffix", 0f, fallbackY)
        }
    }
}


internal fun DrawScope.setCornerRadiiUniform(shader: RuntimeShader, shape: Shape) {
    val sizePx = size
    val maxRadius = sizePx.minDimension / 2f
    val cornerShape = shape as? CornerBasedShape
    if (cornerShape == null) {
        shader.setFloatUniform("cornerRadii", maxRadius, maxRadius, maxRadius, maxRadius)
        return
    }
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val topLeft =
        if (isLtr) cornerShape.topStart.toPx(sizePx, this) else cornerShape.topEnd.toPx(sizePx, this)
    val topRight =
        if (isLtr) cornerShape.topEnd.toPx(sizePx, this) else cornerShape.topStart.toPx(sizePx, this)
    val bottomRight =
        if (isLtr) cornerShape.bottomEnd.toPx(sizePx, this) else cornerShape.bottomStart.toPx(sizePx, this)
    val bottomLeft =
        if (isLtr) cornerShape.bottomStart.toPx(sizePx, this) else cornerShape.bottomEnd.toPx(sizePx, this)
    shader.setFloatUniform(
        "cornerRadii",
        topLeft.fastCoerceAtMost(maxRadius),
        topRight.fastCoerceAtMost(maxRadius),
        bottomLeft.fastCoerceAtMost(maxRadius),
        bottomRight.fastCoerceAtMost(maxRadius),
    )
}
