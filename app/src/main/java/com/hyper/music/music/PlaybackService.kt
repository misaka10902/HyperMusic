package com.hyper.music.music

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.DefaultMediaCodecAdapterFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.wav.WavExtractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.hyper.music.R
import com.hyper.music.ui.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        val mediaSourceFactory = ProgressiveMediaSource.Factory(
            MediaCache.dataSourceFactory(this),
            ExtractorsFactory {
                arrayOf(
                    Mp3Extractor(),
                    Mp4Extractor(SubtitleParser.Factory.UNSUPPORTED),
                    FlacExtractor(),
                    OggExtractor(),
                    WavExtractor(),
                    AdtsExtractor(),
                    AmrExtractor(),
                    Ac3Extractor(),
                )
            },
        )

        val exo = ExoPlayer.Builder(this, AudioOnlyRenderersFactory(this), mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
 MusicLibrary.settings.value.audioFocus,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exo

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaSession.Builder(this, exo)
            .setSessionActivity(openApp)
            .setCallback(HyperMediaSessionCallback())
            .build()

        updateCustomLayout()
        serviceScope.launch {
            combine(PlaybackController.state, MusicLibrary.favorites) { st, fav ->
                val id = st.song?.id
                Triple(st.mode, id, if (id == null) false else fav.any { it.id == id })
            }.distinctUntilChanged().collect { updateCustomLayout() }
        }

        PlaybackController.attach(exo)
    }


    @Suppress("DEPRECATION")
    private fun buildCustomLayout(): List<CommandButton> {
        val state = PlaybackController.state.value
        val songId = state.song?.id
        val liked = songId != null && MusicLibrary.isFavorite(songId)
        val (modeIcon, modeLabel) = when (state.mode) {
            PlayMode.ORDER -> R.drawable.ic_island_mode_order to "列表循环"
            PlayMode.SHUFFLE -> R.drawable.ic_island_mode_shuffle to "随机播放"
            PlayMode.REPEAT_ONE -> R.drawable.ic_island_mode_repeat_one to "单曲循环"
        }
        return listOf(
            CommandButton.Builder()
                .setDisplayName(modeLabel)
                .setIconResId(modeIcon)
                .setSessionCommand(SessionCommand(ACTION_CYCLE_MODE, Bundle.EMPTY))
                .setExtras(
                    Bundle().apply {
                        putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, 0)
                    }
                )
                .build(),
            CommandButton.Builder()
                .setDisplayName(if (liked) "取消收藏" else "收藏")
                .setIconResId(
                    if (liked) R.drawable.ic_island_favorite_fill
                    else R.drawable.ic_island_favorite
                )
                .setSessionCommand(SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                .setExtras(
                    Bundle().apply {
                        putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, 2)
                    }
                )
                .build(),
        )
    }

    private fun updateCustomLayout() {
        session?.setCustomLayout(buildCustomLayout())
    }

    private inner class HyperMediaSessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_CYCLE_MODE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(buildCustomLayout())
                .build()
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: Int,
        ): Int {
            if (command == Player.COMMAND_PLAY_PAUSE) {
                if (session.player.isPlaying) {
                    PlaybackController.pause()
                } else {
                    PlaybackController.play()
                }
                return SessionResult.RESULT_INFO_SKIPPED
            }
            return super.onPlayerCommandRequest(session, controller, command)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_CYCLE_MODE -> PlaybackController.cyclePlayMode()
                ACTION_TOGGLE_FAVORITE -> PlaybackController.currentSong()?.let {
                    MusicLibrary.toggleFavorite(it)
                }
            }
            updateCustomLayout()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        PlaybackController.saveSession()
        val p = player
        if (p == null || !p.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        PlaybackController.detach()
        session?.run {
            player.release()
            release()
        }
        session = null
        player = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_CYCLE_MODE = "com.hyper.music.CYCLE_MODE"
        private const val ACTION_TOGGLE_FAVORITE = "com.hyper.music.TOGGLE_FAVORITE"
    }
}

private class AudioOnlyRenderersFactory(private val context: Context) : RenderersFactory {
    override fun createRenderers(
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        audioRendererEventListener: AudioRendererEventListener,
        textRendererOutput: TextOutput,
        metadataRendererOutput: MetadataOutput,
    ): Array<Renderer> = arrayOf(
        MediaCodecAudioRenderer(
            context,
            DefaultMediaCodecAdapterFactory(context),
            MediaCodecSelector.DEFAULT,
            true,
            eventHandler,
            audioRendererEventListener,
            DefaultAudioSink.Builder(context).build(),
        )
    )
}
