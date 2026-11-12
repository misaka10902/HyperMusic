


package top.yukonga.miuix.kmp.blur.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State


@Immutable
data class DeviceTilt(
    val pitch: Float,
    val roll: Float,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
) {

    companion object {

        @Stable
        val Zero: DeviceTilt = DeviceTilt(0f, 0f, 0f, 0f)
    }
}


