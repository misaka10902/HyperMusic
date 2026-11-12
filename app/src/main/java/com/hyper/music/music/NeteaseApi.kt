package com.hyper.music.music
import kotlinx.coroutines.Dispatchers
import java.net.HttpURLConnection
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.withContext
import android.util.Base64
import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object NeteaseApi {

    private const val BASE = "https://music.163.com"

    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private const val NONCE = "0CoJUm6Qyw8W8jud"
    private const val PUB_KEY = "010001"
    private const val MODULUS =
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d" +
            "2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46" +
            "bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"

    private const val IV = "0102030405060708"
    private const val SECRET_ALPHABET = "012345679abcdef"

    private fun fakeIp(): String = "113.108.${Random.nextInt(256)}.${Random.nextInt(256)}"

    private fun randomSecret(size: Int): String =
        (0 until size).map { SECRET_ALPHABET[Random.nextInt(SECRET_ALPHABET.length)] }.joinToString("")

    private fun visitorCookie(): String {
        val nuid = (0 until 32).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
        return "os=pc; appver=2.9.7; mode=31; _ntes_nuid=$nuid; " +
            "_ntes_nnid3=$nuid,${System.currentTimeMillis()}; NMTID=0;"
    }

    private fun aesEncrypt(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = IvParameterSpec(IV.toByteArray(Charsets.UTF_8))
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"), iv)
        return Base64.encodeToString(
            cipher.doFinal(text.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP,
        )
    }

    private fun rsaEncrypt(secKey: String): String {
        val m = BigInteger(1, secKey.reversed().toByteArray(Charsets.UTF_8))
        val e = BigInteger(PUB_KEY, 16)
        val n = BigInteger(MODULUS, 16)
        return m.modPow(e, n).toString(16).padStart(256, '0')
    }

    private fun weapi(data: JSONObject): String {
        val secKey = randomSecret(16)
        val enc1 = aesEncrypt(data.toString(), NONCE)
        val enc2 = aesEncrypt(enc1, secKey)
        return "params=" + URLEncoder.encode(enc2, "UTF-8") + "&encSecKey=" + rsaEncrypt(secKey)
    }

    private fun post(url: String, body: String, timeoutMs: Int = 12_000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Referer", "$BASE/")
                setRequestProperty("Origin", BASE)
                setRequestProperty("X-Real-IP", fakeIp())
                setRequestProperty("X-Forwarded-For", fakeIp())
                setRequestProperty("Cookie", visitorCookie())
                setRequestProperty("Accept", "application/json, text/plain, */*")
                instanceFollowRedirects = true
            }
            conn.outputStream.bufferedWriter().use { it.write(body) }
            val code = conn.responseCode
            if (code !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    internal suspend fun search(keyword: String, count: Int = 30, page: Int = 1): List<Song> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val offset = ((page - 1) * count).coerceAtLeast(0)
            val body = "s=" + URLEncoder.encode(kw, "UTF-8") +
                "&type=1&offset=$offset&limit=$count"
            val raw = post("$BASE/api/search/pc", body) ?: return@withContext emptyList()
            parseSearch(raw)
        }

    internal suspend fun searchArtists(keyword: String, limit: Int = 20): List<SearchArtist> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val body = "s=" + URLEncoder.encode(kw, "UTF-8") +
                "&type=100&offset=0&limit=$limit"
            val raw = post("$BASE/api/search/pc", body) ?: return@withContext emptyList()
            parseArtists(raw)
        }

    private fun parseArtists(raw: String): List<SearchArtist> = runCatching {
        val result = JSONObject(raw).optJSONObject("result") ?: return@runCatching emptyList()
        val arr = result.optJSONArray("artists") ?: return@runCatching emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val a = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = a.optString("id").ifBlank { return@mapNotNull null }
            SearchArtist(
                id = id,
                name = a.optString("name").ifBlank { return@mapNotNull null },
                avatar = a.optString("picUrl").orEmpty(),
            )
        }
    }.getOrDefault(emptyList())

    internal suspend fun searchAlbums(keyword: String, limit: Int = 20): List<SearchAlbum> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val body = "s=" + URLEncoder.encode(kw, "UTF-8") +
                "&type=10&offset=0&limit=$limit"
            val raw = post("$BASE/api/search/pc", body) ?: return@withContext emptyList()
            parseAlbums(raw)
        }

    private fun parseAlbums(raw: String): List<SearchAlbum> = runCatching {
        val result = JSONObject(raw).optJSONObject("result") ?: return@runCatching emptyList()
        val arr = result.optJSONArray("albums") ?: return@runCatching emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val a = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = a.optString("id").ifBlank { return@mapNotNull null }
            val name = a.optString("name").ifBlank { return@mapNotNull null }
            val artistObj = a.optJSONObject("artist")
            val artistText = if (artistObj != null && artistObj.optString("name").isNotBlank()) {
                artistObj.optString("name")
            } else {
                val arts = a.optJSONArray("artists")
                buildString {
                    if (arts != null) {
                        for (k in 0 until arts.length()) {
                            val n = arts.optJSONObject(k)?.optString("name")?.takeIf { it.isNotBlank() }
                            if (n != null) {
                                if (isNotEmpty()) append(" / ")
                                append(n)
                            }
                        }
                    }
                }
            }
            SearchAlbum(
                id = id,
                name = name,
                artist = artistText,
                cover = a.optString("picUrl").orEmpty(),
            )
        }
    }.getOrDefault(emptyList())

    private fun parseSearch(raw: String): List<Song> = runCatching {
        val result = JSONObject(raw).optJSONObject("result") ?: return@runCatching emptyList<Song>()
        val arr = result.optJSONArray("songs") ?: return@runCatching emptyList<Song>()
        (0 until arr.length()).mapNotNull { i ->
            val s = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = s.optString("id").ifBlank { return@mapNotNull null }
            val artistsArr = s.optJSONArray("artists")
            val artists = if (artistsArr == null) emptyList() else
                (0 until artistsArr.length()).mapNotNull { k ->
                    artistsArr.optJSONObject(k)?.optString("name")?.ifBlank { null }
                }
            val albumObj = s.optJSONObject("album")
            val cover = albumObj?.optString("picUrl").orEmpty()
            val picId = albumObj?.optString("picId").orEmpty()
                .ifBlank { albumObj?.optString("pic_str").orEmpty() }
                .ifBlank { albumObj?.optString("pic").orEmpty() }
            Song(
                id = id,
                name = s.optString("name").ifBlank { "未知歌曲" },
                artists = artists,
                album = albumObj?.optString("name").orEmpty(),
                picId = picId,
                lyricId = id,
                durationMs = s.optLong("duration", 0L),
                source = "netease",
                cover = cover,
            )
        }
            .distinctBy { it.id }
    }.getOrDefault(emptyList())

    internal suspend fun searchAlbumId(albumName: String, artistName: String = ""): String? =
        withContext(Dispatchers.IO) {
            val name = albumName.trim()
            if (name.isBlank()) return@withContext null
            val artist = artistName.trim()
            val kw = if (artist.isNotBlank() && !artist.startsWith("未知")) "$artist $name" else name
            val body = "s=" + URLEncoder.encode(kw, "UTF-8") + "&type=10&limit=20&offset=0"
            val raw = post("$BASE/api/search/pc", body) ?: return@withContext null
            val arr = runCatching {
                JSONObject(raw).optJSONObject("result")?.optJSONArray("albums")
            }.getOrNull() ?: return@withContext null
            var fallback: String? = null
            for (i in 0 until arr.length()) {
                val a = arr.optJSONObject(i) ?: continue
                val id = a.optString("id").ifBlank { continue }
                if (!a.optString("name").equals(name, ignoreCase = true)) continue
                if (artist.isBlank()) return@withContext id
                val arts = a.optJSONArray("artists")
                val matched = arts != null && (0 until arts.length()).any { k ->
                    arts.optJSONObject(k)?.optString("name")
                        ?.equals(artist, ignoreCase = true) == true
                }
                if (matched) return@withContext id
                if (fallback == null) fallback = id
            }
            fallback
        }

    internal suspend fun albumDetail(albumId: String): List<Song> = withContext(Dispatchers.IO) {
        if (albumId.isBlank()) return@withContext emptyList()
        val raw = post("$BASE/weapi/v1/album/$albumId", weapi(JSONObject().put("id", albumId)))
            ?: return@withContext emptyList()
        runCatching {
            val arr = JSONObject(raw).optJSONArray("songs")
                ?: return@runCatching emptyList<Song>()
            (0 until arr.length()).mapNotNull { i ->
                val s = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = s.optString("id").ifBlank { return@mapNotNull null }
                val artistsArr = s.optJSONArray("ar") ?: s.optJSONArray("artists")
                val artists = if (artistsArr == null) emptyList() else
                    (0 until artistsArr.length()).mapNotNull { k ->
                        artistsArr.optJSONObject(k)?.optString("name")?.ifBlank { null }
                    }
                val albumObj = s.optJSONObject("al") ?: s.optJSONObject("album")
                val picId = albumObj?.optString("picId").orEmpty()
                    .ifBlank { albumObj?.optString("pic_str").orEmpty() }
                    .ifBlank { albumObj?.optString("pic").orEmpty() }
                    .ifBlank { albumObj?.optString("id").orEmpty() }
                Song(
                    id = id,
                    name = s.optString("name").ifBlank { "未知歌曲" },
                    artists = artists,
                    album = albumObj?.optString("name").orEmpty(),
                    picId = picId,
                    lyricId = id,
                    durationMs = s.optLong("dt", 0L).takeIf { it > 0 } ?: s.optLong("duration", 0L),
                    source = "netease",
                    cover = albumObj?.optString("picUrl").orEmpty(),
                )
            }.distinctBy { it.id }
        }.getOrDefault(emptyList())
    }

    internal suspend fun playlistTracks(id: String): Pair<String, List<Song>> = withContext(Dispatchers.IO) {
        val pid = id.trim()
        if (pid.isBlank()) return@withContext "" to emptyList()
        val url = "$BASE/api/v6/playlist/detail?id=" + URLEncoder.encode(pid, "UTF-8") + "&n=1000"
        val raw = getJson(url, timeoutMs = 20_000) ?: return@withContext "" to emptyList()
        runCatching {
            val playlist = JSONObject(raw).optJSONObject("playlist")
                ?: return@runCatching "" to emptyList<Song>()
            val name = playlist.optString("name")
            name to parseTrackArray(playlist.optJSONArray("tracks"))
        }.getOrDefault("" to emptyList())
    }

    private fun parseTrackArray(arr: JSONArray?): List<Song> {
        if (arr == null) return emptyList()
        return (0 until arr.length())
            .mapNotNull { i -> parseTrackObject(arr.optJSONObject(i)) }
            .distinctBy { it.id }
    }

    private fun parseTrackObject(s: JSONObject?): Song? {
        if (s == null) return null
        val id = s.optString("id").ifBlank {
            s.optLong("id", 0L).takeIf { it > 0 }?.toString().orEmpty()
        }
        if (id.isBlank()) return null
        val ar = s.optJSONArray("ar") ?: s.optJSONArray("artists")
        val artists = if (ar == null) emptyList() else
            (0 until ar.length()).mapNotNull { k ->
                ar.optJSONObject(k)?.optString("name")?.ifBlank { null }
            }
        val al = s.optJSONObject("al") ?: s.optJSONObject("album")
        val picId = al?.optString("picId").orEmpty()
            .ifBlank { al?.optString("pic_str").orEmpty() }
            .ifBlank { al?.optString("id").orEmpty() }
        return Song(
            id = id,
            name = s.optString("name").ifBlank { "未知歌曲" },
            artists = artists,
            album = al?.optString("name").orEmpty(),
            picId = picId,
            lyricId = id,
            durationMs = s.optLong("dt", 0L).takeIf { it > 0 } ?: s.optLong("duration", 0L),
            source = "netease",
            cover = al?.optString("picUrl").orEmpty(),
        )
    }

    private fun getJson(url: String, timeoutMs: Int = 15_000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Referer", "$BASE/")
                setRequestProperty("Cookie", visitorCookie())
                setRequestProperty("Accept", "application/json, text/plain, */*")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    internal suspend fun fullLyric(id: String): LyricResult? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        val body = weapi(
            JSONObject().apply {
                put("id", id)
                put("lv", -1)
                put("tv", -1)
                put("csrf_token", "")
            },
        )
        val raw = post("$BASE/weapi/song/lyric", body, timeoutMs = 15_000)
            ?: return@withContext null
        runCatching {
            val o = JSONObject(raw)
            val lyric = o.optJSONObject("lrc")?.optString("lyric").orEmpty()
            if (lyric.isBlank()) {
                null
            } else {
                LyricResult(lyric, o.optJSONObject("tlyric")?.optString("lyric").orEmpty())
            }
        }.getOrNull()
    }

    internal suspend fun artistAvatar(name: String): String = withContext(Dispatchers.IO) {
        val kw = name.trim()
        if (kw.isBlank()) return@withContext ""
        val url = "$BASE/api/search/get/web?s=" + URLEncoder.encode(kw, "UTF-8") +
            "&type=100&limit=8&offset=0"
        var conn: HttpURLConnection? = null
        val raw = try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Referer", "$BASE/")
                setRequestProperty("Cookie", visitorCookie())
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                null
            } else {
                conn.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        } ?: return@withContext ""
        runCatching {
            val artists = JSONObject(raw).optJSONObject("result")?.optJSONArray("artists")
                ?: return@runCatching ""
            for (i in 0 until artists.length()) {
                val a = artists.optJSONObject(i) ?: continue
                if (!a.optString("name").equals(kw, ignoreCase = true)) continue
                return@runCatching a.optString("picUrl")
            }
            ""
        }.getOrDefault("")
    }
}
