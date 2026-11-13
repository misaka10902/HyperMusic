package com.hyper.music.music

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

object MusicLibrary {

    private const val PREFS = "music_prefs"
    private const val KEY_HISTORY = "history"
    private const val KEY_PLAYLISTS = "playlists"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_API = "api_base"
    private const val KEY_QUALITY = "quality"
    private const val KEY_VOLUME = "volume"
    private const val KEY_COVER_RADIUS = "cover_corner_radius"
    private const val KEY_COVER_CIRCLE = "cover_circle"
    private const val KEY_LYRIC_CENTERED = "lyric_centered"
    private const val KEY_LYRIC_FONT_SIZE = "lyric_font_size"
    private const val KEY_LYRIC_FONT_WEIGHT = "lyric_font_weight"
    private const val KEY_TITLE_CENTERED = "title_centered"
    private const val KEY_LYRIC_TRANSLATION = "lyric_translation"
    private const val KEY_ADVANCED_MATERIAL = "advanced_material"
    private const val KEY_ADVANCED_MATERIAL_MODE = "advanced_material_mode"
    private const val KEY_AUDIO_FADE = "audio_fade"
    private const val KEY_LIVE_LYRIC = "live_lyric"
    private const val KEY_AUDIO_FOCUS = "audio_focus"
    private const val KEY_DOWNLOAD_QUALITY = "download_quality"
    private const val KEY_DOWNLOAD_METADATA = "download_metadata"
    private const val KEY_DOWNLOAD_PATH = "download_path"
    private const val KEY_DOWNLOAD_TREE_URI = "download_tree_uri"
    private const val KEY_SESSION_QUEUE = "session_queue"
    private const val KEY_SESSION_INDEX = "session_index"
    private const val KEY_SESSION_POSITION = "session_position"
    private const val KEY_SESSION_MODE = "session_mode"

    private const val MAX_HISTORY = 200

    private lateinit var prefs: SharedPreferences

    private val _history = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _favorites = MutableStateFlow<List<Song>>(emptyList())
    val favorites: StateFlow<List<Song>> = _favorites

    private val _settings = MutableStateFlow(MusicSettings())
    val settings: StateFlow<MusicSettings> = _settings

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _history.value = decodeSongs(prefs.getString(KEY_HISTORY, null))
        _playlists.value = decodePlaylists(prefs.getString(KEY_PLAYLISTS, null))
        _favorites.value = decodeSongs(prefs.getString(KEY_FAVORITES, null))
        _settings.value = MusicSettings(
            apiBase = prefs.getString(KEY_API, MusicSettings.DEFAULT_API_BASE)
                ?.ifBlank { MusicSettings.DEFAULT_API_BASE }
                ?: MusicSettings.DEFAULT_API_BASE,
            quality = AudioQuality.entries.firstOrNull { it.br == prefs.getInt(KEY_QUALITY, 192) }
                ?: AudioQuality.HIGH,
            volume = prefs.getFloat(KEY_VOLUME, 1f),
            coverCornerRadius = prefs.getFloat(KEY_COVER_RADIUS, 12f),
            coverCircular = prefs.getBoolean(KEY_COVER_CIRCLE, true),
            lyricCentered = prefs.getBoolean(KEY_LYRIC_CENTERED, true),
            lyricFontSize = prefs.getFloat(KEY_LYRIC_FONT_SIZE, 20f).coerceIn(14f, 36f),
            lyricFontWeight = prefs.getInt(KEY_LYRIC_FONT_WEIGHT, 500).coerceIn(300, 900),
            titleCentered = prefs.getBoolean(KEY_TITLE_CENTERED, true),
            showLyricsTranslation = prefs.getBoolean(KEY_LYRIC_TRANSLATION, true),
            advancedMaterial = readAdvancedMaterial(),
            audioFade = prefs.getBoolean(KEY_AUDIO_FADE, true),
            liveLyric = prefs.getBoolean(KEY_LIVE_LYRIC, false),
            audioFocus = prefs.getBoolean(KEY_AUDIO_FOCUS, false),
            downloadQuality = AudioQuality.entries.firstOrNull {
                it.br == prefs.getInt(KEY_DOWNLOAD_QUALITY, 128)
            } ?: AudioQuality.STANDARD,
            downloadMetadata = prefs.getBoolean(KEY_DOWNLOAD_METADATA, true),
            downloadPath = prefs.getString(KEY_DOWNLOAD_PATH, null)
                ?.takeIf { it.isNotBlank() } ?: MusicSettings.DEFAULT_DOWNLOAD_PATH,
            downloadTreeUri = prefs.getString(KEY_DOWNLOAD_TREE_URI, null) ?: "",
        )
    }

    fun setQuality(quality: AudioQuality) {
        _settings.value = _settings.value.copy(quality = quality)
        prefs.edit().putInt(KEY_QUALITY, quality.br).apply()
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        _settings.value = _settings.value.copy(volume = v)
        prefs.edit().putFloat(KEY_VOLUME, v).apply()
    }

    fun setCoverCircular(circular: Boolean) {
        _settings.value = _settings.value.copy(coverCircular = circular)
        prefs.edit().putBoolean(KEY_COVER_CIRCLE, circular).apply()
    }

    fun setAudioFade(enabled: Boolean) {
        _settings.value = _settings.value.copy(audioFade = enabled)
        prefs.edit().putBoolean(KEY_AUDIO_FADE, enabled).apply()
    }

    fun setLiveLyric(enabled: Boolean) {
        _settings.value = _settings.value.copy(liveLyric = enabled)
        prefs.edit().putBoolean(KEY_LIVE_LYRIC, enabled).apply()
    }

    fun setDownloadQuality(quality: AudioQuality) {
        _settings.value = _settings.value.copy(downloadQuality = quality)
        prefs.edit().putInt(KEY_DOWNLOAD_QUALITY, quality.br).apply()
    }

    fun setDownloadMetadata(enabled: Boolean) {
        _settings.value = _settings.value.copy(downloadMetadata = enabled)
        prefs.edit().putBoolean(KEY_DOWNLOAD_METADATA, enabled).apply()
    }

    fun setDownloadPath(path: String, treeUri: String? = null) {
        prefs.edit().putString(KEY_DOWNLOAD_TREE_URI, treeUri ?: "").apply()
        setDownloadPathInternal(path)
    }

    private fun setDownloadPathInternal(path: String) {
        val normalized = path.trim().ifBlank { MusicSettings.DEFAULT_DOWNLOAD_PATH }
            .let { if (it.endsWith('/')) it else "$it/" }
        _settings.value = _settings.value.copy(downloadPath = normalized)
        prefs.edit().putString(KEY_DOWNLOAD_PATH, normalized).apply()
    }

    fun setLyricCentered(centered: Boolean) {
        _settings.value = _settings.value.copy(lyricCentered = centered)
        prefs.edit().putBoolean(KEY_LYRIC_CENTERED, centered).apply()
    }

    fun setLyricFontWeight(weight: Int) {
        val v = weight.coerceIn(300, 900)
        _settings.value = _settings.value.copy(lyricFontWeight = v)
        prefs.edit().putInt(KEY_LYRIC_FONT_WEIGHT, v).apply()
    }

    fun setLyricFontSize(size: Float) {
        val v = size.coerceIn(14f, 36f)
        _settings.value = _settings.value.copy(lyricFontSize = v)
        prefs.edit().putFloat(KEY_LYRIC_FONT_SIZE, v).apply()
    }

    fun setTitleCentered(centered: Boolean) {
        _settings.value = _settings.value.copy(titleCentered = centered)
        prefs.edit().putBoolean(KEY_TITLE_CENTERED, centered).apply()
    }

    fun setShowLyricsTranslation(show: Boolean) {
        _settings.value = _settings.value.copy(showLyricsTranslation = show)
        prefs.edit().putBoolean(KEY_LYRIC_TRANSLATION, show).apply()
    }

    fun setAudioFocus(enabled: Boolean) {
        _settings.value = _settings.value.copy(audioFocus = enabled)
        prefs.edit().putBoolean(KEY_AUDIO_FOCUS, enabled).apply()
    }

    fun setAdvancedMaterial(enabled: Boolean) {
        _settings.value = _settings.value.copy(advancedMaterial = enabled)
        prefs.edit().putBoolean(KEY_ADVANCED_MATERIAL, enabled).apply()
    }

    private fun readAdvancedMaterial(): Boolean {
        val legacy = prefs.getString(KEY_ADVANCED_MATERIAL_MODE, null)
        if (legacy != null) {
            val enabled = legacy != "OFF"
            prefs.edit()
                .putBoolean(KEY_ADVANCED_MATERIAL, enabled)
                .remove(KEY_ADVANCED_MATERIAL_MODE)
                .apply()
            return enabled
        }
        return prefs.getBoolean(KEY_ADVANCED_MATERIAL, true)
    }

    fun addHistory(song: Song) {
        // 历史列表顺序写死：已在列表中的歌曲保持原位，只把新歌追加到尾部，不做任何重排
        val current = _history.value
        if (current.any { it.id == song.id }) return
        val list = current.toMutableList()
        list.add(song)
        while (list.size > MAX_HISTORY) list.removeAt(0)
        _history.value = list
        prefs.edit().putString(KEY_HISTORY, encodeSongs(list)).apply()
    }

    fun removeHistory(songId: String) {
        val list = _history.value.filterNot { it.id == songId }
        _history.value = list
        prefs.edit().putString(KEY_HISTORY, encodeSongs(list)).apply()
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun removeByLocalUri(uris: Set<String>) {
        if (uris.isEmpty()) return
        val matched: (Song) -> Boolean = { it.isLocal && it.localUri in uris }

        val newHistory = _history.value.filterNot(matched)
        if (newHistory.size != _history.value.size) {
            _history.value = newHistory
            prefs.edit().putString(KEY_HISTORY, encodeSongs(newHistory)).apply()
        }

        val newFavorites = _favorites.value.filterNot(matched)
        if (newFavorites.size != _favorites.value.size) {
            _favorites.value = newFavorites
            prefs.edit().putString(KEY_FAVORITES, encodeSongs(newFavorites)).apply()
        }

        var playlistsChanged = false
        val newPlaylists = _playlists.value.map { p ->
            val songs = p.songs.filterNot(matched)
            if (songs.size != p.songs.size) {
                playlistsChanged = true
                p.copy(songs = songs)
            } else p
        }
        if (playlistsChanged) {
            _playlists.value = newPlaylists
            persistPlaylists(newPlaylists)
        }
    }

    data class SavedSession(
        val songs: List<Song>,
        val index: Int,
        val positionMs: Long,
        val mode: PlayMode,
    )

    fun saveSession(songs: List<Song>, index: Int, positionMs: Long, mode: PlayMode) {
        if (songs.isEmpty()) {
            clearSession()
            return
        }
        prefs.edit()
            .putString(KEY_SESSION_QUEUE, encodeSongs(songs))
            .putInt(KEY_SESSION_INDEX, index)
            .putLong(KEY_SESSION_POSITION, positionMs)
            .putString(KEY_SESSION_MODE, mode.name)
            .apply()
    }

    fun loadSession(): SavedSession? {
        val songs = decodeSongs(prefs.getString(KEY_SESSION_QUEUE, null))
        if (songs.isEmpty()) return null
        val index = prefs.getInt(KEY_SESSION_INDEX, 0).coerceIn(0, songs.lastIndex)
        val mode = prefs.getString(KEY_SESSION_MODE, null)
            ?.let { runCatching { PlayMode.valueOf(it) }.getOrNull() } ?: PlayMode.ORDER
        return SavedSession(
            songs,
            index,
            prefs.getLong(KEY_SESSION_POSITION, 0L).coerceAtLeast(0L),
            mode,
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_SESSION_QUEUE)
            .remove(KEY_SESSION_INDEX)
            .remove(KEY_SESSION_POSITION)
            .remove(KEY_SESSION_MODE)
            .apply()
    }

    fun isFavorite(songId: String): Boolean = _favorites.value.any { it.id == songId }

    fun toggleFavorite(song: Song): Boolean {
        val current = _favorites.value
        val added = current.none { it.id == song.id }
        val list = if (added) current + song else current.filterNot { it.id == song.id }
        _favorites.value = list
        prefs.edit().putString(KEY_FAVORITES, encodeSongs(list)).apply()
        return added
    }

    fun removeFavorite(songId: String) {
        val list = _favorites.value.filterNot { it.id == songId }
        if (list.size == _favorites.value.size) return
        _favorites.value = list
        prefs.edit().putString(KEY_FAVORITES, encodeSongs(list)).apply()
    }

    fun reorderFavorites(from: Int, to: Int) {
        val list = _favorites.value.toMutableList()
        if (from == to || from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        _favorites.value = list
        prefs.edit().putString(KEY_FAVORITES, encodeSongs(list)).apply()
    }

    fun toggleFavoriteWithToast(context: Context, song: Song) {
        val added = toggleFavorite(song)
        Toast.makeText(
            context,
            if (added) "已添加到收藏，可在歌单页查看" else "已取消收藏",
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun createPlaylist(name: String): Playlist {
        val title = name.trim().ifBlank { "新建歌单" }
        val playlist = Playlist(id = UUID.randomUUID().toString(), name = title)
        val list = _playlists.value + playlist
        _playlists.value = list
        persistPlaylists(list)
        return playlist
    }

    fun renamePlaylist(id: String, name: String) {
        val list = _playlists.value.map {
            if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it
        }
        _playlists.value = list
        persistPlaylists(list)
    }

    fun deletePlaylist(id: String) {
        val list = _playlists.value.filterNot { it.id == id }
        _playlists.value = list
        persistPlaylists(list)
    }

    fun addSongToPlaylist(playlistId: String, song: Song): Boolean {
        var added = false
        val list = _playlists.value.map { p ->
            if (p.id == playlistId && p.songs.none { it.id == song.id }) {
                added = true
                p.copy(songs = p.songs + song)
            } else p
        }
        if (added) {
            _playlists.value = list
            persistPlaylists(list)
        }
        return added
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val list = _playlists.value.map { p ->
            if (p.id == playlistId) p.copy(songs = p.songs.filterNot { it.id == songId }) else p
        }
        _playlists.value = list
        persistPlaylists(list)
    }

    fun reorderPlaylists(from: Int, to: Int) {
        val list = _playlists.value.toMutableList()
        if (from == to || from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        _playlists.value = list
        persistPlaylists(list)
    }

    fun reorderPlaylistSongs(playlistId: String, from: Int, to: Int) {
        if (from == to) return
        var changed = false
        val list = _playlists.value.map { p ->
            val songs = p.songs.toMutableList()
            if (p.id == playlistId && from in songs.indices && to in songs.indices) {
                songs.add(to, songs.removeAt(from))
                changed = true
                p.copy(songs = songs)
            } else p
        }
        if (!changed) return
        _playlists.value = list
        persistPlaylists(list)
    }

    fun playlist(id: String): Playlist? = _playlists.value.firstOrNull { it.id == id }

    private fun persistPlaylists(list: List<Playlist>) {
        prefs.edit().putString(KEY_PLAYLISTS, encodePlaylists(list)).apply()
    }

    private fun encodeSongs(list: List<Song>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    private fun decodeSongs(raw: String?): List<Song> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { songFromJson(it) }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodePlaylists(list: List<Playlist>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("createdAt", p.createdAt)
                    put("songs", JSONArray().also { a -> p.songs.forEach { a.put(it.toJson()) } })
                }
            )
        }
        return arr.toString()
    }

    private fun decodePlaylists(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val songsArr = o.optJSONArray("songs")
                val songs = if (songsArr == null) emptyList() else
                    (0 until songsArr.length()).mapNotNull { j ->
                        songsArr.optJSONObject(j)?.let { songFromJson(it) }
                    }
                Playlist(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    songs = songs,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }.getOrDefault(emptyList())
    }
}

internal fun Song.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("artists", JSONArray().also { a -> artists.forEach { a.put(it) } })
    put("album", album)
    put("picId", picId)
    put("lyricId", lyricId)
    put("duration", durationMs)
    put("source", source)
    put("cover", cover)
    put("localUri", localUri)
}

internal fun songFromJson(o: JSONObject): Song {
    val artistsArr = o.optJSONArray("artists")
    val artists = if (artistsArr == null) emptyList() else
        (0 until artistsArr.length()).mapNotNull { artistsArr.optString(it).ifBlank { null } }
    return Song(
        id = o.optString("id"),
        name = o.optString("name"),
        artists = artists,
        album = o.optString("album"),
        picId = o.optString("picId"),
        lyricId = o.optString("lyricId"),
        durationMs = o.optLong("duration", 0L),
        source = o.optString("source", "netease").ifBlank { "netease" },
        cover = o.optString("cover"),
        localUri = o.optString("localUri"),
    )
}
