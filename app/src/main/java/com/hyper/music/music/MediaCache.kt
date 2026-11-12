package com.hyper.music.music

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.IOException
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking

object MediaCache {

    private const val DIR_NAME = "hyper_media_cache"

    private const val URL_PREFS = "hyper_stream_url_cache"

    private const val MAX_BYTES = 512L * 1024 * 1024

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    @Volatile private var cache: SimpleCache? = null
    @Volatile private var appContext: Context? = null

    private fun ensureContext(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun urlPrefs(): SharedPreferences? =
        appContext?.getSharedPreferences(URL_PREFS, Context.MODE_PRIVATE)

    @Synchronized
    private fun cache(context: Context): SimpleCache {
        cache?.let { return it }
        val dir = File(context.applicationContext.cacheDir, DIR_NAME)
        val created = SimpleCache(
            dir,
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext),
        )
        cache = created
        return created
    }

    fun dataSourceFactory(context: Context): DataSource.Factory {
        ensureContext(context)
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)

        val upstream = DefaultDataSource.Factory(
            context.applicationContext,
            GdStreamDataSource.Factory(http),
        )

        val cached = CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return LocalAwareDataSource.Factory(cached, upstream)
    }

    fun usageBytes(context: Context): Long {
        ensureContext(context)
        return runCatching { cache(context).cacheSpace }.getOrDefault(0L)
    }

    fun removeByLocalUris(context: Context, uris: Collection<String>) {
        if (uris.isEmpty()) return
        ensureContext(context)
        runCatching {
            val c = cache(context)
            c.keys.toList().forEach { key ->
                if (key in uris) c.removeResource(key)
            }
        }
    }

    fun clear(context: Context) {
        ensureContext(context)

        runCatching {
            val c = cache(context)
            c.keys.toList().forEach { key -> c.removeResource(key) }
        }
        StreamUrlCache.invalidate()
    }

    private class GdStreamDataSource(
        private val upstream: DataSource,
    ) : DataSource by upstream {

        override fun open(dataSpec: DataSpec): Long {
            val uri = dataSpec.uri
            if (!uri.toString().contains("types=url", ignoreCase = true)) {
                return upstream.open(dataSpec)
            }
            val id = uri.getQueryParameter("id")
            val br = uri.getQueryParameter("br")?.toIntOrNull() ?: 320
            val source = uri.getQueryParameter("source") ?: GdApi.SOURCE
            if (id.isNullOrBlank()) return upstream.open(dataSpec)

            fun openResolved(real: String) =
                upstream.open(dataSpec.buildUpon().setUri(Uri.parse(real)).build())

            val real = resolveWithFallback(id, br, source)
            if (real.isNullOrBlank()) return upstream.open(dataSpec)
            return try {
                openResolved(real)
            } catch (e: Exception) {

                StreamUrlCache.invalidateOne(id, br, source)
                val fresh = resolveWithFallback(id, br, source)
                if (fresh.isNullOrBlank()) throw e
                openResolved(fresh)
            }
        }

        class Factory(private val upstreamFactory: DataSource.Factory) : DataSource.Factory {
            override fun createDataSource(): DataSource =
                GdStreamDataSource(upstreamFactory.createDataSource())
        }
    }

    private class LocalAwareDataSource(
        private val cachedFactory: DataSource.Factory,
        private val directFactory: DataSource.Factory,
    ) : DataSource {

        private var delegate: DataSource? = null
        private var transferListener: TransferListener? = null

        override fun addTransferListener(listener: TransferListener) {
            transferListener = listener
            delegate?.addTransferListener(listener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val scheme = dataSpec.uri.scheme?.lowercase()
            val factory = if (scheme == "file" || scheme == "content") directFactory else cachedFactory
            val ds = factory.createDataSource()
            transferListener?.let { ds.addTransferListener(it) }
            delegate = ds
            return ds.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate?.read(buffer, offset, length)
                ?: throw IOException("DataSource 尚未打开")

        override fun getUri(): Uri = delegate?.uri ?: Uri.EMPTY

        override fun getResponseHeaders(): Map<String, List<String>> =
            delegate?.responseHeaders ?: emptyMap()

        override fun close() {
            delegate?.close()
            delegate = null
        }

        class Factory(
            private val cachedFactory: DataSource.Factory,
            private val directFactory: DataSource.Factory,
        ) : DataSource.Factory {
            override fun createDataSource(): DataSource =
                LocalAwareDataSource(cachedFactory, directFactory)
        }
    }

    private fun resolveBlocking(id: String, br: Int, source: String = GdApi.SOURCE): String? {
        StreamUrlCache.get(id, br, source)?.let { return it }
        val url = runBlocking {
            runCatching { GdApi.nativeResolveUrl(id, br, source, timeoutMs = 8_000, retries = 2)?.url }
                .getOrNull()
        } ?: runBlocking { runCatching { ApiBridge.resolveUrl(id, br, source)?.url }.getOrNull() }
        if (!url.isNullOrBlank()) StreamUrlCache.put(id, br, source, url)
        return url
    }

    private fun resolveWithFallback(id: String, br: Int, source: String): String? {
        resolveBlocking(id, br, source)?.let { return it }
        return if (br != 128) resolveBlocking(id, 128, source) else null
    }

    private object StreamUrlCache {

        private const val MEM_TTL_MS = 20 * 60 * 1000L
        private const val DISK_TTL_MS = 12 * 60 * 60 * 1000L
        private val map = ConcurrentHashMap<String, Entry>()
        private data class Entry(val url: String, val at: Long)

        private fun key(id: String, br: Int, source: String) = "$source@$id@$br"

        fun get(id: String, br: Int, source: String): String? {
            val k = key(id, br, source)
            val now = System.currentTimeMillis()
            val mem = map[k]
            if (mem != null) {
                if (now - mem.at < MEM_TTL_MS) return mem.url
                map.remove(k)
            }
            val prefs = urlPrefs() ?: return null
            val raw = prefs.getString(k, null) ?: return null
            val sep = raw.indexOf('|')
            if (sep <= 0) return null
            val at = raw.substring(0, sep).toLongOrNull() ?: return null
            if (now - at >= DISK_TTL_MS) {
                prefs.edit().remove(k).apply()
                return null
            }
            val url = raw.substring(sep + 1)
            map[k] = Entry(url, at)
            return url
        }

        fun put(id: String, br: Int, source: String, url: String) {
            val k = key(id, br, source)
            val now = System.currentTimeMillis()
            map[k] = Entry(url, now)
            urlPrefs()?.edit()?.putString(k, "$now|$url")?.apply()
        }

        fun invalidateOne(id: String, br: Int, source: String) {
            val k = key(id, br, source)
            map.remove(k)
            urlPrefs()?.edit()?.remove(k)?.apply()
        }

        fun invalidate() {
            map.clear()
            urlPrefs()?.edit()?.clear()?.apply()
        }
    }

    fun invalidateResolvedUrls() = StreamUrlCache.invalidate()
}
