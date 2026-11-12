package com.hyper.music.music

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest

class ArtworkProvider : ContentProvider() {

    internal companion object {

        private const val AUTHORITY = "com.hyper.music.artwork"
        private const val DIR = "hyper_artwork"
        private const val UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

        private val failed = ConcurrentHashMap.newKeySet<String>()

        fun artworkUri(picId: String, size: Int = 1200, source: String = "netease"): Uri? {
            if (picId.isBlank()) return null
            val builder = Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendQueryParameter("size", size.toString())
                .appendQueryParameter("source", source)
            if (picId.startsWith("http", ignoreCase = true)) {
                builder.appendPath("direct")
                builder.appendQueryParameter("url", picId)
            } else {
                builder.appendPath(Uri.encode(picId))
            }
            return builder.build()
        }

        fun artworkUri(song: Song, size: Int = 1200): Uri? {
            if (song.cover.isNotBlank()) return artworkUri(song.cover, size, song.source)
            if (song.picId.isBlank()) return null
            return artworkUri(song.picId, size, song.source)
        }

        private fun fileDir(context: android.content.Context): File =
            File(context.cacheDir, DIR).apply { if (!exists()) mkdirs() }

        private const val MAX_FILES = 400
        private const val MAX_BYTES = 120L * 1024 * 1024

        private fun enforceBudget(context: android.content.Context) {
            val dir = fileDir(context)
            val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
            if (files.isEmpty()) return
            var count = files.size
            var total = files.sumOf { it.length() }
            if (count <= MAX_FILES && total <= MAX_BYTES) return

            for (f in files) {
                if (count - 1 >= MAX_FILES || total - f.length() >= MAX_BYTES) {
                    total -= f.length()
                    count -= 1
                    f.delete()
                } else break
            }
        }

        private fun cacheKey(picId: String, size: Int): String {
            val digest = MessageDigest.getInstance("MD5")
                .digest("$picId@$size".toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        private fun withSize(url: String, size: Int): String {
            if (!url.startsWith("http", ignoreCase = true)) return url
            if (url.contains("param=", ignoreCase = true)) return url
            val sep = if (url.contains("?")) "&" else "?"
            return "$url${sep}param=${size}y${size}"
        }

        private fun download(url: String, timeoutMs: Int = 15_000): ByteArray? {
            var conn: HttpURLConnection? = null
            return try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", UA)
                }
                if (conn.responseCode !in 200..299) return null
                conn.inputStream.use { it.readBytes() }
            } catch (_: Throwable) {
                null
            } finally {
                conn?.disconnect()
            }
        }

        private fun materialize(
            context: android.content.Context,
            picId: String,
            size: Int,
            source: String = "netease",
        ): File? {
            if (picId.isBlank()) return null

            val key = cacheKey(picId, size)
            val failKey = "$source@$key"
            if (failed.contains(failKey)) return null

            val target = File(fileDir(context), "$source@$key")
            if (target.exists() && target.length() > 0L) return target

            enforceBudget(context)

            if (picId.startsWith("http", ignoreCase = true)) {
                val directBytes = download(withSize(picId, size))?.takeIf { it.size >= 1024 }
                if (directBytes == null) {
                    failed.add(failKey)
                    return null
                }
                return runCatching {
                    target.writeBytes(directBytes)
                    target
                }.getOrNull()
            }

            val direct = runBlocking { runCatching { GdApi.nativePic(picId, size, source) }.getOrNull() }
                ?: CoverLoader.peekResolved(picId, size, source)
                ?: runBlocking { runCatching { ApiBridge.resolvePic(picId, size, source) }.getOrNull() }

            val bytes = direct?.let { download(it) }?.takeIf { it.size >= 1024 }
            if (bytes == null) {
                failed.add(failKey)
                return null
            }
            return runCatching {
                target.writeBytes(bytes)
                target
            }.getOrNull()
        }
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "image/*"

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String?>?,
    ): Int = 0

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        if (mode != "r") return null
        val context = context ?: return null
        val picId = uri.getQueryParameter("url")
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.takeIf { it.isNotBlank() && it != "direct" }
            ?: return null
        val size = uri.getQueryParameter("size")?.toIntOrNull()?.coerceIn(128, 2048) ?: 1200
        val source = uri.getQueryParameter("source") ?: "netease"
        val file = materialize(context, picId, size, source) ?: return null
        val pfd = runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }.getOrNull() ?: return null
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? =
        openAssetFile(uri, mode)?.parcelFileDescriptor
}
