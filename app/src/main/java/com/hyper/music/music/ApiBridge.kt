package com.hyper.music.music

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

object ApiBridge {

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var webView: WebView? = null
    @Volatile private var ready = false

    private val reqCounter = AtomicInteger(0)
    private val pending = ConcurrentHashMap<String, Pending<*>>()

    private data class Pending<T>(val deferred: CompletableDeferred<T>, val type: Int)
    private const val T_SEARCH = 1
    private const val T_URL = 2
    private const val T_LYRIC = 3
    private const val T_PIC = 4

    @SuppressLint("SetJavaScriptEnabled")
    fun init(context: android.content.Context) {
        if (webView != null) return
        handler.post {
            val wv = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        ready = true
                    }
                }
                addJavascriptInterface(Bridge(), "HyperBridge")
                loadUrl("file:///android_asset/api_bridge.html")
            }
            runCatching {
                wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            runCatching { wv.resumeTimers() }
            webView = wv
        }
    }

    @Keep
    private class Bridge {
        @JavascriptInterface
        fun onReady() {
            ready = true
        }

        @JavascriptInterface
        fun onSearch(reqId: String, json: String) {
            (pending.remove(reqId) as? Pending<List<Song>>)
                ?.deferred?.complete(GdApi.parseSongsJson(json))
        }

        @JavascriptInterface
        fun onUrl(reqId: String, url: String, size: Long, br: Int) {
            (pending.remove(reqId) as? Pending<StreamInfo?>)
                ?.deferred?.complete(
                    if (url.isBlank()) null
                    else StreamInfo(url, if (size > 0) size * 8L / br * 1000L else 0L)
                )
        }

        @JavascriptInterface
        fun onLyric(reqId: String, text: String) {
            (pending.remove(reqId) as? Pending<String?>)
                ?.deferred?.complete(text.ifBlank { null })
        }

        @JavascriptInterface
        fun onPic(reqId: String, url: String) {
            (pending.remove(reqId) as? Pending<String?>)
                ?.deferred?.complete(url.ifBlank { null })
        }

        @JavascriptInterface
        fun onError(reqId: String, msg: String) {
            pending.remove(reqId)?.deferred?.completeExceptionally(RuntimeException(msg))
        }
    }

    private fun jq(s: String): String = JSONObject.quote(s)

    private suspend fun ensureReady(timeoutMs: Long = 4000) {
        if (ready) return
        withTimeout(timeoutMs) {
            while (!ready) delay(50)
        }
    }

    private suspend fun <T> callWebView(type: Int, jsFactory: (String) -> String): T? {
        ensureReady()
        val reqId = "r" + reqCounter.incrementAndGet()
        val deferred = CompletableDeferred<T>()
        pending[reqId] = Pending(deferred, type)
        val wv = webView
        if (wv == null) {
            pending.remove(reqId)
            return null
        }
        withContext(Dispatchers.Main.immediate) {
            runCatching { wv.resumeTimers() }
            wv.evaluateJavascript(jsFactory(reqId), null)
        }
        return try {
            withTimeout(15_000) { deferred.await() }
        } catch (_: Throwable) {
            pending.remove(reqId)
            null
        }
    }

    suspend fun search(keyword: String, count: Int = 30, page: Int = 1): List<Song> {
        if (keyword.isBlank()) return emptyList()
        val viaWeb = runCatching {
            callWebView<List<Song>>(T_SEARCH) { reqId ->
                "window.__hyper.search(${jq(reqId)}, ${jq(keyword)}, $count, $page)"
            }
        }.getOrNull()
        return viaWeb ?: GdApi.nativeSearch(keyword, count, page)
    }

    suspend fun resolveUrl(id: String, br: Int, source: String = "netease"): StreamInfo? {
        val viaWeb = runCatching {
            callWebView<StreamInfo?>(T_URL) { reqId ->
                "window.__hyper.url(${jq(reqId)}, ${jq(id)}, $br, ${jq(source)})"
            }
        }.getOrNull()
        return viaWeb ?: GdApi.nativeResolveUrl(id, br, source)
    }

    suspend fun lyric(id: String, source: String = "netease"): String? {
        val viaWeb = runCatching {
            callWebView<String?>(T_LYRIC) { reqId ->
                "window.__hyper.lyric(${jq(reqId)}, ${jq(id)}, ${jq(source)})"
            }
        }.getOrNull()
        return viaWeb ?: GdApi.nativeLyric(id, source)
    }

    suspend fun resolvePic(picId: String, size: Int = 500, source: String = "netease"): String? {
        if (picId.isBlank()) return null
        val viaWeb = runCatching {
            callWebView<String?>(T_PIC) { reqId ->
                "window.__hyper.pic(${jq(reqId)}, ${jq(picId)}, $size, ${jq(source)})"
            }
        }.getOrNull()
        return viaWeb ?: GdApi.nativePic(picId, size, source)
    }

}
