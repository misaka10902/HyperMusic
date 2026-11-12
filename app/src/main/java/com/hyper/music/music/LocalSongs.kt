package com.hyper.music.music

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalSongs {

    private const val UNKNOWN = "未知"
    private const val UNKNOWN_TAG = "<unknown>"

    private val AUDIO_EXT = setOf(
        "mp3", "flac", "m4a", "aac", "ogg", "oga", "opus", "wav", "wma",
        "ape", "mp4", "m4b", "aif", "aiff", "amr", "3gp", "mka", "ac3",
    )

    private data class Entry(
        val uri: Uri,
        val displayName: String,
        val path: String,
        val size: Long,
        val durationMs: Long,
    )

    private fun isAudio(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in AUDIO_EXT

    private fun clean(value: String?): String? = value
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals(UNKNOWN_TAG, ignoreCase = true) }

    suspend fun scan(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val settings = MusicLibrary.settings.value
        val found = LinkedHashMap<String, Entry>()

        settings.downloadTreeUri.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching {
                collectFromTree(DocumentFile.fromTreeUri(app, Uri.parse(raw)), found, 0)
            }
        }

        val downloadPath = settings.downloadPath

        runCatching { collectFromMediaStore(app, downloadPath, found) }

        runCatching { collectFromPath(downloadPath, found) }

        found.values
            .mapNotNull { entry -> runCatching { buildSong(app, entry) }.getOrNull() }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun put(found: MutableMap<String, Entry>, entry: Entry) {

        val key = "${entry.displayName.lowercase()}#${entry.size}"
        found.putIfAbsent(key, entry)
    }

    private fun collectFromTree(dir: DocumentFile?, found: MutableMap<String, Entry>, depth: Int) {
        if (dir == null || depth > 3) return
        val children = runCatching { dir.listFiles() }.getOrDefault(emptyArray())
        for (doc in children) {
            if (doc.isDirectory) {
                collectFromTree(doc, found, depth + 1)
                continue
            }
            val name = doc.name ?: continue
            if (!doc.isFile || !isAudio(name)) continue
            val size = runCatching { doc.length() }.getOrDefault(0L)
            put(found, Entry(doc.uri, name, "", size, 0L))
        }
    }

    private fun collectFromPath(downloadPath: String, found: MutableMap<String, Entry>) {
        val root = runCatching { File(downloadPath.trim().trimEnd('/')) }.getOrNull() ?: return
        if (!root.isDirectory) return
        walk(root, 0, found)
    }

    private fun walk(dir: File, depth: Int, found: MutableMap<String, Entry>) {
        if (depth > 2) return
        val children = dir.listFiles() ?: return
        for (f in children) {
            if (f.isDirectory) {
                walk(f, depth + 1, found)
            } else if (f.isFile && isAudio(f.name)) {
                put(found, Entry(Uri.fromFile(f), f.name, f.absolutePath, f.length(), 0L))
            }
        }
    }

    private fun collectFromMediaStore(
        app: Context,
        downloadPath: String,
        found: MutableMap<String, Entry>,
    ) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
        )
        val cursor = app.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null,
        ) ?: return
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val relCol = c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
            val sizeCol = c.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val durCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val name = c.getString(nameCol) ?: continue
                if (!isAudio(name)) continue
                val rel = if (relCol >= 0) c.getString(relCol).orEmpty() else ""
                val data = if (dataCol >= 0) c.getString(dataCol).orEmpty() else ""
                if (!inDownloadFolder(rel, data, downloadPath)) continue
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val size = if (sizeCol >= 0) c.getLong(sizeCol) else 0L
                val duration = if (durCol >= 0) c.getLong(durCol) else 0L
                put(found, Entry(uri, name, data, size, duration))
            }
        }
    }

    private fun inDownloadFolder(rel: String, data: String, downloadPath: String): Boolean {
        val d = downloadPath.trim().trimEnd('/')
        if (data.isNotBlank() && (data == d || data.startsWith("$d/"))) return true
        val marker = "/storage/emulated/0/"
        if (d.startsWith(marker, ignoreCase = true)) {
            val relative = d.removePrefix(marker).trimEnd('/') + "/"
            if (rel.startsWith(relative, ignoreCase = true)) return true
        }

        return false
    }

    private fun buildSong(app: Context, entry: Entry): Song {
        val nameNoExt = entry.displayName.substringBeforeLast('.', entry.displayName)
            .ifBlank { entry.displayName }

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration = entry.durationMs
        var coverBytes: ByteArray? = null

        val retriever = MediaMetadataRetriever()
        try {
            if (entry.path.isNotBlank()) {
                retriever.setDataSource(entry.path)
            } else {
                retriever.setDataSource(app, entry.uri)
            }
            title = clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            artist = clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
            album = clean(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
                ?.let { duration = it }
            coverBytes = runCatching { retriever.embeddedPicture }.getOrNull()
        } catch (_: Throwable) {

        } finally {
            runCatching { retriever.release() }
        }

        val cover = coverBytes?.takeIf { it.isNotEmpty() }?.let { bytes ->
            runCatching { saveCover(app, entry.uri.toString(), bytes) }.getOrNull()
        }.orEmpty()

        val uriText = entry.uri.toString()
        return Song(
            id = "local:$uriText",
            name = title ?: nameNoExt,
            artists = listOf(artist ?: UNKNOWN),
            album = album ?: UNKNOWN,
            picId = "",
            lyricId = "",
            durationMs = duration,
            source = "local",
            cover = cover,
            localUri = uriText,
        )
    }

    fun deletePlan(songs: List<Song>): DeletePlan {
        val media = mutableListOf<Song>()
        val direct = mutableListOf<Song>()
        for (song in songs) {
            if (!song.isLocal) continue
            val uri = runCatching { Uri.parse(song.localUri) }.getOrNull() ?: continue
            if (uri.scheme.equals("content", ignoreCase = true) &&
                uri.authority.equals(MediaStore.AUTHORITY, ignoreCase = true)
            ) {
                media.add(song)
            } else {
                direct.add(song)
            }
        }
        return DeletePlan(mediaStoreSongs = media, directSongs = direct)
    }

    fun deleteDirect(context: Context, songs: List<Song>): List<Song> {
        val app = context.applicationContext
        val deleted = mutableListOf<Song>()
        for (song in songs) {
            if (!song.isLocal) continue
            val uri = runCatching { Uri.parse(song.localUri) }.getOrNull() ?: continue
            val ok = runCatching {
                when {
                    uri.scheme.equals("content", ignoreCase = true) ->

                        (DocumentFile.fromSingleUri(app, uri)?.delete() == true) ||
                            app.contentResolver.delete(uri, null, null) > 0
                    else -> File(uri.path ?: song.localUri).delete()
                }
            }.getOrDefault(false)
            if (ok) deleted.add(song)
        }
        return deleted
    }

    data class DeletePlan(
        val mediaStoreSongs: List<Song>,
        val directSongs: List<Song>,
    )

    private fun saveCover(app: Context, key: String, bytes: ByteArray): String {
        val dir = File(app.cacheDir, "local_covers").apply { mkdirs() }
        val digest = MessageDigest.getInstance("MD5")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val file = File(dir, "$digest.img")
        if (!file.exists() || file.length() != bytes.size.toLong()) {
            file.writeBytes(bytes)
        }
        return Uri.fromFile(file).toString()
    }
}
