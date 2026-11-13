package com.hyper.music.music

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * 音频焦点：开启后主动持有 AUDIOFOCUS_GAIN，任何焦点丢失（含 duck）一律暂停；
 * 关闭时完全不介入，允许与其他应用音频同时播放。
 *
 * 参考 Melox 的 PlaybackAudioFocus：
 *  - 以 playWhenReady（播放意图）而非 isPlaying 驱动焦点请求/释放；
 *  - setAcceptsDelayedFocusGain(true)：否则焦点被占用时请求直接失败、收不到任何回调；
 *  - 打开开关且正在播放时立即补请求，避免「先播放、后开开关」永不生效；
 *  - generation 丢弃 abandon 之后的旧回调。
 */
object AudioFocusMonitor {

    private var audioManager: AudioManager? = null
    private val handler = Handler(Looper.getMainLooper())

    private var enabled = false
    private var playbackRequested = false
    private var focusRequest: AudioFocusRequest? = null
    private var generation = 0L

    private fun isFocusLoss(change: Int): Boolean =
        change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK

    /** Application 启动时调用，恢复持久化开关。 */
    fun init(context: Context) {
        audioManager = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        enabled = MusicLibrary.settings.value.audioFocus
    }

    /** 设置页切换开关即时生效。 */
    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        val wasEnabled = enabled
        enabled = value
        when {
            !value -> clearFocus()
            !wasEnabled && playbackRequested -> acquireFocus()
        }
    }

    /** 由 Player.Listener.onPlayWhenReadyChanged 驱动，反映用户/媒体会话的播放意图。 */
    fun onPlaybackIntentChanged(play: Boolean) {
        playbackRequested = play
        if (!enabled) return
        if (play) acquireFocus() else clearFocus()
    }

    private fun acquireFocus() {
        val am = audioManager ?: return
        if (focusRequest != null) return
        val myGeneration = ++generation
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener({ change ->
                if (enabled && generation == myGeneration && isFocusLoss(change) && playbackRequested) {
                    PlaybackController.pause()
                }
            }, handler)
            .build()
        focusRequest = request
        if (am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            clearFocus()
        }
    }

    private fun clearFocus() {
        generation++
        focusRequest?.let { req ->
            runCatching { audioManager?.abandonAudioFocusRequest(req) }
        }
        focusRequest = null
    }
}
