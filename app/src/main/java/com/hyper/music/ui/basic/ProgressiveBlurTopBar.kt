package com.hyper.music.ui.basic

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyper.music.music.WallpaperManager
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProgressiveBlurTopBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    height: Dp = Dp.Unspecified,
    scrollBehavior: ScrollBehavior? = null,
    blurAlpha: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val wallpaperActive = WallpaperManager.state.collectAsState().value.hasImage
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topInset = if (statusBarHeight > 0.dp) statusBarHeight else 64.dp
    val totalHeight = if (height != Dp.Unspecified) {
        height
    } else {
        if (statusBarHeight > 0.dp) 80.dp + statusBarHeight else 120.dp + (topInset - 64.dp)
    }

    val isContentScrolled by remember(scrollBehavior) {
        derivedStateOf {
            val st = scrollBehavior?.state
            st != null && (st.collapsedFraction > 0.01f || st.contentOffset < -0.5f)
        }
    }
    val transition = updateTransition(isContentScrolled, label = "glassTopBarFloat")
    val maskProgress = transition.animateFloat(
        transitionSpec = { GlassMotion.topBarMask() },
        label = "glassTopBarMask",
    ) { if (it) 1f else 0f }

    val overhangPx = remember(density) {
        with(density) { GlassTopAppBarDefaults.BandOverhang.toPx() }
    }
    val bandBrush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface)
    val blurColors = BlurDefaults.blurColors(
        blendColors = listOf(BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.3f)))
    )

    Box(modifier = modifier) {
        if (advancedMaterial) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .graphicsLayer {
                        val bandAlpha = if (wallpaperActive) maskProgress.value else 1f
                        alpha = (bandAlpha * blurAlpha).coerceIn(0f, 1f)
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 0.5f),
                        blurRadius = GlassTopAppBarDefaults.ProgressiveBlurRadius.value,
                        colors = blurColors,
                    )
            )
        } else {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
                    .graphicsLayer {
                        val bandAlpha = if (wallpaperActive) maskProgress.value else 1f
                        alpha = (bandAlpha * blurAlpha).coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleY = if (size.height > 0f) (size.height + overhangPx) / size.height else 1f
                    }
                    .background(bandBrush)
            )
        }
        content()
    }
}
