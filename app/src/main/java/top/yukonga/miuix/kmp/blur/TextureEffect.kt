package top.yukonga.miuix.kmp.blur

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode

fun Modifier.textureBlur(
    backdrop: Backdrop,
    shape: Shape,
    blurRadius: Float = BlurDefaults.BlurRadius,
    noiseCoefficient: Float = BlurDefaults.NoiseCoefficient,
    colors: BlurColors = BlurColors(),
    highlight: Highlight? = null,
    contentBlendMode: ComposeBlendMode = ComposeBlendMode.SrcOver,
    enabled: Boolean = true,
): Modifier = textureEffect(
    backdrop = backdrop,
    shape = shape,
    blurRadius = blurRadius,
    noiseCoefficient = noiseCoefficient,
    colors = colors,
    highlight = highlight,
    contentBlendMode = contentBlendMode,
    enabled = enabled,
)

fun Modifier.textureBlur(
    backdrop: Backdrop,
    shape: Shape,
    blurRadiusX: Float,
    blurRadiusY: Float,
    noiseCoefficient: Float = BlurDefaults.NoiseCoefficient,
    colors: BlurColors = BlurColors(),
    highlight: Highlight? = null,
    contentBlendMode: ComposeBlendMode = ComposeBlendMode.SrcOver,
    enabled: Boolean = true,
): Modifier = textureEffect(
    backdrop = backdrop,
    shape = shape,
    blurRadiusX = blurRadiusX,
    blurRadiusY = blurRadiusY,
    noiseCoefficient = noiseCoefficient,
    colors = colors,
    highlight = highlight,
    contentBlendMode = contentBlendMode,
    enabled = enabled,
)

fun Modifier.textureEffect(
    backdrop: Backdrop,
    shape: Shape,
    blurRadius: Float = BlurDefaults.BlurRadius,
    noiseCoefficient: Float = BlurDefaults.NoiseCoefficient,
    colors: BlurColors = BlurColors(),
    highlight: Highlight? = null,
    contentBlendMode: ComposeBlendMode = ComposeBlendMode.SrcOver,
    enabled: Boolean = true,
): Modifier = textureEffect(
    backdrop = backdrop,
    shape = shape,
    blurRadiusX = blurRadius,
    blurRadiusY = blurRadius,
    noiseCoefficient = noiseCoefficient,
    colors = colors,
    highlight = highlight,
    contentBlendMode = contentBlendMode,
    enabled = enabled,
)

fun Modifier.textureEffect(
    backdrop: Backdrop,
    shape: Shape,
    blurRadiusX: Float,
    blurRadiusY: Float,
    noiseCoefficient: Float = BlurDefaults.NoiseCoefficient,
    colors: BlurColors = BlurColors(),
    highlight: Highlight? = null,
    contentBlendMode: ComposeBlendMode = ComposeBlendMode.SrcOver,
    enabled: Boolean = true,
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        textureBlurEffect(
            blurRadiusX = blurRadiusX,
            blurRadiusY = blurRadiusY,
            noiseCoefficient = noiseCoefficient,
            colors = colors,
        )
    },
    highlight = if (highlight != null) {
        { highlight }
    } else {
        null
    },
    contentBlendMode = contentBlendMode,
    enabled = enabled,
)

fun Modifier.progressiveTextureBlur(
    backdrop: Backdrop,
    shape: Shape,
    blurRadius: Float = BlurDefaults.BlurRadius,
    gradient: ProgressiveBlur = ProgressiveBlur.Top,
    noiseCoefficient: Float = BlurDefaults.ProgressiveNoiseCoefficient,
    colors: BlurColors = BlurColors(),
    highlight: Highlight? = null,
    contentBlendMode: ComposeBlendMode = ComposeBlendMode.SrcOver,
    enabled: Boolean = true,
): Modifier = progressiveTextureBlur(
    backdrop = backdrop,
    shape = shape,
    blurRadiusX = blurRadius,
    blurRadiusY = blurRadius,
    gradient = gradient,
    noiseCoefficient = noiseCoefficient,
    colors = colors,
    highlight = highlight,
    contentBlendMode = contentBlendMode,
    enabled = enabled,
)

fun Modifier.progressiveTextureBlur(
    backdrop: Backdrop,
    shape: Shape,
    blurRadiusX: Float,
    blurRadiusY: Float,
    gradient: ProgressiveBlur = ProgressiveBlur.Top,
    noiseCoefficient: Float = BlurDefaults.ProgressiveNoiseCoefficient,
    colors: BlurColors = BlurColors(),
    highlight: Highlight? = null,
    contentBlendMode: ComposeBlendMode = ComposeBlendMode.SrcOver,
    enabled: Boolean = true,
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        progressiveTextureBlurEffect(
            blurRadiusX = blurRadiusX,
            blurRadiusY = blurRadiusY,
            gradient = gradient,
            noiseCoefficient = noiseCoefficient,
            colors = colors,
        )
    },
    highlight = if (highlight != null) {
        { highlight }
    } else {
        null
    },
    contentBlendMode = contentBlendMode,
    progressiveGradient = gradient,
    enabled = enabled,
)
