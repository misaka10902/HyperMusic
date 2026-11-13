package com.hyper.music.music

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SearchArtist(
    val id: String,
    val name: String,
    val avatar: String,
)

data class SearchAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val cover: String,
)

object GdApi {

    internal const val SOURCE = "netease"

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private fun base(): String = MusicLibrary.settings.value.apiBase

    suspend fun search(keyword: String, count: Int = 30, page: Int = 1): List<Song> {
        if (keyword.isBlank()) return emptyList()
        val official = runCatching { NeteaseApi.search(keyword, count, page) }
            .getOrDefault(emptyList())
        val list = if (official.isNotEmpty()) official else ApiBridge.search(keyword, count, page)
        return list.distinctBy { it.id }
    }

    suspend fun searchArtists(keyword: String, limit: Int = 20): List<SearchArtist> =
        NeteaseApi.searchArtists(keyword, limit)

    suspend fun searchAlbums(keyword: String, limit: Int = 20): List<SearchAlbum> =
        NeteaseApi.searchAlbums(keyword, limit)

    fun streamEndpoint(id: String, br: Int, source: String = SOURCE): String =
        "${base()}?types=url&source=$source&id=$id&br=$br"

    suspend fun artistSongs(artistName: String, count: Int = 30, page: Int = 1): List<Song> {
        val keyword = artistName.trim()
        if (keyword.isBlank()) return emptyList()
        val all = search(keyword, count, page)
        val filtered = all.filter { s -> s.artists.any { it.equals(keyword, ignoreCase = true) } }
        return filtered.ifEmpty { all }
    }

    suspend fun albumSongs(
        albumName: String,
        count: Int = 30,
        page: Int = 1,
        artistName: String = "",
    ): List<Song> {
        val keyword = albumName.trim()
        if (keyword.isBlank()) return emptyList()
        val artist = artistName.trim()

        runCatching {
            val albumId = NeteaseApi.searchAlbumId(keyword, artist)
            if (albumId != null) NeteaseApi.albumDetail(albumId) else emptyList()
        }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }

        if (artist.isNotEmpty() && !artist.startsWith("未知")) {
            val combined = search("$artist $keyword", count, page)
            val matched = combined.filter { it.album.equals(keyword, ignoreCase = true) }
            if (matched.isNotEmpty()) return matched
        }

        val all = search(keyword, count, page)
        val filtered = all.filter { it.album.equals(keyword, ignoreCase = true) }
        return filtered.ifEmpty { all }
    }

    suspend fun resolveStream(song: Song, br: Int): StreamInfo? =
        ApiBridge.resolveUrl(song.id, br, song.source)

    suspend fun fullLyric(song: Song): LyricResult? {
        if (song.lyricId.isBlank()) return null
        runCatching { NeteaseApi.fullLyric(song.lyricId) }.getOrNull()?.let { return it }
        return nativeFullLyric(song.lyricId, song.source)
            ?: ApiBridge.lyric(song.lyricId, song.source)?.let { LyricResult(it, "") }
    }

    fun picUrl(picId: String, size: Int = 300, source: String = SOURCE): String {
        if (picId.isBlank()) return ""
        return "${base()}?types=pic&source=$source&id=$picId&size=$size"
    }

    suspend fun artistAvatar(name: String): String = NeteaseApi.artistAvatar(name)

    fun coverUrl(song: Song, size: Int = 300): String {
        if (song.cover.isNotBlank()) {

            if (song.cover.startsWith("file://") ||
                song.cover.startsWith("content://") ||
                song.cover.startsWith("/")
            ) return song.cover
            if (song.cover.contains("param=", ignoreCase = true)) return song.cover
            val sep = if (song.cover.contains("?")) "&" else "?"
            return "${song.cover}${sep}param=${size}y${size}"
        }

        if (song.isLocal) return ""
        return picUrl(song.picId, size, song.source)
    }

    fun shareUrl(song: Song): String = "https://music.163.com/song?id=${song.id}"

    suspend fun comments(song: Song, cursor: Long, pageNo: Int, pageSize: Int): CommentPage =
        if (song.isLocal || song.id.isBlank()) CommentPage()
        else NeteaseApi.comments(song.id, cursor, pageNo, pageSize)

    internal fun parseSongsJson(body: String): List<Song> = runCatching {
        val arr = JSONArray(body)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id").ifBlank { return@mapNotNull null }
            val artistsArr = o.optJSONArray("artist")
            val artists = if (artistsArr == null) emptyList() else
                (0 until artistsArr.length()).mapNotNull { k ->
                    artistsArr.optString(k).ifBlank { null }
                }
            Song(
                id = id,
                name = o.optString("name").ifBlank { "未知歌曲" },
                artists = artists,
                album = o.optString("album"),
                picId = o.optString("pic_id"),
                lyricId = o.optString("lyric_id").ifBlank { id },
                source = o.optString("source", "netease").ifBlank { "netease" },
            )
        }
    }.getOrDefault(emptyList())

    internal suspend fun nativeSearch(keyword: String, count: Int = 30, page: Int = 1): List<Song> =
        withContext(Dispatchers.IO) {
            val url = buildString {
                append(base())
                append("?types=search&source=").append(SOURCE).append("&name=")
                append(URLEncoder.encode(keyword.trim(), "UTF-8"))
                append("&count=").append(count)
                append("&pages=").append(page)
            }
            val body = getWithRetry(url) ?: return@withContext emptyList()
            parseSongsJson(body).map { if (it.source == SOURCE) it else it.copy(source = SOURCE) }
        }

    internal suspend fun nativeResolveUrl(
        id: String,
        br: Int,
        source: String = SOURCE,
        timeoutMs: Int = 20_000,
        retries: Int = 3,
    ): StreamInfo? =
        withContext(Dispatchers.IO) {
            val url = "${base()}?types=url&source=$source&id=$id&br=$br"
            val body = getWithRetry(url, timeoutMs = timeoutMs, retries = retries)
                ?: return@withContext null
            runCatching {
                val o = JSONObject(body)
                val stream = o.optString("url")
                if (stream.isBlank() || stream == "null") return@runCatching null
                val size = o.optLong("size", 0L)
                val actualBr = o.optInt("br", br).takeIf { it > 0 } ?: br
                val durationMs = if (size > 0) (size * 8L / actualBr) * 1000L else 0L
                StreamInfo(stream, durationMs)
            }.getOrNull()
        }

    internal suspend fun nativeLyric(id: String, source: String = SOURCE): String? = withContext(Dispatchers.IO) {
        val url = "${base()}?types=lyric&source=$source&id=$id"
        val body = getWithRetry(url) ?: return@withContext null
        runCatching { JSONObject(body).optString("lyric").ifBlank { null } }.getOrNull()
    }

    internal suspend fun nativeFullLyric(id: String, source: String = SOURCE): LyricResult? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        val url = "${base()}?types=lyric&source=$source&id=$id"
        val body = getWithRetry(url, timeoutMs = 20_000) ?: return@withContext null
        runCatching {
            val o = JSONObject(body)
            val lyric = o.optString("lyric").orEmpty()
            if (lyric.isBlank()) null else LyricResult(lyric, o.optString("tlyric").orEmpty())
        }.getOrNull()
    }

    internal suspend fun nativePic(picId: String, size: Int = 500, source: String = SOURCE): String? =
        withContext(Dispatchers.IO) {
            if (picId.isBlank()) return@withContext null
            val url = "${base()}?types=pic&source=$source&id=$picId&size=$size"
            val body = getWithRetry(url) ?: return@withContext null
            runCatching { JSONObject(body).optString("url").ifBlank { null } }.getOrNull()
        }

    internal fun getWithRetry(rawUrl: String, timeoutMs: Int = 12_000, retries: Int = 3): String? {
        var last: String? = null
        repeat(retries) { attempt ->
            val body = get(rawUrl, timeoutMs)
            if (body != null) {
                val t = body.trimStart()
                if (t.startsWith("{") || t.startsWith("[")) return body
                last = body
            }
            if (attempt < retries - 1) Thread.sleep((300L * (attempt + 1)))
        }
        return null
    }

    internal fun get(rawUrl: String, timeoutMs: Int = 12_000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/json, text/plain, */*")
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            if (code !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
