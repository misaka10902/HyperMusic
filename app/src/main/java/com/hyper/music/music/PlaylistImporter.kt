package com.hyper.music.music

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TrackRef(val name: String, val artist: String)

data class PlaylistImportResult(
    val name: String,
    val songs: List<Song>,
    val total: Int,
    val skipped: Int,
)

object PlaylistImporter {

    enum class Platform { NETEASE, QQ, KUGOU }

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    fun extractUrl(text: String): String {
        val raw = text.trim()
        return Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE).find(raw)?.value ?: raw
    }

    fun detect(url: String): Platform? {
        val u = url.lowercase()
        return when {
            u.contains("music.163.com") || u.contains("163cn.tv") ||
                u.contains("y.music.163.com") -> Platform.NETEASE
            u.contains("y.qq.com") || u.contains("qq.com") && u.contains("playlist") ||
                u.contains("qq.com") && u.contains("taoge") -> Platform.QQ
            u.contains("kugou.com") || u.contains("t1.kugou.com") ||
                u.contains("m.kugou.com") -> Platform.KUGOU
            else -> null
        }
    }

    fun parseNeteaseId(url: String): String? {
        Regex("""[?&]id=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""/playlist/(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        if (Regex("""^\d{5,}$""").matches(url.trim())) return url.trim()
        return null
    }

    fun parseQqId(url: String): String? {
        Regex("""playlist/(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""[?&]id=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""disstid=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        if (Regex("""^\d{5,}$""").matches(url.trim())) return url.trim()
        return null
    }

    fun parseKugouId(url: String): String? {
        Regex("""special/single/(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""[?&]specialid=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""[?&]id=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""/(?:songlist|playlist|special)/(\d+)""").find(url)?.groupValues?.getOrNull(1)
            ?.let { return it }
        if (Regex("""^\d{3,}$""").matches(url.trim())) return url.trim()
        return null
    }

    suspend fun importFromLink(
        raw: String,
        onProgress: ((done: Int, total: Int) -> Unit)? = null,
    ): PlaylistImportResult {
        val url = extractUrl(raw)
        val platform = detect(url) ?: throw IllegalArgumentException("不支持的链接")
        return when (platform) {
            Platform.NETEASE -> {
                val id = parseNeteaseId(url)
                    ?: resolveShortLink(url)?.let { parseNeteaseId(it) }
                    ?: throw IllegalArgumentException("无法从链接解析歌单 ID")
                val (name, songs) = NeteaseApi.playlistTracks(id)
                if (songs.isEmpty()) throw IllegalStateException("歌单为空或获取失败")
                PlaylistImportResult(
                    name = name.ifBlank { "添加歌单" },
                    songs = songs.distinctBy { it.id },
                    total = songs.size,
                    skipped = 0,
                )
            }
            Platform.QQ -> {
                val id = parseQqId(url) ?: throw IllegalArgumentException("无法从链接解析歌单 ID")
                val (name, refs) = fetchQq(id)
                if (refs.isEmpty()) throw IllegalStateException("歌单为空或获取失败")
                val (matched, skipped) = matchRefs(refs, onProgress)
                PlaylistImportResult(
                    name = name.ifBlank { "添加歌单" },
                    songs = matched,
                    total = refs.size,
                    skipped = skipped,
                )
            }
            Platform.KUGOU -> {

                val id = parseKugouId(url)
                    ?: resolveShortLink(url)?.let { parseKugouId(it) }
                    ?: throw IllegalArgumentException("无法从链接解析歌单 ID")
                val (name, refs) = fetchKugou(id)
                if (refs.isEmpty()) throw IllegalStateException("歌单为空或获取失败")
                val (matched, skipped) = matchRefs(refs, onProgress)
                PlaylistImportResult(
                    name = name.ifBlank { "添加歌单" },
                    songs = matched,
                    total = refs.size,
                    skipped = skipped,
                )
            }
        }
    }

    suspend fun importFromJson(
        text: String,
        onProgress: ((done: Int, total: Int) -> Unit)? = null,
    ): PlaylistImportResult {
        val trimmed = text.trim().trimStart('\uFEFF')
        if (trimmed.isBlank()) throw IllegalArgumentException("文件内容为空")

        val obj = runCatching { JSONObject(trimmed) }.getOrNull()
        if (obj != null) {
            val name = obj.optString("name").ifBlank { obj.optString("title") }.ifBlank { "添加歌单" }
            val arr = obj.optJSONArray("songs") ?: obj.optJSONArray("tracks")
                ?: throw IllegalArgumentException("JSON 中没有找到歌曲列表")
            return collect(name, arr, onProgress)
        }

        val arr = runCatching { JSONArray(trimmed) }.getOrNull()
            ?: throw IllegalArgumentException("无法识别的 JSON 格式")

        val first = arr.optJSONObject(0)
        if (first != null && (first.has("songs") || first.has("tracks"))) {
            val name = first.optString("name").ifBlank { "添加歌单" }
            val nested = first.optJSONArray("songs") ?: first.optJSONArray("tracks")
            return collect(name, nested, onProgress)
        }
        return collect("添加歌单", arr, onProgress)
    }

    private suspend fun collect(
        name: String,
        arr: JSONArray?,
        onProgress: ((Int, Int) -> Unit)?,
    ): PlaylistImportResult {
        if (arr == null || arr.length() == 0) throw IllegalArgumentException("没有可导入的歌曲")
        val direct = ArrayList<Song>()
        val refs = ArrayList<TrackRef>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").trim()
            if (id.isNotBlank()) {
                direct += songFromJson(o)
            } else {
                val n = o.optString("name").ifBlank { o.optString("title") }.trim()
                if (n.isBlank()) continue
                val a = o.optString("artist").ifBlank {
                    o.optJSONArray("artists")?.let { if (it.length() > 0) it.optString(0) else "" }.orEmpty()
                }
                refs += TrackRef(n, a)
            }
        }
        val (matched, skipped) = matchRefs(refs, onProgress)
        val songs = (direct + matched).distinctBy { it.id }
        return PlaylistImportResult(
            name = name,
            songs = songs,
            total = direct.size + refs.size,
            skipped = skipped,
        )
    }

    fun exportJson(playlist: Playlist): String = JSONObject().apply {
        put("format", "hyper-playlist")
        put("version", 1)
        put("name", playlist.name)
        put("exportedAt", System.currentTimeMillis())
        put("songs", JSONArray().also { a -> playlist.songs.forEach { a.put(it.toJson()) } })
    }.toString(2)

    private suspend fun matchRefs(
        refs: List<TrackRef>,
        onProgress: ((Int, Int) -> Unit)?,
    ): Pair<List<Song>, Int> {
        if (refs.isEmpty()) return emptyList<Song>() to 0
        val out = LinkedHashMap<String, Song>()
        var skipped = 0
        refs.forEachIndexed { index, ref ->
            val song = matchOne(ref)
            if (song != null) out.putIfAbsent(song.id, song) else skipped++
            onProgress?.invoke(index + 1, refs.size)
        }
        return out.values.toList() to skipped
    }

    private suspend fun matchOne(ref: TrackRef): Song? {
        val name = ref.name.trim()
        if (name.isBlank()) return null
        val artist = ref.artist.trim()
        val keyword = if (artist.isBlank()) name else "$name $artist"
        val results = runCatching { GdApi.search(keyword, 12) }.getOrDefault(emptyList())
        if (results.isEmpty()) return null

        fun artistMatch(s: Song): Boolean {
            if (artist.isBlank()) return false
            return s.artists.any { a ->
                a.equals(artist, ignoreCase = true) ||
                    a.contains(artist, ignoreCase = true) ||
                    artist.contains(a, ignoreCase = true)
            }
        }

        results.firstOrNull { it.name.equals(name, ignoreCase = true) && artistMatch(it) }?.let { return it }

        results.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }

        results.firstOrNull { it.name.contains(name, ignoreCase = true) && artistMatch(it) }?.let { return it }

        return results.firstOrNull { it.name.contains(name, ignoreCase = true) }
    }

    private suspend fun fetchQq(id: String): Pair<String, List<TrackRef>> = withContext(Dispatchers.IO) {
        val url = "https://i.y.qq.com/qzone-music/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg" +
            "?type=1&json=1&utf8=1&nosign=1&disstid=" + URLEncoder.encode(id, "UTF-8") +
            "&g_tk=5381&loginUin=0&hostUin=0&format=json&inCharset=GB2312&outCharset=utf-8" +
            "&notice=0&platform=yqq&needNewCode=0"
        val body = httpGet(url, referer = "https://y.qq.com/")
            ?: throw IllegalStateException("获取 QQ 音乐歌单失败，请检查网络")
        val root = JSONObject(stripJsonp(body))
        val cd = root.optJSONArray("cdlist")?.optJSONObject(0)
            ?: throw IllegalStateException("歌单不存在或已被删除")
        val name = cd.optString("dissname")
        val list = cd.optJSONArray("songlist")
        val refs = ArrayList<TrackRef>()
        if (list != null) {
            for (i in 0 until list.length()) {
                val o = list.optJSONObject(i) ?: continue
                val songName = o.optString("songname").ifBlank { o.optString("title") }.trim()
                if (songName.isBlank()) continue
                val singers = o.optJSONArray("singer")
                val artist = if (singers != null && singers.length() > 0) {
                    (0 until singers.length()).mapNotNull { k ->
                        singers.optJSONObject(k)?.optString("name")?.ifBlank { null }
                    }.joinToString("/")
                } else {
                    o.optString("singername")
                }
                refs += TrackRef(songName, artist)
            }
        }
        name to refs
    }

    private suspend fun fetchKugou(id: String): Pair<String, List<TrackRef>> =
        withContext(Dispatchers.IO) {
            val base = "https://mobiles.kugou.com/api/v3/special"
            val name = runCatching {
                val raw = httpGet(
                    "$base/info?specialid=$id",
                    referer = "https://www.kugou.com/",
                ) ?: return@runCatching ""
                JSONObject(raw).optJSONObject("data")?.optString("specialname").orEmpty()
            }.getOrDefault("")

            val refs = ArrayList<TrackRef>()
            var page = 1
            var total = Int.MAX_VALUE
            val pageSize = 100
            while (refs.size < total && page <= 30) {
                val raw = httpGet(
                    "$base/song?specialid=$id&page=$page&pagesize=$pageSize",
                    referer = "https://www.kugou.com/",
                ) ?: break
                val data = runCatching { JSONObject(raw).optJSONObject("data") }.getOrNull() ?: break
                total = data.optInt("total").takeIf { it > 0 } ?: 0
                val info = data.optJSONArray("info")
                if (info == null || info.length() == 0) break
                for (i in 0 until info.length()) {
                    val o = info.optJSONObject(i) ?: continue
                    val songName = o.optString("songname").ifBlank { o.optString("name") }.trim()
                    val singer = o.optString("singername").ifBlank { o.optString("singer") }.trim()
                    val filename = o.optString("filename").trim()
                    val (artist, title) = when {
                        songName.isNotBlank() -> singer to songName
                        else -> splitKugouFilename(filename)
                    }
                    if (title.isBlank()) continue
                    refs += TrackRef(title, artist)
                }
                if (info.length() < pageSize) break
                page++
            }
            name to refs
        }

    private fun splitKugouFilename(filename: String): Pair<String, String> {
        if (filename.isBlank()) return "" to ""
        val idx = filename.indexOf(" - ")
        if (idx < 0) return "" to filename
        return filename.substring(0, idx).trim() to filename.substring(idx + 3).trim()
    }

    private fun stripJsonp(text: String): String {
        val t = text.trim()
        if (t.startsWith("{")) return t
        val m = Regex("""^[\w$.]+\s*\(([\s\S]*)\)\s*;?$""").find(t)
        return m?.groupValues?.getOrNull(1) ?: t
    }

    private fun resolveShortLink(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
            }
            conn.responseCode
            conn.url?.toString()
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpGet(url: String, referer: String? = null, timeoutMs: Int = 15_000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/json, text/plain, */*")
                if (referer != null) setRequestProperty("Referer", referer)
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
