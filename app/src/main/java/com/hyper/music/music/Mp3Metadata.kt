package com.hyper.music.music

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class AudioFormat { MP3, FLAC, OTHER }

data class DownloadMeta(
    val title: String,
    val artist: String,
    val album: String,
    val lyrics: String?,
    val coverBytes: ByteArray?,
    val coverMime: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DownloadMeta
        return title == other.title && artist == other.artist && album == other.album &&
            lyrics == other.lyrics && coverBytes.contentEquals(other.coverBytes) &&
            coverMime == other.coverMime
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        result = 31 * result + (coverBytes?.contentHashCode() ?: 0)
        result = 31 * result + coverMime.hashCode()
        return result
    }
}

suspend fun gatherDownloadMeta(song: Song): DownloadMeta {
    val coverBytes = if (song.cover.isNotBlank()) {
        runCatching { fetchBytes(song.cover) }.getOrNull()
    } else {
        null
    }

    return DownloadMeta(
        title = song.name,
        artist = song.artistText,
        album = song.albumText,
        lyrics = fetchEmbedLyric(song),
        coverBytes = coverBytes,
        coverMime = "image/jpeg",
    )
}

private const val TLYRIC_DELIMITER = "\n[TLYRIC]\n"

private suspend fun fetchEmbedLyric(song: Song): String? = runCatching {
    val target = if (song.lyricId.isBlank()) song.copy(lyricId = song.id) else song
    val result = GdApi.fullLyric(target)
    val lyric = result?.lyric.orEmpty()
    if (lyric.isBlank()) return@runCatching null
    val translation = result?.translation.orEmpty()
    if (translation.isBlank()) lyric else lyric + TLYRIC_DELIMITER + translation
}.getOrNull()

private fun fetchBytes(url: String): ByteArray? = runCatching {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
        instanceFollowRedirects = true
    }
    if (conn.responseCode !in 200..299) return null
    conn.inputStream.use { it.readBytes() }
}.getOrNull()

fun probeAudio(file: File): AudioFormat {
    val h = ByteArray(12)
    val n = runCatching { FileInputStream(file).use { it.read(h) } }.getOrDefault(-1)
    if (n < 4) return AudioFormat.OTHER
    if (h[0] == 'f'.code.toByte() && h[1] == 'L'.code.toByte() &&
        h[2] == 'a'.code.toByte() && h[3] == 'C'.code.toByte()
    ) return AudioFormat.FLAC
    if (h[0] == 'I'.code.toByte() && h[1] == 'D'.code.toByte() &&
        h[2] == '3'.code.toByte()
    ) return AudioFormat.MP3
    if ((h[0].toInt() and 0xFF) == 0xFF &&
        ((h[1].toInt() and 0xFF) and 0xE0) == 0xE0
    ) return AudioFormat.MP3
    return AudioFormat.OTHER
}

private fun sourceId3Length(file: File): Int {
    val h = ByteArray(10)
    if (FileInputStream(file).use { it.read(h) } != 10) return 0
    if (h[0] != 'I'.code.toByte() || h[1] != 'D'.code.toByte() ||
        h[2] != '3'.code.toByte()
    ) return 0
    val flags = h[5].toInt() and 0xFF
    val size = ((h[6].toInt() and 0x7F) shl 21) or
        ((h[7].toInt() and 0x7F) shl 14) or
        ((h[8].toInt() and 0x7F) shl 7) or
        (h[9].toInt() and 0x7F)
    var total = 10 + size
    if (flags and 0x10 != 0) total += 10
    return total
}

private fun skipFully(input: FileInputStream, count: Long) {
    var left = count
    while (left > 0) {
        val s = input.skip(left)
        if (s <= 0) {
            if (input.read() == -1) break else left--
        } else left -= s
    }
}

fun buildTaggedAudio(src: File, dst: File, format: AudioFormat, meta: DownloadMeta?): Boolean {
    if (meta == null) return false
    return runCatching {
        when (format) {
            AudioFormat.MP3 -> writeMp3(src, dst, meta)
            AudioFormat.FLAC -> writeFlac(src, dst, meta)
            AudioFormat.OTHER -> false
        }
    }.getOrElse {
        dst.delete()
        false
    }
}

private fun writeMp3(src: File, dst: File, meta: DownloadMeta): Boolean {
    val skip = sourceId3Length(src).toLong()
    FileOutputStream(dst).use { out ->
        out.write(buildId3v2_3(meta))
        FileInputStream(src).use { input ->
            skipFully(input, skip)
            input.copyTo(out, 64 * 1024)
        }
    }
    return true
}

private fun writeFlac(src: File, dst: File, meta: DownloadMeta): Boolean {
    val head = ByteArray(4 + 4 + 34)
    if (FileInputStream(src).use { it.read(head) } != head.size) return false
    if (head[0] != 'f'.code.toByte() || head[1] != 'L'.code.toByte() ||
        head[2] != 'a'.code.toByte() || head[3] != 'C'.code.toByte()
    ) return false
    val streamInfoHeader = head.copyOfRange(4, 8)
    if ((streamInfoHeader[0].toInt() and 0x7F) != 0) return false
    val wasLast = (streamInfoHeader[0].toInt() and 0x80) != 0

    val blocks = mutableListOf<Pair<Int, ByteArray>>()
    blocks += 4 to vorbisComment(meta)
    val cover = meta.coverBytes
    if (cover != null && cover.isNotEmpty()) blocks += 6 to pictureBlock(cover, meta.coverMime)

    FileOutputStream(dst).use { out ->
        out.write(head, 0, 4)
        streamInfoHeader[0] = (streamInfoHeader[0].toInt() and 0x7F).toByte()
        out.write(streamInfoHeader)
        out.write(head, 8, 34)
        for (i in blocks.indices) {
            val (type, body) = blocks[i]
            out.write(flacBlockHeader(type, body.size, wasLast && i == blocks.lastIndex))
            out.write(body)
        }
        FileInputStream(src).use { input ->
            skipFully(input, (4 + 4 + 34).toLong())
            input.copyTo(out, 64 * 1024)
        }
    }
    return true
}

private fun flacBlockHeader(type: Int, size: Int, last: Boolean): ByteArray {
    val first = (type and 0x7F) or (if (last) 0x80 else 0)
    return byteArrayOf(
        first.toByte(),
        ((size shr 16) and 0xFF).toByte(),
        ((size shr 8) and 0xFF).toByte(),
        (size and 0xFF).toByte(),
    )
}

private fun vorbisComment(meta: DownloadMeta): ByteArray {
    val out = ByteArrayOutputStream()
    val vendor = "HyperMusic".toByteArray(Charsets.UTF_8)
    le32(out, vendor.size)
    out.write(vendor)
    val fields = buildList {
        if (meta.title.isNotBlank()) add("TITLE=${meta.title}")
        if (meta.artist.isNotBlank()) add("ARTIST=${meta.artist}")
        if (meta.album.isNotBlank()) add("ALBUM=${meta.album}")
        if (!meta.lyrics.isNullOrBlank()) {
            add("UNSYNCEDLYRICS=${meta.lyrics}")
            add("LYRICS=${meta.lyrics}")
        }
    }
    le32(out, fields.size)
    for (f in fields) {
        val b = f.toByteArray(Charsets.UTF_8)
        le32(out, b.size)
        out.write(b)
    }
    return out.toByteArray()
}

private fun pictureBlock(image: ByteArray, mime: String): ByteArray {
    val out = ByteArrayOutputStream()
    be32(out, 3)
    val m = mime.toByteArray(Charsets.US_ASCII)
    be32(out, m.size)
    out.write(m)
    be32(out, 0)
    repeat(4) { be32(out, 0) }
    be32(out, image.size)
    out.write(image)
    return out.toByteArray()
}

private fun le32(out: ByteArrayOutputStream, v: Int) {
    out.write(v and 0xFF)
    out.write((v shr 8) and 0xFF)
    out.write((v shr 16) and 0xFF)
    out.write((v shr 24) and 0xFF)
}

private fun be32(out: ByteArrayOutputStream, v: Int) {
    out.write((v shr 24) and 0xFF)
    out.write((v shr 16) and 0xFF)
    out.write((v shr 8) and 0xFF)
    out.write(v and 0xFF)
}

fun buildId3v2_3(meta: DownloadMeta): ByteArray {
    val frames = mutableListOf<ByteArray>()
    frames += textFrame("TIT2", meta.title)
    frames += textFrame("TPE1", meta.artist)
    frames += textFrame("TALB", meta.album)
    if (!meta.lyrics.isNullOrBlank()) frames += usltFrame(meta.lyrics)
    val cover = meta.coverBytes
    if (cover != null && cover.isNotEmpty()) frames += apicFrame(cover, meta.coverMime)

    val body = frames.fold(ByteArray(0)) { acc, f -> acc + f }
    val header = byteArrayOf(
        'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(),
        3, 0, 0,
    ) + synchsafe(body.size)
    return header + body
}

private fun utf16(text: String): ByteArray =
    byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)

private fun textFrame(id: String, text: String): ByteArray {
    if (text.isBlank()) return byteArrayOf()
    return frame(id, byteArrayOf(0x01) + utf16(text))
}

private fun usltFrame(lyrics: String): ByteArray = frame(
    "USLT",
    byteArrayOf(0x01) + "zho".toByteArray(Charsets.US_ASCII) +
        utf16("Lyrics") + byteArrayOf(0x00, 0x00) + utf16(lyrics),
)

private fun apicFrame(image: ByteArray, mime: String): ByteArray = frame(
    "APIC",
    byteArrayOf(0x00) + mime.toByteArray(Charsets.US_ASCII) + 0x00.toByte() +
        0x03.toByte() + 0x00.toByte() + image,
)

private fun frame(id: String, data: ByteArray): ByteArray {
    val size = byteArrayOf(
        (data.size shr 24).toByte(),
        (data.size shr 16).toByte(),
        (data.size shr 8).toByte(),
        data.size.toByte(),
    )
    return id.toByteArray(Charsets.US_ASCII) + size + byteArrayOf(0x00, 0x00) + data
}

private fun synchsafe(value: Int): ByteArray = byteArrayOf(
    ((value shr 21) and 0x7F).toByte(),
    ((value shr 14) and 0x7F).toByte(),
    ((value shr 7) and 0x7F).toByte(),
    (value and 0x7F).toByte(),
)
