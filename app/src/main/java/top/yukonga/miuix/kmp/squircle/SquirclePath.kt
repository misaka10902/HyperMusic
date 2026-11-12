


package top.yukonga.miuix.kmp.squircle

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import kotlin.math.max
import kotlin.math.min


object SquircleDefaults {


    val Extension = 1.1f


    val ExtensionMin = 1f


    val ExtensionMax = 2f
}


internal const val SQUIRCLE_CONTROL = 0.643f


fun Path.addSquircleRect(
    width: Float,
    height: Float,
    cornerRadius: Float,
    extension: Float = SquircleDefaults.Extension,
    squircleEnabled: Boolean = true,
) {
    if (width <= 0f || height <= 0f) return
    if (!squircleEnabled) {
        val radius = max(0f, cornerRadius).coerceAtMost(min(width, height) * 0.5f)
        if (radius <= 0f) {
            addRect(Rect(0f, 0f, width, height))
        } else {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = width,
                    bottom = height,
                    cornerRadius = CornerRadius(radius, radius),
                ),
            )
        }
        return
    }
    val extClamped = extension.coerceIn(SquircleDefaults.ExtensionMin, SquircleDefaults.ExtensionMax)
    val halfMin = min(width, height) * 0.5f
    val tile = max(0f, cornerRadius * extClamped).coerceAtMost(halfMin)
    if (tile <= 0f) {
        addRect(Rect(0f, 0f, width, height))
        return
    }
    val handle = tile * (1f - SQUIRCLE_CONTROL)
    moveTo(tile, 0f)
    lineTo(width - tile, 0f)
    cubicTo(width - handle, 0f, width, handle, width, tile)
    lineTo(width, height - tile)
    cubicTo(width, height - handle, width - handle, height, width - tile, height)
    lineTo(tile, height)
    cubicTo(handle, height, 0f, height - handle, 0f, height - tile)
    lineTo(0f, tile)
    cubicTo(0f, handle, handle, 0f, tile, 0f)
    close()
}
