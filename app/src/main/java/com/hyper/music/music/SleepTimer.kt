package com.hyper.music.music

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SleepTimerState(
    val active: Boolean = false,
    val remainingMs: Long = 0L,
    val extendToTrackEnd: Boolean = false,
)

object SleepTimer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state

    fun start(hours: Int, minutes: Int, seconds: Int, extendToTrackEnd: Boolean) {
        val totalMs = (hours.toLong() * 3600L + minutes.toLong() * 60L + seconds.toLong()) * 1000L
        if (totalMs <= 0L) return
        job?.cancel()
        _state.value = SleepTimerState(active = true, remainingMs = totalMs, extendToTrackEnd = extendToTrackEnd)
        job = scope.launch {
            var remaining = totalMs
            while (remaining > 0L && isActive) {
                delay(1000L)
                remaining -= 1000L
                _state.value = _state.value.copy(remainingMs = remaining.coerceAtLeast(0L))
            }
            if (extendToTrackEnd && isActive) {
                val s = PlaybackController.state.value
                var left = (s.durationMs - s.positionMs).coerceAtLeast(0L)
                if (left > 1500L) {
                    while (left > 0L && isActive) {
                        delay(1000L)
                        left -= 1000L
                        _state.value = _state.value.copy(remainingMs = left.coerceAtLeast(0L))
                    }
                }
            }
            if (isActive) {
                PlaybackController.pause()
                _state.value = SleepTimerState(active = false)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = SleepTimerState()
    }
}
