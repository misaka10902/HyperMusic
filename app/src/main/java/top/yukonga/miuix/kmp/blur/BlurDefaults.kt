


package top.yukonga.miuix.kmp.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlin.jvm.JvmInline


@Immutable
data class BlurColors(
    val blendColors: List<BlendColorEntry> = emptyList(),
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
)


@Immutable
data class BlendColorEntry(
    val color: Color,
    val mode: BlurBlendMode = BlurBlendMode.SrcOver,
)


@JvmInline
value class BlurBlendMode(val value: Int) {

    companion object {


        val Clear = BlurBlendMode(0)
        val Src = BlurBlendMode(1)
        val Dst = BlurBlendMode(2)
        val SrcOver = BlurBlendMode(3)
        val DstOver = BlurBlendMode(4)
        val SrcIn = BlurBlendMode(5)
        val DstIn = BlurBlendMode(6)
        val SrcOut = BlurBlendMode(7)
        val DstOut = BlurBlendMode(8)
        val SrcAtop = BlurBlendMode(9)
        val DstAtop = BlurBlendMode(10)
        val Xor = BlurBlendMode(11)
        val Plus = BlurBlendMode(12)
        val Modulate = BlurBlendMode(13)
        val Screen = BlurBlendMode(14)
        val Overlay = BlurBlendMode(15)
        val Darken = BlurBlendMode(16)
        val Lighten = BlurBlendMode(17)
        val ColorDodge = BlurBlendMode(18)
        val ColorBurn = BlurBlendMode(19)
        val HardLight = BlurBlendMode(20)
        val SoftLight = BlurBlendMode(21)
        val Difference = BlurBlendMode(22)
        val Exclusion = BlurBlendMode(23)
        val Multiply = BlurBlendMode(24)
        val Hue = BlurBlendMode(25)
        val Saturation = BlurBlendMode(26)
        val Color = BlurBlendMode(27)
        val Luminosity = BlurBlendMode(28)






        val LinearLight = BlurBlendMode(100)


        val LinearLightWithGreyscale = BlurBlendMode(101)


        val MiDifference = BlurBlendMode(102)


        val LabLightenWithGreyscale = BlurBlendMode(103)


        val LabDarkenWithGreyscale = BlurBlendMode(105)


        val Lab = BlurBlendMode(106)


        val LinearLightLab = BlurBlendMode(107)


        val MiColorDodge = BlurBlendMode(118)


        val MiColorBurn = BlurBlendMode(119)


        val PlusDarker = BlurBlendMode(120)


        val PlusLighter = BlurBlendMode(121)


        val AlphaBlend = BlurBlendMode(200)


        val MiSaturation = BlurBlendMode(201)


        val MiBrightness = BlurBlendMode(202)


        val MiLuminance = BlurBlendMode(203)


    }
}


object BlurDefaults {


    val BlurRadius: Float = 20f


    val NoiseCoefficient: Float = 0.0045f


    val ProgressiveNoiseCoefficient: Float = 0f


    val MaxBlurRadius: Float = 150f


    @Composable
    fun blurColors(
        blendColors: List<BlendColorEntry> = emptyList(),
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
    ): BlurColors = remember(blendColors, brightness, contrast, saturation) {
        BlurColors(
            blendColors = blendColors,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
        )
    }
}
