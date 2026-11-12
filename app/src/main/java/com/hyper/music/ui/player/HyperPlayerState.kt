package com.hyper.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.hyper.music.music.CoverSize
import com.hyper.music.music.GdApi
import com.hyper.music.music.PlayMode
import com.hyper.music.music.PlaybackController
import com.hyper.music.music.PlaybackUiState
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class PlayerTrack(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String?,
    val lyricId: String = "",
    val hasSong: Boolean,
)

class HyperPlayerState(
    private val coroutineScope: CoroutineScope,
) {
    private val expandAnimatable = Animatable(0f)

    val expandProgress: Float get() = expandAnimatable.value

    var isExpanded: Boolean by mutableStateOf(false)
        private set

    var playback: PlaybackUiState by mutableStateOf(PlaybackUiState())

    val track: PlayerTrack
        get() {
            val song = playback.song
            return PlayerTrack(
                title = song?.name ?: "未播放歌曲",
                artist = song?.artistText ?: "快去邂逅合拍的节奏吧",
                album = song?.albumText ?: "",
                durationMs = if (playback.durationMs > 0) playback.durationMs else (song?.durationMs ?: 0L),
                coverUrl = song?.let {
                    GdApi.coverUrl(it, CoverSize.LARGE.request).ifBlank { null }
                },
                lyricId = song?.lyricId.orEmpty(),
                hasSong = song != null,
            )
        }

    val isPlaying: Boolean get() = playback.isPlaying
    val positionMs: Long get() = playback.positionMs

    fun togglePlayPause() = PlaybackController.toggle()
    fun previous() = PlaybackController.previous()
    fun next() = PlaybackController.next()
    fun seekTo(ms: Long) = PlaybackController.seekTo(ms)
    fun cyclePlayMode() = PlaybackController.cyclePlayMode()

    val playMode: PlayMode get() = playback.mode

    fun expand() {
        if (isExpanded) return
        isExpanded = true
        animateTo(1f, ExpandSpec)
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false
        animateTo(0f, CollapseSpec)
    }

    fun dragBy(deltaPx: Float, containerHeightPx: Float) {
        if (!isExpanded) return
        val range = (containerHeightPx * 0.7f).absoluteValue.coerceAtLeast(1f)
        val target = (expandProgress - deltaPx / range).coerceIn(0f, 1f)
        coroutineScope.launch { expandAnimatable.snapTo(target) }
    }

    fun endDrag(velocityY: Float) {
        if (!isExpanded) return
        val shouldCollapse = velocityY > 900f || expandProgress < 0.62f
        isExpanded = !shouldCollapse
        val target = if (shouldCollapse) 0f else 1f
        val remaining = (target - expandProgress).absoluteValue
        val durationMs = (remaining * 200f).coerceIn(90f, 220f).roundToInt()
        animateTo(target, tween(durationMs, easing = LinearEasing))
    }

    private fun animateTo(target: Float, spec: AnimationSpec<Float>, initialVelocity: Float = 0f) {
        coroutineScope.launch { expandAnimatable.animateTo(target, spec, initialVelocity = initialVelocity) }
    }

    companion object {
        val ExpandSpec: AnimationSpec<Float> =
            tween(durationMillis = 260, easing = FastOutSlowInEasing)
        val CollapseSpec: AnimationSpec<Float> =
            tween(durationMillis = 220, easing = FastOutSlowInEasing)
    }
}

@Composable
fun rememberHyperPlayerState(): HyperPlayerState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { HyperPlayerState(scope) }

    val playback by PlaybackController.state.collectAsState()
    state.playback = playback

    return state
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = (ms / 1000f).roundToLong()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
