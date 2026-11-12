


package top.yukonga.miuix.kmp.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp


@Stable
class DialogSurfaceScope internal constructor(
    val cornerRadius: Dp,
    val scrimColor: State<Color>,
    val scale: State<Float>,
)
