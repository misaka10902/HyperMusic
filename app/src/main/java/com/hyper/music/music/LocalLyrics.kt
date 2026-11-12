package com.hyper.music.music

import android.content.Context
import android.net.Uri
import java.io.File

object LocalLyrics {

    private const val TLYRIC_DELIMITER = "\n[TLYRIC]\n"
    private const val HEAD_LIMIT = 512 * 1024

    fun read(context: Context, song: Song): LyricResult? {
        if (!song.isLocal) return null
        val raw = readRaw(context.applicationContext, song.localUri) ?: return null
        val (main, translation) = if (raw.contains(TLYRIC_DELIMITER)) {
            val parts = raw.split(TLYRIC_DELIMITER, limit = 2)
            parts[0] to parts.getOrElse(1) { "" }
        } else {
            raw to ""
        }
        if (main.isBlank()) return null
        return LyricResult(main, translation)
    }

    private fun readRaw(app: Context, uriText: String): String? {
        val bytes = runCatching { openHead(app, uriText) }.getOrNull() ?: return null
        if (bytes.size < 10) return null
        return parseId3(bytes) ?: parseFlac(bytes)
    }

    private fun openHead(app: Context, uriText: String): ByteArray {
        val uri = Uri.parse(uriText)
        val input = when {
            uri.scheme == "file" -> File(uri.path.orEmpty()).inputStream()
            uriText.startsWith("/") -> File(uriText).inputStream()
            else -> app.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法打开 $uriText")
        }
        return input.use { stream ->
            val buffer = ByteArray(HEAD_LIMIT)
            var total = 0
            while (total < buffer.size) {
                val n = stream.read(buffer, total, buffer.size - total)
                if (n <= 0) break
                total += n
            }
            if (total == buffer.size) buffer else buffer.copyOf(total)
        }
    }

    private fun parseId3(b: ByteArray): String? {
        if (b.size < 10 || b[0] != 'I'.code.toByte() || b[1] != 'D'.code.toByte() ||
            b[2] != '3'.code.toByte()
        ) return null
        val version = b[3].toInt() and 0xFF
        val tagSize = syncSafe(b[6], b[7], b[8], b[9])
        if (tagSize <= 0) return null
        val end = minOf(10 + tagSize, b.size)
        var pos = 10
        while (pos + 10 <= end) {
            val id = String(b, pos, 4, Charsets.US_ASCII)
            val frameSize = if (version >= 4) {
                syncSafe(b[pos + 4], b[pos + 5], b[pos + 6], b[pos + 7])
            } else {
                be32(b, pos + 4)
            }
            if (frameSize <= 0 || pos + 10 + frameSize > end) break
            val dataOff = pos + 10
            if (id == "USLT") {
                decodeUslt(b, dataOff, frameSize)?.let { return it }
            }
            pos += 10 + frameSize
        }
        return null
    }

    private fun decodeUslt(b: ByteArray, off: Int, len: Int): String? {
        if (len < 4) return null
        val encoding = b[off].toInt() and 0xFF
        var p = off + 4
        val end = off + len
        when (encoding) {
            1, 2 -> {
                while (p + 1 < end) {
                    if (b[p] == 0.toByte() && b[p + 1] == 0.toByte()) {
                        p += 2
                        break
                    }
                    p += 2
                }
            }
            else -> {
                while (p < end && b[p] != 0.toByte()) p++
                p++
            }
        }
        if (p >= end) return null
        val text = when (encoding) {
            0 -> String(b, p, end - p, Charsets.ISO_8859_1)
            1 -> String(b, p, end - p, Charsets.UTF_16)
            2 -> String(b, p, end - p, Charsets.UTF_16BE)
            3 -> String(b, p, end - p, Charsets.UTF_8)
            else -> String(b, p, end - p, Charsets.UTF_8)
        }
        return text.trim('\u0000', ' ', '\n', '\r').takeIf { it.isNotBlank() }
    }

    private fun parseFlac(b: ByteArray): String? {
        if (b.size < 42 || String(b, 0, 4, Charsets.US_ASCII) != "fLaC") return null
        var pos = 4
        while (pos + 4 <= b.size) {
            val head = b[pos].toInt() and 0xFF
            val type = head and 0x7F
            val last = head and 0x80 != 0
            val len = ((b[pos + 1].toInt() and 0xFF) shl 16) or
                ((b[pos + 2].toInt() and 0xFF) shl 8) or
                (b[pos + 3].toInt() and 0xFF)
            val bodyOff = pos + 4
            if (len < 0 || bodyOff + len > b.size) break
            if (type == 4) return parseVorbisComment(b, bodyOff, bodyOff + len)
            pos = bodyOff + len
            if (last) break
        }
        return null
    }

    private fun parseVorbisComment(b: ByteArray, off: Int, end: Int): String? {
        var p = off
        if (p + 4 > end) return null
        val vendorLen = le32(b, p)
        p += 4 + vendorLen
        if (p + 4 > end) return null
        val count = le32(b, p)
        p += 4
        if (count < 0 || count > 4096) return null
        var lyrics: String? = null
        var i = 0
        while (i < count) {
            if (p + 4 > end) break
            val len = le32(b, p)
            p += 4
            if (len < 0 || p + len > end) break
            val field = String(b, p, len, Charsets.UTF_8)
            p += len
            val key = field.substringBefore('=')
            if (key.equals("UNSYNCEDLYRICS", ignoreCase = true) ||
                key.equals("LYRICS", ignoreCase = true)
            ) {
                field.substringAfter('=', "").takeIf { it.isNotBlank() }?.let { lyrics = it }
            }
            i++
        }
        return lyrics?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun syncSafe(a: Byte, b: Byte, c: Byte, d: Byte): Int =
        ((a.toInt() and 0x7F) shl 21) or
            ((b.toInt() and 0x7F) shl 14) or
            ((c.toInt() and 0x7F) shl 7) or
            (d.toInt() and 0x7F)

    private fun be32(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 24) or
            ((b[off + 1].toInt() and 0xFF) shl 16) or
            ((b[off + 2].toInt() and 0xFF) shl 8) or
            (b[off + 3].toInt() and 0xFF)

    private fun le32(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 3].toInt() and 0xFF) shl 24)
}
