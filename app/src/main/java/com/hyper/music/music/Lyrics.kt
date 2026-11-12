
package com.hyper.music.music


data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String = "",
)


data class LyricResult(
    val lyric: String,
    val translation: String,
)

private val TIME_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")


private fun parseTime(text: String): Long? {
    val m = TIME_TAG.matchAt(text, 0) ?: return null
    val minute = m.groupValues[1].toLongOrNull() ?: return null
    val second = m.groupValues[2].toLongOrNull() ?: return null

    val fractionRaw = m.groupValues[3]
    val fraction = if (fractionRaw.isEmpty()) 0L else {
        val v = fractionRaw.toLongOrNull() ?: 0L
        if (fractionRaw.length <= 2) v * (10L.pow(3 - fractionRaw.length)) else v
    }
    return minute * 60_000L + second * 1_000L + fraction
}


private fun stripTags(line: String): String =
    TIME_TAG.replace(line, "").trim()


private fun parseLrc(raw: String?): Map<Long, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    val result = LinkedHashMap<Long, String>()
    raw.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEach

        val first = parseTime(line) ?: return@forEach
        val text = stripTags(line)
        val times = TIME_TAG.findAll(line).mapNotNull { m ->
            val minute = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val second = m.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            val fractionRaw = m.groupValues[3]
            val fraction = if (fractionRaw.isEmpty()) 0L else {
                val v = fractionRaw.toLongOrNull() ?: 0L
                if (fractionRaw.length <= 2) v * (10L.pow(3 - fractionRaw.length)) else v
            }
            minute * 60_000L + second * 1_000L + fraction
        }.toList()
        val keys = times.ifEmpty { listOf(first) }
        keys.forEach { t -> result[t] = text }
    }
    return result
}


fun buildLyrics(raw: String?, translationRaw: String?): List<LyricLine> {
    val main = parseLrc(raw)
    if (main.isEmpty()) return emptyList()
    val trans = parseLrc(translationRaw)
    return main.entries
        .sortedBy { it.key }
        .map { (time, text) -> LyricLine(time, text, trans[time].orEmpty()) }


        .filter { it.text.isNotBlank() || it.translation.isNotBlank() }
}


fun currentLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var index = -1

    for (i in lines.indices) {
        if (lines[i].timeMs <= positionMs + 120L) index = i else break
    }
    return if (index < 0) 0 else index
}

private fun Long.pow(n: Int): Long {
    var result = 1L
    repeat(n.coerceAtLeast(0)) { result *= 10L }
    return result
}
