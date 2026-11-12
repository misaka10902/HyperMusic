package top.yukonga.miuix.kmp.blur

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

@Stable
interface Backdrop {

    val isCoordinatesDependent: Boolean

    val offsetResidualX: Float get() = 0f

    val offsetResidualY: Float get() = 0f

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
        downscaleFactor: Int = 1,
    )
}
