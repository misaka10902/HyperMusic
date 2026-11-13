package com.hyper.music.music

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayMode { ORDER, SHUFFLE, REPEAT_ONE }

data class PlaybackUiState(
    val song: Song? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val index: Int = -1,
    val mode: PlayMode = PlayMode.ORDER,
    val volume: Float = 1f,
    val error: String? = null,
) {
    val hasSong: Boolean get() = song != null
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

object PlaybackController {

    private var player: Player? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    private var queue: List<Song> = emptyList()

    private var liveLyricLines: List<LyricLine> = emptyList()
    private var liveLyricSongId: String = ""
    private var lastPublishedTitle: String? = null
    private var lyricLoadJob: Job? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = sync()
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val p = player
                if (p != null && p.mediaItemCount > 0) {
                    p.seekTo(0, 0L)
                    p.play()
                }
            }
            sync()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            sync()
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                _state.value.song?.let { MusicLibrary.addHistory(it) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = error.message ?: "播放失败", isLoading = false) }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) = sync()

        override fun onVolumeChanged(volume: Float) {
            _state.update { it.copy(volume = volume) }
        }
    }

    fun attach(exo: Player) {
        if (player === exo) return
        player?.removeListener(listener)
        player = exo
        exo.addListener(listener)
        exo.volume = MusicLibrary.settings.value.volume
        (exo as? ExoPlayer)?.setAudioAttributes(
            audioAttributes(),
            MusicLibrary.settings.value.audioFocus,
        )
        applyMode(exo, _state.value.mode)
        restoreSession(exo)
        sync()
        startTicker()
    }

    fun detach() {
        saveSession()
        player?.removeListener(listener)
        player = null
        ticker?.cancel()
        ticker = null
        lyricLoadJob?.cancel()
        liveLyricLines = emptyList()
        liveLyricSongId = ""
    }

    fun isAttached(): Boolean = player != null

    fun saveSession() {
        val songs = queue
        if (songs.isEmpty()) return
        val p = player
        val index = (p?.currentMediaItemIndex ?: _state.value.index).coerceIn(0, songs.lastIndex)
        val position = (p?.currentPosition ?: _state.value.positionMs).coerceAtLeast(0L)
        MusicLibrary.saveSession(songs, index, position, _state.value.mode)
    }

    private fun restoreSession(p: Player) {
        if (queue.isNotEmpty()) return
        val saved = MusicLibrary.loadSession() ?: return
        if (saved.songs.isEmpty()) return
        val index = saved.index.coerceIn(0, saved.songs.lastIndex)
        queue = saved.songs
        _state.value = PlaybackUiState(
            song = saved.songs[index],
            isPlaying = false,
            isLoading = false,
            positionMs = saved.positionMs,
            durationMs = saved.songs[index].durationMs,
            queue = saved.songs,
            index = index,
            mode = saved.mode,
            volume = _state.value.volume,
        )
        applyMode(p, saved.mode)
        p.setMediaItems(saved.songs.map { it.toMediaItem(currentBr) }, index, saved.positionMs)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val p = player
                if (p != null && p.isPlaying) {
                    _state.update {
                        it.copy(
                            positionMs = p.currentPosition.coerceAtLeast(0L),
                            durationMs = p.duration.safeDuration(it.durationMs),
                        )
                    }
                    publishCurrentLyric()
                }
                delay(400)
            }
        }
    }

    private fun Long.safeDuration(fallback: Long): Long =
        if (this == C.TIME_UNSET || this <= 0L) fallback else this

    private fun sync() {
        val p = player ?: return
        val index = p.currentMediaItemIndex
        val current = queue.getOrNull(index)
            ?: p.currentMediaItem?.mediaId?.let { id -> queue.firstOrNull { it.id == id } }
        _state.update {
            it.copy(
                song = current ?: it.song,
                isPlaying = p.isPlaying,
                isLoading = p.playbackState == Player.STATE_BUFFERING,
                positionMs = p.currentPosition.coerceAtLeast(0L),
                durationMs = p.duration.safeDuration(current?.durationMs ?: it.durationMs),
                queue = queue,
                index = index,
                mode = it.mode,
                error = null,
            )
        }
        if (current?.id != liveLyricSongId) loadLiveLyrics(current)
        saveSession()
    }

    private val currentBr: Int get() = MusicLibrary.settings.value.quality.br

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val index = startIndex.coerceIn(0, songs.lastIndex)
        _state.update {
            it.copy(
                queue = songs,
                index = index,
                song = songs[index],
                error = null,
                isLoading = true,
            )
        }
        MusicLibrary.addHistory(songs[index])
        scope.launch { prepareAndPlay(songs, index) }
    }

    private suspend fun prepareAndPlay(songs: List<Song>, index: Int) {
        val br = currentBr
        val p = player ?: return
        queue = songs
        val items = songs.map { it.toMediaItem(br) }
        p.setMediaItems(items, index, 0L)
        applyMode(p, _state.value.mode)
        p.prepare()
        play()
        sync()
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) pause() else play()
    }

    private val fadeSteps = 15
    private val fadeStepMs = 20L
    private var fadeJob: Job? = null

    private fun audioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    fun play() {
        val p = player ?: return
        if (p.playbackState == Player.STATE_IDLE) p.prepare()
        val target = MusicLibrary.settings.value.volume
        fadeJob?.cancel()
        if (!MusicLibrary.settings.value.audioFade) {
            p.volume = target
            p.play()
            return
        }
        p.volume = 0f
        p.play()
        fadeJob = scope.launch {
            for (i in 1..fadeSteps) {
                delay(fadeStepMs)
                p.volume = target * i / fadeSteps
            }
            p.volume = target
        }
    }

    fun pause() {
        val p = player ?: return
        val target = MusicLibrary.settings.value.volume
        fadeJob?.cancel()
        if (!MusicLibrary.settings.value.audioFade) {
            p.pause()
            return
        }
        val start = p.volume
        fadeJob = scope.launch {
            for (i in 1..fadeSteps) {
                delay(fadeStepMs)
                p.volume = start * (1f - i.toFloat() / fadeSteps)
            }
            p.pause()
            p.volume = target
        }
    }

    fun next() {
        val p = player ?: return
        if (queue.isEmpty()) return
        when (_state.value.mode) {
            PlayMode.SHUFFLE -> p.seekToNextMediaItem()
            else -> {
                val idx = p.currentMediaItemIndex.coerceAtLeast(0)
                val target = if (idx < queue.lastIndex) idx + 1 else 0
                p.seekTo(target, 0L)
            }
        }
        p.play()
        sync()
    }

    fun playAt(index: Int) {
        val p = player ?: return
        if (index !in queue.indices) return
        p.seekTo(index, 0L)
        p.play()
        sync()
    }

    fun previous() {
        val p = player ?: return
        if (queue.isEmpty()) return
        if (_state.value.mode == PlayMode.SHUFFLE) {
            p.seekToPreviousMediaItem()
            sync()
            return
        }
        if (p.currentPosition > 3000L) {
            p.seekTo(0L)
        } else {
            val idx = p.currentMediaItemIndex
            val target = if (idx > 0) idx - 1 else queue.lastIndex
            if (target in queue.indices) p.seekTo(target, 0L) else p.seekTo(0L)
        }
        p.play()
        sync()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun cyclePlayMode() {
        val next = when (_state.value.mode) {
            PlayMode.ORDER -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.ORDER
        }
        _state.update { it.copy(mode = next) }
        player?.let { applyMode(it, next) }
    }

    private fun applyMode(p: Player, mode: PlayMode) {
        p.shuffleModeEnabled = mode == PlayMode.SHUFFLE
        p.repeatMode = if (mode == PlayMode.REPEAT_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        fadeJob?.cancel()
        player?.volume = v
        _state.update { it.copy(volume = v) }
    }

    fun reloadCurrent() {
        val p = player ?: return
        val idx = p.currentMediaItemIndex
        val song = queue.getOrNull(idx) ?: return
        val pos = p.currentPosition
        p.replaceMediaItem(idx, song.toMediaItem(currentBr))
        p.seekTo(idx, pos)
        sync()
    }

    fun playNext(song: Song) {
        val p = player ?: return
        if (queue.isEmpty()) {
            playSongs(listOf(song), 0)
            return
        }
        val insertAt = (p.currentMediaItemIndex + 1).coerceIn(0, queue.size)
        queue = queue.toMutableList().apply { add(insertAt, song) }
        p.addMediaItem(insertAt, song.toMediaItem(currentBr))
        _state.update { it.copy(queue = queue) }
    }

    fun addToQueue(song: Song) {
        val p = player ?: return
        if (queue.isEmpty()) {
            playSongs(listOf(song), 0)
            return
        }
        queue = queue + song
        p.addMediaItem(song.toMediaItem(currentBr))
        _state.update { it.copy(queue = queue) }
    }

    fun removeFromQueue(songId: String) {
        val p = player ?: return
        val index = queue.indexOfFirst { it.id == songId }
        if (index < 0) return
        queue = queue.toMutableList().apply { removeAt(index) }
        p.removeMediaItem(index)
        _state.update { it.copy(queue = queue) }
        sync()
    }

    fun moveInQueue(from: Int, to: Int) {
        val p = player ?: return
        if (from !in queue.indices || to !in queue.indices || from == to) return
        val list = queue.toMutableList()
        val song = list.removeAt(from)
        list.add(to, song)
        queue = list
        p.moveMediaItem(from, to)
        _state.update { it.copy(queue = queue) }
        sync()
    }

    fun removeByLocalUri(uris: Set<String>) {
        val p = player ?: return
        if (uris.isEmpty() || queue.isEmpty()) return
        val matched: (Song) -> Boolean = { it.isLocal && it.localUri in uris }

        val removeIndexes = queue.mapIndexedNotNull { i, s -> if (matched(s)) i else null }
        if (removeIndexes.isEmpty()) return

        for (i in removeIndexes.sortedDescending()) {
            runCatching { p.removeMediaItem(i) }
        }
        queue = queue.filterNot(matched)
        if (queue.isEmpty()) {
            clearQueue()
            return
        }
        _state.update { it.copy(queue = queue) }
        sync()
    }

    fun clearQueue() {
        val p = player ?: return
        queue = emptyList()
        MusicLibrary.clearSession()
        p.clearMediaItems()
        p.stop()
        _state.value = PlaybackUiState(mode = _state.value.mode)
        lyricLoadJob?.cancel()
        liveLyricLines = emptyList()
        liveLyricSongId = ""
        lastPublishedTitle = null
    }

    fun refreshLiveLyric() {
        loadLiveLyrics(_state.value.song)
    }

    private fun loadLiveLyrics(song: Song?) {
        lyricLoadJob?.cancel()
        liveLyricLines = emptyList()
        lastPublishedTitle = null
        liveLyricSongId = song?.id.orEmpty()
        if (song == null) return

        if (song.isLocal) {
            publishTitle(song.name)
            return
        }
        if (!MusicLibrary.settings.value.liveLyric) {
            publishTitle(song.name)
            return
        }
        lyricLoadJob = scope.launch {
            val result = runCatching { GdApi.fullLyric(song) }.getOrNull()
            liveLyricLines = result?.let { buildLyrics(it.lyric, null) } ?: emptyList()
            publishCurrentLyric()
        }
    }

    private fun publishCurrentLyric() {
        val p = player ?: return
        val song = _state.value.song ?: return
        if (!MusicLibrary.settings.value.liveLyric) return
        val index = currentLineIndex(liveLyricLines, p.currentPosition)
        val text = liveLyricLines.getOrNull(index)?.text?.takeIf { it.isNotBlank() } ?: song.name
        publishTitle(text)
    }

    private fun publishTitle(title: String?) {
        if (title == lastPublishedTitle) return
        lastPublishedTitle = title
        val p = player ?: return
        val index = p.currentMediaItemIndex
        val item = p.currentMediaItem ?: return
        val song = queue.getOrNull(index) ?: return
        val metadata = item.mediaMetadata.buildUpon()
            .setTitle(title ?: song.name)
            .build()
        p.replaceMediaItem(index, item.buildUpon().setMediaMetadata(metadata).build())
    }

    fun currentSong(): Song? = _state.value.song

    private fun Song.toMediaItem(br: Int): MediaItem {
        val song = this

        val uri: String = if (isLocal) localUri else GdApi.streamEndpoint(id, br, source)
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setArtist(artistText)
                    .setAlbumTitle(albumText)
                    .apply {
                        if (isLocal) {
                            if (cover.isNotBlank()) {
                                runCatching { setArtworkUri(Uri.parse(cover)) }
                            }
                        } else {
                            ArtworkProvider.artworkUri(song, CoverSize.LARGE.request)
                                ?.let { setArtworkUri(it) }
                        }
                    }
                    .build()
            )
            .build()
    }
}
