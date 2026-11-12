package top.yukonga.miuix.kmp.interfaces

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.interfaces.HoldDownInteraction.HoldDown
import top.yukonga.miuix.kmp.interfaces.HoldDownInteraction.Release

interface HoldDownInteraction : Interaction {

    class HoldDown : HoldDownInteraction

    class Release(
        val holdDown: HoldDown,
    ) : HoldDownInteraction
}

@Composable
fun InteractionSource.collectIsHeldDownAsState(): State<Boolean> {
    val isHeldDown = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        val holdInteraction = mutableListOf<HoldDown>()
        interactions.collect { interaction ->
            when (interaction) {
                is HoldDown -> holdInteraction.add(interaction)
                is Release -> holdInteraction.remove(interaction.holdDown)
            }
            isHeldDown.value = holdInteraction.isNotEmpty()
        }
    }
    return isHeldDown
}

@Composable
internal fun HoldDownObserver(
    holdDownState: Boolean,
    interactionSource: MutableInteractionSource,
) {
    val holdDown = remember { mutableStateOf<HoldDown?>(null) }
    LaunchedEffect(holdDownState, interactionSource) {
        suspend fun release() {
            holdDown.value?.let { current ->
                interactionSource.emit(Release(current))
                holdDown.value = null
            }
        }
        if (holdDownState) {
            release()
            val interaction = HoldDown()
            holdDown.value = interaction
            interactionSource.emit(interaction)
        } else {
            release()
        }
    }
    DisposableEffect(interactionSource) {
        onDispose {
            holdDown.value?.let { current ->
                interactionSource.tryEmit(Release(current))
            }
            holdDown.value = null
        }
    }
}
