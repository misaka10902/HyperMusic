package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.internal.drawGlassMask
import top.yukonga.miuix.kmp.glass.internal.drawGlassShadow
import top.yukonga.miuix.kmp.glass.internal.drawGlassStroke

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inheritTopBarMaterial: Boolean = true,
    backdrop: Backdrop? = null,
    surfaceAlpha: Float = 1f,
    style: GlassStyle = LocalGlassTopAppBarContext.current?.style ?: GlassDefaults.Style,
    material: GlassMaterial? = null,
    size: Dp = LocalGlassTopAppBarContext.current?.size ?: GlassTopAppBarDefaults.ButtonSize,
    shape: GlassShape = LocalGlassTopAppBarContext.current?.shape ?: GlassShape(size / 2),
    fill: Color = LocalGlassTopAppBarContext.current?.fill ?: GlassTopAppBarDefaults.buttonFill(),
    stroke: GlassStroke? = LocalGlassTopAppBarContext.current.let { if (it != null) it.stroke else GlassTopAppBarDefaults.buttonStroke() },
    shadow: GlassShadow? = LocalGlassTopAppBarContext.current.let { if (it != null) it.shadow else GlassShadows.Regular },
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val topBarContext = LocalGlassTopAppBarContext.current
    val materialContext = topBarContext.takeIf { inheritTopBarMaterial }
    val resolvedSurfaceAlpha = surfaceAlpha * (topBarContext?.alpha ?: 1f)
    val topBarRamp = topBarContext?.ramp
    GlassButtonSurface(
        backdrop = materialContext?.buttonBackdrop ?: backdrop ?: topBarContext?.buttonBackdrop,
        floating = topBarContext?.floating ?: true,
        surfaceAlpha = resolvedSurfaceAlpha,
        shadowAlpha = { resolvedSurfaceAlpha * (topBarRamp?.value ?: 1f) },
        style = style,
        material = material?.withUnderlayBlur(materialContext?.buttonUnderlayMaterial)
            ?: materialContext?.material ?: GlassTopAppBarDefaults.buttonMaterial(),
        underlayMaterial = materialContext?.buttonUnderlayMaterial,
        shape = shape,
        size = size,
        fill = fill,
        stroke = stroke,
        shadow = shadow,
        sharedProgress = topBarContext?.materialProgress,
        sharedKeepMaterial = topBarContext?.keepMaterial,
        pressed = pressed,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        content = content,
    )
}

@Composable

@Suppress("ktlint:compose:modifier-reused-check", "ktlint:compose:state-param-check")
private fun GlassButtonSurface(
    backdrop: Backdrop?,
    floating: Boolean,
    surfaceAlpha: Float,
    shadowAlpha: () -> Float,
    style: GlassStyle,
    material: GlassMaterial,
    underlayMaterial: GlassMaterial?,
    fill: Color,
    shape: GlassShape,
    size: Dp,
    stroke: GlassStroke?,
    shadow: GlassShadow?,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,

    sharedProgress: State<Float>? = null,
    sharedKeepMaterial: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val popupAnchor = modifier.foldIn<GlassPopupAnchor?>(null) { found, element ->
        if (element is GlassPopupAnchorElement) element.anchor else found
    }
    DisposableEffect(popupAnchor) {
        onDispose { popupAnchor?.surface = null }
    }
    val progress = sharedProgress ?: run {
        val floatingTransition = updateTransition(targetState = floating, label = "glassTopBarButtonFloat")
        floatingTransition.animateFloat(
            transitionSpec = { GlassMotion.topBarButtonFloat() },
            label = "glassTopBarButtonMaterial",
        ) { active ->
            if (active) 1f else 0f
        }
    }
    val ownKeepMaterial by remember(floating, progress) {
        derivedStateOf { floating || progress.value > 0f }
    }
    val keepMaterial = sharedKeepMaterial ?: ownKeepMaterial
    val opacity = surfaceAlpha.coerceIn(0f, 1f)

    val materialOn = opacity > 0.002f
    val anchorSurface = remember(backdrop, style, material, underlayMaterial, stroke, fill) {
        GlassAnchorSurface(backdrop, style, material, underlayMaterial, stroke, fill)
    }
    SideEffect {
        val anchor = popupAnchor ?: return@SideEffect
        anchor.surface = anchorSurface
        anchor.surfaceProgress = progress
        anchor.surfaceMaterialAlpha = opacity
        anchor.surfaceFloating = floating
    }
    val pressTransition = updateTransition(targetState = pressed, label = "glassTopBarButtonPress")
    val contentAlpha = pressTransition.animateFloat(
        transitionSpec = { if (targetState) GlassMotion.pressDown() else GlassMotion.pressUp() },
        label = "glassTopBarButtonContentAlpha",
    ) { down ->
        if (down) GlassTopAppBarDefaults.PressedContentAlpha else 1f
    }
    val overlayAlpha = pressTransition.animateFloat(
        transitionSpec = { if (targetState) GlassMotion.pressDown() else GlassMotion.pressUp() },
        label = "glassTopBarButtonOverlayAlpha",
    ) { down ->
        if (down) 1f else 0f
    }
    val pressedOverlay = GlassTopAppBarDefaults.buttonPressedOverlay()

    val shadowPad = if (shadow != null) {
        ((shadow.radius / GlassDefaults.SourceDensity) + 18f).dp
    } else {
        0.dp
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (shadow != null) {
            Box(
                modifier = Modifier
                    .size(size + shadowPad * 2)
                    .graphicsLayer { alpha = shadowAlpha() }
                    .drawBehind {
                        val pad = shadowPad.toPx()
                        drawGlassShadow(
                            shape = shape,
                            layoutDirection = layoutDirection,
                            shadow = shadow,
                            alpha = 1f,
                            surfaceSize = Size(
                                (this.size.width - pad * 2f).coerceAtLeast(1f),
                                (this.size.height - pad * 2f).coerceAtLeast(1f),
                            ),
                        )
                    },
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()

                .graphicsLayer { alpha = progress.value * opacity }
                .then(
                    if (backdrop != null) {
                        if (underlayMaterial != null) {
                            Modifier.glassOnActionBar(
                                backdrop = backdrop,
                                shape = shape,
                                style = style,
                                alpha = 1f,
                                material = material,
                                underlayMaterial = underlayMaterial,
                                stroke = stroke.takeIf { keepMaterial && materialOn },
                                enabled = keepMaterial && materialOn,
                            )
                        } else {
                            Modifier.glass(
                                backdrop = backdrop,
                                shape = shape,
                                style = style,
                                alpha = 1f,
                                material = material,
                                stroke = stroke.takeIf { keepMaterial && materialOn },
                                enabled = keepMaterial && materialOn,
                                shading = false,
                            )
                        }
                    } else {
                        Modifier
                            .then(
                                if (isRuntimeShaderSupported()) {
                                    Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                } else {
                                    Modifier.clip(shape)
                                },
                            )
                            .background(fill)
                            .drawWithContent {
                                drawContent()
                                if (stroke != null && keepMaterial) {
                                    drawGlassStroke(shape, layoutDirection, stroke, 1f)
                                }
                                drawGlassMask(shape, layoutDirection)
                            }
                    },
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .drawBehind {
                    drawRect(pressedOverlay.copy(alpha = pressedOverlay.alpha * overlayAlpha.value.coerceIn(0f, 1f)))
                },
        )
        Box(
            modifier = Modifier.graphicsLayer { alpha = contentAlpha.value.coerceIn(0f, 1f) },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
