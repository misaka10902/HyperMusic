package com.hyper.music.music

data class Song(
    val id: String,
    val name: String,
    val artists: List<String>,
    val album: String,
    val picId: String,
    val lyricId: String,
    val durationMs: Long = 0L,
    val source: String = "netease",
    val cover: String = "",

    val localUri: String = "",
) {
    val isLocal: Boolean get() = localUri.isNotBlank()
    val artistText: String get() = artists.joinToString(" / ").ifBlank { "未知艺术家" }
    val albumText: String get() = album.ifBlank { "未知专辑" }
    val subtitle: String get() = "$artistText · $albumText"
}

data class StreamInfo(
    val url: String,
    val durationMs: Long = 0L,
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

enum class AudioQuality(val br: Int, val label: String, val bitrateText: String) {
    STANDARD(128, "标准", "128Kbps"),
    HIGH(192, "高品", "192Kbps"),
    HIGHER(320, "极高", "320Kbps"),
    LOSSLESS(999, "无损", "999Kbps"),
}

data class MusicSettings(
    val apiBase: String = DEFAULT_API_BASE,
    val quality: AudioQuality = AudioQuality.HIGH,
    val volume: Float = 1f,
    val coverCornerRadius: Float = 12f,

    val coverCircular: Boolean = true,
    val lyricCentered: Boolean = true,
    val lyricFontSize: Float = 20f,

    val lyricFontWeight: Int = 500,
    val titleCentered: Boolean = true,
    val showLyricsTranslation: Boolean = true,
    val advancedMaterial: Boolean = true,
    val audioFade: Boolean = true,
    val liveLyric: Boolean = false,
    val audioFocus: Boolean = false,
    val downloadQuality: AudioQuality = AudioQuality.STANDARD,
    val downloadMetadata: Boolean = true,
    val downloadPath: String = DEFAULT_DOWNLOAD_PATH,
    val downloadTreeUri: String = "",
) {
    companion object {
        const val DEFAULT_API_BASE = "https://music-api.gdstudio.xyz/api.php"
        const val DEFAULT_DOWNLOAD_PATH = "/storage/emulated/0/Music/"
    }
}
