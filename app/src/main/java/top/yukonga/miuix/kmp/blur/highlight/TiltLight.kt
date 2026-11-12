


package top.yukonga.miuix.kmp.blur.highlight

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt


@Composable
fun rememberTiltLight(
    basePosition: LightPosition,
    color: Color = Color.White,
    @FloatRange(from = 0.0) intensity: Float = 1f,
    @FloatRange(from = 0.0) sensitivity: Float = 0.1f,
): LightSource {
    val tilt by rememberDeviceTilt()
    return remember(tilt, basePosition, color, intensity, sensitivity) {
        LightSource(
            position = LightPosition(
                x = basePosition.x + sensitivity * tilt.roll,
                y = basePosition.y - sensitivity * tilt.pitch,
                z = basePosition.z,
            ),
            color = color,
            intensity = intensity,
        )
    }
}
