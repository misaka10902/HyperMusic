package com.hyper.music.music

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoverLoader {

    private val memory = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            value.width * value.height * 4
    }



    private val pinnedCache = ConcurrentHashMap<String, ImageBitmap>()

    private val resolved = ConcurrentHashMap<String, String>()

    fun cached(url: String, maxSize: Int = 720, pinned: Boolean = false): ImageBitmap? =
        if (pinned) pinnedCache[key(url, maxSize)] else memory.get(key(url, maxSize))

    fun peekResolved(picId: String, size: Int, source: String = "netease"): String? =
        resolved["$source@$picId@$size"]

    suspend fun load(url: String, maxSize: Int = 720, pinned: Boolean = false): ImageBitmap? {
        if (url.isBlank()) return null
        val k = key(url, maxSize)
        if (pinned) pinnedCache[k]?.let { return it }
        memory.get(k)?.let { return it }
        return withContext(Dispatchers.IO) {
            if (pinned) pinnedCache[k]?.let { return@withContext it }
            memory.get(k)?.let { return@withContext it }
            val target = resolveApiPic(url) ?: url
            download(target, maxSize)?.also {
                if (pinned) pinnedCache[k] = it else memory.put(k, it)
            }
        }
    }


    suspend fun pin(url: String, maxSize: Int = CoverSize.MEDIUM.decode): ImageBitmap? =
        load(url, maxSize, pinned = true)


    fun clearPinned() {
        pinnedCache.clear()
    }

    private fun key(url: String, maxSize: Int): String = "$url@$maxSize"

    private suspend fun resolveApiPic(url: String): String? {
        if (!url.contains("types=pic", ignoreCase = true)) return null
        val uri = Uri.parse(url)
        val picId = uri.getQueryParameter("id")
        if (picId.isNullOrBlank()) return null
        val size = uri.getQueryParameter("size")?.toIntOrNull() ?: 300
        val source = uri.getQueryParameter("source") ?: "netease"
        val key = "$source@$picId@$size"
        resolved[key]?.let { return it }

        val real = runCatching { ApiBridge.resolvePic(picId, size, source) }.getOrNull()
        if (!real.isNullOrBlank()) resolved[key] = real
        return real
    }

    private fun download(url: String, maxSize: Int): ImageBitmap? {

        if (url.startsWith("file://") || url.startsWith("content://") || url.startsWith("/")) {
            return decodeLocal(url, maxSize)
        }
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            }
            if (conn.responseCode !in 200..299) return null
            val bytes = conn.inputStream.use { it.readBytes() }
            decodeBytes(bytes, maxSize)
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun decodeLocal(url: String, maxSize: Int): ImageBitmap? {
        val bytes = runCatching {
            val app = com.hyper.music.HyperApp.instance
            when {
                url.startsWith("content://") ->
                    app.contentResolver.openInputStream(Uri.parse(url))?.use { it.readBytes() }
                url.startsWith("file://") ->
                    java.io.File(Uri.parse(url).path.orEmpty()).readBytes()
                else -> java.io.File(url).readBytes()
            }
        }.getOrNull() ?: return null
        return decodeBytes(bytes, maxSize)
    }

    private fun decodeBytes(bytes: ByteArray, maxSize: Int): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSize) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: return null
        return bmp.asImageBitmap()
    }
}

enum class CoverSize(val request: Int, val decode: Int) {
    SMALL(300, 260),
    MEDIUM(700, 620),
    LARGE(1200, 1024),
}

@Composable
fun NetCover(
    url: String?,
    modifier: Modifier = Modifier,
    corner: Dp? = null,
    contentDescription: String? = null,
    maxSize: Int = CoverSize.MEDIUM.decode,
    pinned: Boolean = false,
) {
    val settings by MusicLibrary.settings.collectAsState()
    val radius = corner ?: settings.coverCornerRadius.dp



    val cachedInitial = remember(url, maxSize, pinned) {
        url?.let { CoverLoader.cached(it, maxSize, pinned) }
    }
    var image by remember(url, maxSize, pinned) { mutableStateOf(cachedInitial) }
    val fadeFromCache = cachedInitial != null
    val coverAlpha = remember(url, maxSize, pinned) { Animatable(if (fadeFromCache) 1f else 0f) }
    LaunchedEffect(url, maxSize, pinned) {
        if (image == null) {
            val loaded = url?.let { CoverLoader.load(it, maxSize, pinned) }
            if (loaded != null) {
                image = loaded
                coverAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.linearGradient(
                    listOf(
                        androidx.compose.ui.graphics.Color(0xFF2A2A2E),
                        androidx.compose.ui.graphics.Color(0xFF3A3A40),
                    )
                )
            ),
    ) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha.value },
                contentScale = ContentScale.Crop,
            )
        }
    }
}
