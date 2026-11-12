package com.hyper.music.ui.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.anim.folmeSpring

class ChipBounce internal constructor(
    private val anim: Animatable<Float, AnimationVector1D>,
) {

    val scale: State<Float> = derivedStateOf { anim.value }
}

private const val PressedScale = 0.952f

private const val TapDipScale = 0.93f

private const val TapThresholdMs = 120L

@Composable
fun rememberChipBounce(interactionSource: MutableInteractionSource): ChipBounce {
    val anim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(interactionSource) {
        var pressedAt = 0L
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressedAt = System.currentTimeMillis()

                    scope.launch {
                        anim.animateTo(PressedScale, folmeSpring(damping = 1f, response = 0.07f))
                    }
                }

                is PressInteraction.Release -> {
                    val heldMs = System.currentTimeMillis() - pressedAt
                    scope.launch {

                        if (heldMs < TapThresholdMs && anim.value > 0.975f) {
                            anim.animateTo(
                                targetValue = TapDipScale,
                                animationSpec = tween(
                                    durationMillis = 45,
                                    easing = LinearOutSlowInEasing,
                                ),
                            )
                        }
                        anim.animateTo(1f, folmeSpring(damping = 1f, response = 0.16f))
                    }
                }

                is PressInteraction.Cancel -> {
                    scope.launch {
                        anim.animateTo(1f, folmeSpring(damping = 1f, response = 0.16f))
                    }
                }
            }
        }
    }

    return remember(interactionSource) { ChipBounce(anim) }
}

@Composable
fun Modifier.chipBounce(bounce: ChipBounce): Modifier = this.graphicsLayer {
    val s = bounce.scale.value
    scaleX = s
    scaleY = s
}
