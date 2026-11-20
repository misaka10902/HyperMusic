package com.hyper.music.ui.theme

import androidx.compose.ui.graphics.lerp
import top.yukonga.miuix.kmp.theme.Colors

/**
 * 在两套 miuix Colors 之间逐字段插值，返回新实例。
 * 0.9.5 起 Colors 字段 setter 为 internal，无法就地写入；但所有 getter 与 data class
 * 的 copy() 均为 public，因此用 copy 逐字段 lerp 实现主题/壁纸切换的平滑过渡。
 */
internal fun lerpColors(from: Colors, to: Colors, fraction: Float): Colors {
    val f = fraction.coerceIn(0f, 1f)
    fun c(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color) = lerp(a, b, f)
    return from.copy(
        primary = c(from.primary, to.primary),
        onPrimary = c(from.onPrimary, to.onPrimary),
        primaryVariant = c(from.primaryVariant, to.primaryVariant),
        onPrimaryVariant = c(from.onPrimaryVariant, to.onPrimaryVariant),
        error = c(from.error, to.error),
        onError = c(from.onError, to.onError),
        errorContainer = c(from.errorContainer, to.errorContainer),
        onErrorContainer = c(from.onErrorContainer, to.onErrorContainer),
        disabledPrimary = c(from.disabledPrimary, to.disabledPrimary),
        disabledOnPrimary = c(from.disabledOnPrimary, to.disabledOnPrimary),
        disabledPrimaryButton = c(from.disabledPrimaryButton, to.disabledPrimaryButton),
        disabledOnPrimaryButton = c(from.disabledOnPrimaryButton, to.disabledOnPrimaryButton),
        disabledPrimarySlider = c(from.disabledPrimarySlider, to.disabledPrimarySlider),
        primaryContainer = c(from.primaryContainer, to.primaryContainer),
        onPrimaryContainer = c(from.onPrimaryContainer, to.onPrimaryContainer),
        secondary = c(from.secondary, to.secondary),
        onSecondary = c(from.onSecondary, to.onSecondary),
        secondaryVariant = c(from.secondaryVariant, to.secondaryVariant),
        onSecondaryVariant = c(from.onSecondaryVariant, to.onSecondaryVariant),
        disabledSecondary = c(from.disabledSecondary, to.disabledSecondary),
        disabledOnSecondary = c(from.disabledOnSecondary, to.disabledOnSecondary),
        disabledSecondaryVariant = c(from.disabledSecondaryVariant, to.disabledSecondaryVariant),
        disabledOnSecondaryVariant = c(from.disabledOnSecondaryVariant, to.disabledOnSecondaryVariant),
        secondaryContainer = c(from.secondaryContainer, to.secondaryContainer),
        onSecondaryContainer = c(from.onSecondaryContainer, to.onSecondaryContainer),
        secondaryContainerVariant = c(from.secondaryContainerVariant, to.secondaryContainerVariant),
        onSecondaryContainerVariant = c(from.onSecondaryContainerVariant, to.onSecondaryContainerVariant),
        tertiaryContainer = c(from.tertiaryContainer, to.tertiaryContainer),
        onTertiaryContainer = c(from.onTertiaryContainer, to.onTertiaryContainer),
        tertiaryContainerVariant = c(from.tertiaryContainerVariant, to.tertiaryContainerVariant),
        background = c(from.background, to.background),
        onBackground = c(from.onBackground, to.onBackground),
        onBackgroundVariant = c(from.onBackgroundVariant, to.onBackgroundVariant),
        surface = c(from.surface, to.surface),
        onSurface = c(from.onSurface, to.onSurface),
        surfaceVariant = c(from.surfaceVariant, to.surfaceVariant),
        onSurfaceSecondary = c(from.onSurfaceSecondary, to.onSurfaceSecondary),
        onSurfaceVariantSummary = c(from.onSurfaceVariantSummary, to.onSurfaceVariantSummary),
        onSurfaceVariantActions = c(from.onSurfaceVariantActions, to.onSurfaceVariantActions),
        disabledOnSurface = c(from.disabledOnSurface, to.disabledOnSurface),
        surfaceContainer = c(from.surfaceContainer, to.surfaceContainer),
        onSurfaceContainer = c(from.onSurfaceContainer, to.onSurfaceContainer),
        onSurfaceContainerVariant = c(from.onSurfaceContainerVariant, to.onSurfaceContainerVariant),
        surfaceContainerHigh = c(from.surfaceContainerHigh, to.surfaceContainerHigh),
        onSurfaceContainerHigh = c(from.onSurfaceContainerHigh, to.onSurfaceContainerHigh),
        surfaceContainerHighest = c(from.surfaceContainerHighest, to.surfaceContainerHighest),
        onSurfaceContainerHighest = c(from.onSurfaceContainerHighest, to.onSurfaceContainerHighest),
        outline = c(from.outline, to.outline),
        dividerLine = c(from.dividerLine, to.dividerLine),
        windowDimming = c(from.windowDimming, to.windowDimming),
        sliderKeyPoint = c(from.sliderKeyPoint, to.sliderKeyPoint),
        sliderKeyPointForeground = c(from.sliderKeyPointForeground, to.sliderKeyPointForeground),
        sliderBackground = c(from.sliderBackground, to.sliderBackground),
    )
}
