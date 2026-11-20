package com.hyper.music.ui.effects.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.material.RuntimeShader
import top.yukonga.miuix.kmp.material.asBrush
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import androidx.compose.ui.util.lerp
import com.hyper.music.ui.utils.isAppDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * 关于页的「流光」背景，移植自 miuix demo 的 `AboutPage` / `BgEffectBackground`。
 *
 * 与原版的差别只有两点：
 *  1. 去掉手机 / 平板、OS2 / OS3 的分支，固定用 OS3 的手机预设（两套 AGSL 的 main() 完全一致）；
 *  2. 不依赖 demo 的 LocalAppState：深浅色读 app 的主题设置，并按全局配色过渡
 *     的同一时长/曲线在亮、暗两套配色之间插值（不是阈值切换）。
 *
 * 注意：这一层会**先铺一层不透明 surface** 再画流光，所以它天然把壁纸挡住——
 * 关于页靠这一点做到「无论有没有壁纸，永远是流光背景」。
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    // 挂在流光这一层上的修饰符（例如 layerBackdrop，供卡片/顶栏采样）。
    bgModifier: Modifier = Modifier,
    animated: Boolean = true,
    alpha: () -> Float = { 1f },
) {
    val surface = MiuixTheme.colorScheme.surface
    val supported = remember { isRuntimeShaderSupported() }
    if (!supported) {
        // 不支持 AGSL 的机器：退化成纯色，不显示半成品。
        Box(modifier = modifier.fillMaxSize().background(surface))
        return
    }

    // 深/浅色不是二选一，而是一条 0..1 的连续量。
    // 之前是拿 surface 亮度去和 0.5 比：卡片从第 0ms 就开始渐变，阈值却要等
    // 亮度过半（≈ 一半时长）才翻，于是「卡片先变、背景后变」，翻的那一下还是突变。
    // 这里用和全局配色过渡完全相同的时长与曲线（420ms / FastOutSlowIn），
    // 起点也是同一帧（都源于同一次深浅色状态变化），两层就落在同一拍上。
    // 只读不写全局主题：配色过渡逻辑本身一点没动。
    val darkTarget = if (isAppDarkTheme()) 1f else 0f
    val darkFraction by animateFloatAsState(
        targetValue = darkTarget,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "auroraDarkFraction",
    )
    val mixState = rememberUpdatedState(darkFraction)
    val painter = remember { AuroraPainter() }
    val colorStage = remember { Animatable(0f) }

    // 颜色循环周期：亮/暗两套不一样（暗色更慢），按当前进度插值。
    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        var target = floor(colorStage.value) + 1f
        while (isActive) {
            val period = lerp(
                AuroraPresets.Light.colorInterpPeriod,
                AuroraPresets.Dark.colorInterpPeriod,
                mixState.value,
            )
            delay((period * 500).toLong())
            colorStage.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 35f))
            target += 1f
        }
    }

    Box(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .auroraDraw(
                    painter = painter,
                    mix = darkFraction,
                    surface = surface,
                    animated = animated,
                    colorStage = { colorStage.value },
                    alpha = alpha,
                ),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// 预设（数值取自 miuix demo 的 BgEffectConfig，OS3 手机 亮/暗两套）
// ---------------------------------------------------------------------------------------------

internal class AuroraPreset(
    val points: FloatArray,
    val colors1: FloatArray,
    val colors2: FloatArray,
    val colors3: FloatArray,
    val colorInterpPeriod: Float,
    val lightOffset: Float,
    val saturateOffset: Float,
    val pointOffset: Float,
)

internal object AuroraPresets {
    val Light = AuroraPreset(
        points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
        colors1 = floatArrayOf(
            1.0f, 0.9f, 0.94f, 1.0f, 1.0f, 0.84f, 0.89f, 1.0f, 0.97f, 0.73f, 0.82f, 1.0f, 0.64f, 0.65f, 0.98f, 1.0f,
        ),
        colors2 = floatArrayOf(
            0.58f, 0.74f, 1.0f, 1.0f, 1.0f, 0.9f, 0.93f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f,
        ),
        colors3 = floatArrayOf(
            0.98f, 0.86f, 0.9f, 1.0f, 0.6f, 0.73f, 0.98f, 1.0f, 0.92f, 0.93f, 1.0f, 1.0f, 0.56f, 0.69f, 1.0f, 1.0f,
        ),
        colorInterpPeriod = 5.0f,
        lightOffset = 0.1f,
        saturateOffset = 0.2f,
        pointOffset = 0.2f,
    )

    val Dark = AuroraPreset(
        points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
        colors1 = floatArrayOf(
            0.2f, 0.06f, 0.88f, 0.4f, 0.3f, 0.14f, 0.55f, 0.5f, 0.0f, 0.64f, 0.96f, 0.5f, 0.11f, 0.16f, 0.83f, 0.4f,
        ),
        colors2 = floatArrayOf(
            0.07f, 0.15f, 0.79f, 0.5f, 0.62f, 0.21f, 0.67f, 0.5f, 0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.2f, 0.78f, 0.5f,
        ),
        colors3 = floatArrayOf(
            0.58f, 0.3f, 0.74f, 0.4f, 0.27f, 0.18f, 0.6f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f, 0.12f, 0.16f, 0.7f, 0.6f,
        ),
        colorInterpPeriod = 8.0f,
        lightOffset = 0.0f,
        saturateOffset = 0.17f,
        pointOffset = 0.4f,
    )
}

// ---------------------------------------------------------------------------------------------
// 画笔：RuntimeShader + uniform 缓存
// ---------------------------------------------------------------------------------------------

private class AuroraPainter {
    val runtimeShader by lazy {
        RuntimeShader(AURORA_FRAG).also {
            it.setFloatUniform("uTranslateY", 0f)
            it.setFloatUniform("uNoiseScale", 1.5f)
            it.setFloatUniform("uPointRadiusMulti", 1f)
            it.setFloatUniform("uAlphaMulti", 1f)
        }
    }

    val brush: Brush get() = runtimeShader.asBrush()

    private val resolution = FloatArray(2)
    private val bound = FloatArray(4)
    private val colorsBuffer = FloatArray(16)
    private val pointsBuffer = FloatArray(8)

    private var animTime = Float.NaN
    private var cachedBound = FloatArray(4)

    // 三组 uniform 各自记缓存：不能共用一个 mix 值，否则先跑的那次把缓存写上，
    // 后跑的就会误判「没变」而跳过更新。
    private var offsetMix = Float.NaN
    private var colorStageCache = Float.NaN
    private var colorMix = Float.NaN
    private var pointsTime = Float.NaN
    private var pointsMix = Float.NaN

    fun updateResolution(width: Float, height: Float) {
        if (resolution[0] == width && resolution[1] == height) return
        resolution[0] = width
        resolution[1] = height
        runtimeShader.setFloatUniform("uResolution", resolution)
    }

    fun updateAnimTime(time: Float) {
        if (animTime == time) return
        animTime = time
        runtimeShader.setFloatUniform("uAnimTime", time)
    }

    /** 点位两套预设完全一致，只有三个偏移量随深浅色插值。 */
    fun updatePreset(mix: Float) {
        if (offsetMix == mix) return
        offsetMix = mix
        val light = AuroraPresets.Light
        val dark = AuroraPresets.Dark
        runtimeShader.setFloatUniform("uPoints", light.points)
        runtimeShader.setFloatUniform("uLightOffset", lerp(light.lightOffset, dark.lightOffset, mix))
        runtimeShader.setFloatUniform("uSaturateOffset", lerp(light.saturateOffset, dark.saturateOffset, mix))
    }

    /** 光点随时间游走：x += sin(t + y)，y += cos(t + x)。 */
    fun updatePoints(time: Float, mix: Float) {
        if (pointsTime == time && pointsMix == mix) return
        pointsTime = time
        pointsMix = mix
        val offset = lerp(AuroraPresets.Light.pointOffset, AuroraPresets.Dark.pointOffset, mix)
        val points = AuroraPresets.Light.points
        var i = 0
        while (i < 4) {
            val srcX = points[i * 3]
            val srcY = points[i * 3 + 1]
            val animX = srcX + sin(time + srcY) * offset
            val animY = srcY + cos(time + animX) * offset
            pointsBuffer[i * 2] = animX
            pointsBuffer[i * 2 + 1] = animY
            i++
        }
        runtimeShader.setFloatUniform("uPointsAnim", pointsBuffer)
    }

    /**
     * 先按 stage 在「同色系内的两组色」之间插值，再按 mix 在亮/暗两套之间插值。
     * 于是换主题和换色系都会是连续的，不会出现「先不动、过半再突变」。
     */
    fun updateColors(stage: Float, mix: Float) {
        if (colorStageCache == stage && colorMix == mix) return
        colorStageCache = stage
        colorMix = mix
        val base = stage.toInt()
        val fraction = stage - base
        val lightStart = colorsFor(AuroraPresets.Light, base)
        val lightEnd = colorsFor(AuroraPresets.Light, base + 1)
        val darkStart = colorsFor(AuroraPresets.Dark, base)
        val darkEnd = colorsFor(AuroraPresets.Dark, base + 1)
        for (i in 0 until 16) {
            val light = lightStart[i] + (lightEnd[i] - lightStart[i]) * fraction
            val dark = darkStart[i] + (darkEnd[i] - darkStart[i]) * fraction
            colorsBuffer[i] = light + (dark - light) * mix
        }
        runtimeShader.setFloatUniform("uColors", colorsBuffer)
    }

    private fun colorsFor(preset: AuroraPreset, index: Int): FloatArray = when (index.mod(4)) {
        1 -> preset.colors1
        3 -> preset.colors3
        else -> preset.colors2
    }

    /** 流光的绘制范围：整屏宽、80% 高（与 demo 的 isFullSize 一致）。 */
    fun updateBound(width: Float, height: Float) {
        val drawHeight = height * 0.8f
        val heightRatio = drawHeight / height
        bound[0] = 0f
        bound[1] = 1f - heightRatio
        bound[2] = 1f
        bound[3] = heightRatio
        if (cachedBound.contentEquals(bound)) return
        bound.copyInto(cachedBound)
        runtimeShader.setFloatUniform("uBound", bound)
    }
}

// ---------------------------------------------------------------------------------------------
// 绘制节点
// ---------------------------------------------------------------------------------------------

private fun Modifier.auroraDraw(
    painter: AuroraPainter,
    mix: Float,
    surface: Color,
    animated: Boolean,
    colorStage: () -> Float,
    alpha: () -> Float,
): Modifier = this then AuroraElement(painter, mix, surface, animated, colorStage, alpha)

private data class AuroraElement(
    val painter: AuroraPainter,
    val mix: Float,
    val surface: Color,
    val animated: Boolean,
    val colorStage: () -> Float,
    val alpha: () -> Float,
) : ModifierNodeElement<AuroraNode>() {
    override fun create(): AuroraNode = AuroraNode(painter, mix, surface, animated, colorStage, alpha)
    override fun update(node: AuroraNode) {
        node.bind(painter, mix, surface, animated, colorStage, alpha)
    }
}

private class AuroraNode(
    private var painter: AuroraPainter,
    private var mix: Float,
    private var surface: Color,
    private var animated: Boolean,
    private var colorStage: () -> Float,
    private var alpha: () -> Float,
) : Modifier.Node(), DrawModifierNode {

    private var job: Job? = null
    private var animTime = 0f
    private var startOffset = 0f

    override fun onAttach() {
        if (animated) start()
    }

    override fun onDetach() {
        job?.cancel()
        job = null
    }

    fun bind(
        painter: AuroraPainter,
        mix: Float,
        surface: Color,
        animated: Boolean,
        colorStage: () -> Float,
        alpha: () -> Float,
    ) {
        this.painter = painter
        this.mix = mix
        this.surface = surface
        this.colorStage = colorStage
        this.alpha = alpha
        if (this.animated != animated) {
            this.animated = animated
            if (animated) start() else {
                job?.cancel()
                job = null
            }
        }
        invalidateDraw()
    }

    private fun start() {
        job?.cancel()
        startOffset = animTime
        job = coroutineScope.launch {
            // 限到 60fps：屏幕刷新率再高也不多跑一遍全屏着色器。
            val minDelta = 1_000_000_000L / 60L
            val origin = withFrameNanos { it }
            var last = origin
            while (isActive) {
                val now = withFrameNanos { it }
                if (now - last < minDelta) continue
                last = now
                animTime = startOffset + (now - origin) / 1_000_000_000f
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        // 1) 不透明底色：这是「流光盖住壁纸」的那一行，与 demo 的 drawRect(surface) 等价。
        drawRect(surface)
        val a = alpha()
        if (a > 0f) {
            if (animated && job == null) start()
            painter.updateResolution(size.width, size.height)
            painter.updateBound(size.width, size.height)
            painter.updatePreset(mix)
            painter.updateColors(colorStage(), mix)
            painter.updateAnimTime(animTime)
            painter.updatePoints(animTime, mix)
            drawRect(brush = painter.brush, alpha = a)
        } else if (job != null) {
            // 淡到 0（滚出视野）就停，别空转。
            job?.cancel()
            job = null
        }
        drawContent()
    }
}

// ---------------------------------------------------------------------------------------------
// AGSL：与 miuix demo 的 OS3_BG_FRAG 完全一致
// ---------------------------------------------------------------------------------------------

private const val AURORA_FRAG = """
    uniform vec2 uResolution;
    uniform float uAnimTime;
    uniform vec4 uBound;
    uniform float uTranslateY;
    uniform vec3 uPoints[4];
    uniform vec2 uPointsAnim[4];
    uniform vec4 uColors[4];
    uniform float uAlphaMulti;
    uniform float uNoiseScale;
    uniform float uPointRadiusMulti;
    uniform float uSaturateOffset;
    uniform float uLightOffset;

    vec3 rgb2hsv(vec3 c) {
        vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
        vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
        vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
        float d = q.x - min(q.w, q.y);
        float e = 1.0e-10;
        return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
    }

    vec3 hsv2rgb(vec3 c) {
        vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
        vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
        return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
    }

    float hash(vec2 p) {
        vec3 p3 = fract(vec3(p.xyx) * 0.13);
        p3 += dot(p3, p3.yzx + 3.333);
        return fract((p3.x + p3.y) * p3.z);
    }

    float perlin(vec2 x) {
        vec2 i = floor(x); vec2 f = fract(x);

        float a = hash(i); float b = hash(i + vec2(1.0, 0.0));
        float c = hash(i + vec2(0.0, 1.0)); float d = hash(i + vec2(1.0, 1.0));

        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
    }

    float gradientNoise(in vec2 uv) {
        return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
    }

    vec4 main(vec2 fragCoord){
        vec2 vUv = fragCoord/uResolution;
        vUv.y = 1.0-vUv.y;
        vec2 uv = vUv;
        uv -= vec2(0., uTranslateY);
        uv.xy -= uBound.xy;
        uv.xy /= uBound.zw;

        vec4 color = vec4(0.0);
        float noiseValue = perlin(vUv * uNoiseScale + vec2(-uAnimTime, -uAnimTime));

        for (int i = 0; i < 4; i++){
            vec4 pointColor = uColors[i];
            pointColor.rgb *= pointColor.a;
            vec2 point = uPointsAnim[i];
            float rad = uPoints[i].z * uPointRadiusMulti;

            float d = distance(uv, point);
            float pct = smoothstep(rad, 0., d);
            color.rgb = mix(color.rgb, pointColor.rgb, pct);
            color.a = mix(color.a, pointColor.a, pct);
        }

        float oppositeNoise = smoothstep(0., 1., noiseValue);
        color.rgb /= color.a;
        vec3 hsv = rgb2hsv(color.rgb);
        hsv.y = mix(hsv.y, 0.0, oppositeNoise * uSaturateOffset);
        color.rgb = hsv2rgb(hsv);
        color.rgb += oppositeNoise * uLightOffset;

        color.a = clamp(color.a, 0., 1.);
        color.a *= uAlphaMulti;

        color += (10.0 / 255.0) * gradientNoise(fragCoord.xy) - (5.0 / 255.0);
        return vec4(color.rgb * color.a, color.a);
    }
"""
