package com.hyper.music.music

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

data class WallpaperState(
    val hasImage: Boolean = false,
    val blur: Float = 0f,
    val dim: Float = 0.2f,

    val cardOpacity: Float = 0.4f,
    val cardBlur: Float = 0f,
    val version: Int = 0,
)


data class RenderedWallpaper(val version: Int, val image: ImageBitmap?)

object WallpaperManager {
    private const val PREFS = "wallpaper_prefs"
    private const val KEY_BLUR = "blur"
    private const val KEY_DIM = "dim"
    private const val KEY_CARD_OPACITY = "card_opacity"
    private const val KEY_CARD_BLUR = "card_blur"
    private const val FILE_NAME = "wallpaper.jpg"
    private const val MAX_EDGE = 1024


    private const val BLUR_BASE_EDGE = 360

    const val MAX_BLUR = 15f
    const val MAX_DIM = 0.85f
    const val MIN_CARD_OPACITY = 0.3f
    const val MAX_CARD_BLUR = 50f

    private val _state = MutableStateFlow(WallpaperState())
    val state: StateFlow<WallpaperState> = _state


    val cardOpacityFlow = state.map { it.cardOpacity }.distinctUntilChanged()
    val cardBlurFlow = state.map { it.cardBlur }.distinctUntilChanged()
    val hasImageFlow = state.map { it.hasImage }.distinctUntilChanged()


    private val _render = MutableStateFlow<RenderedWallpaper?>(null)
    val render: StateFlow<RenderedWallpaper?> = _render

    private val renderScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    @Volatile
    private var cachedBitmap: ImageBitmap? = null
    @Volatile
    private var cachedVersion: Int = -1

    @Volatile
    private var blurBase: Bitmap? = null
    @Volatile
    private var blurBaseVersion: Int = -1

    private fun file(): File = File(appContext.filesDir, FILE_NAME)

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = WallpaperState(
            hasImage = file().exists(),
            blur = prefs.getFloat(KEY_BLUR, 0f).coerceIn(0f, MAX_BLUR),
            dim = prefs.getFloat(KEY_DIM, 0.2f).coerceIn(0f, MAX_DIM),
            cardOpacity = prefs.getFloat(KEY_CARD_OPACITY, 0.4f).coerceIn(MIN_CARD_OPACITY, 1f),
            cardBlur = prefs.getFloat(KEY_CARD_BLUR, 0f).coerceIn(0f, MAX_CARD_BLUR),
        )
        startRenderPipeline()
    }

    private fun startRenderPipeline() {
        renderScope.launch {
            state
                .map { Triple(it.version, it.blur, it.hasImage) }
                .distinctUntilChanged()
                .collectLatest { (version, blur, hasImage) ->
                    val image = if (!hasImage) {
                        null
                    } else {
                        loadBitmap()?.let { src ->
                            renderForDraw(src, blur, appContext.resources.displayMetrics.density)
                        }
                    }
                    publish(version, image)
                }
        }
    }

    private fun publish(version: Int, image: ImageBitmap?) {
        _render.value = RenderedWallpaper(version, image)
    }


    @Volatile
    var shownImage: ImageBitmap? = null
        private set

    @Volatile
    var shownVersion: Int = -1
        private set

    fun markShown(version: Int, image: ImageBitmap) {
        shownVersion = version
        shownImage = image
    }


    private var persistJob: Job? = null

    private fun persist(key: String, value: Float) {
        if (!::prefs.isInitialized) return
        persistJob?.cancel()
        persistJob = renderScope.launch {
            delay(260)
            prefs.edit().putFloat(key, value).apply()
        }
    }

    fun setBlur(value: Float) {
        val v = value.coerceIn(0f, MAX_BLUR)
        _state.value = _state.value.copy(blur = v)
        persist(KEY_BLUR, v)
    }

    fun setDim(value: Float) {
        val v = value.coerceIn(0f, MAX_DIM)
        _state.value = _state.value.copy(dim = v)
        persist(KEY_DIM, v)
    }

    fun setCardOpacity(value: Float) {
        val v = value.coerceIn(MIN_CARD_OPACITY, 1f)
        _state.value = _state.value.copy(cardOpacity = v)
        persist(KEY_CARD_OPACITY, v)
    }

    fun setCardBlur(value: Float) {
        val v = value.coerceIn(0f, MAX_CARD_BLUR)
        _state.value = _state.value.copy(cardBlur = v)
        persist(KEY_CARD_BLUR, v)
    }

    suspend fun setImage(context: Context, uri: Uri): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            init(context)
            val resolver = appContext.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return@withContext false
            var sample = 1
            while (longest / (sample * 2) >= MAX_EDGE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@withContext false
            val target = file()
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()

            blurBase = null
            blurBaseVersion = -1
            _state.value = _state.value.copy(
                hasImage = true,
                version = _state.value.version + 1,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clear() {
        if (::appContext.isInitialized) runCatching { file().delete() }
        blurBase = null
        blurBaseVersion = -1
        shownImage = null
        shownVersion = -1
        _state.value = _state.value.copy(
            hasImage = false,
            version = _state.value.version + 1,
        )
    }

    fun loadBitmap(): ImageBitmap? = synchronized(this) {
        val version = _state.value.version
        if (cachedVersion == version && cachedBitmap != null) return@synchronized cachedBitmap
        val bitmap = if (!::appContext.isInitialized || !file().exists()) {
            null
        } else {
            runCatching {
                BitmapFactory.decodeFile(file().absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
        cachedBitmap = bitmap
        cachedVersion = version
        bitmap
    }


    fun renderForDraw(src: ImageBitmap, radiusDp: Float, density: Float): ImageBitmap {
        if (radiusDp <= 0.5f) return src
        return runCatching {
            val srcBmp = src.asAndroidBitmap()
            val base = blurBaseFor(srcBmp)
            val w = base.width
            val h = base.height


            val pix = IntArray(w * h)
            base.getPixels(pix, 0, w, 0, 0, w, h)
            val scale = w.toFloat() / srcBmp.width.coerceAtLeast(1)
            val radiusPx = radiusDp * density

            val r = (radiusPx * scale / 2f).coerceIn(0.02f, 64f)
            boxBlurFractional(pix, w, h, r, 3)
            Bitmap.createBitmap(pix, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        }.getOrElse { src }
    }

    private fun blurBaseFor(src: Bitmap): Bitmap {
        val version = _state.value.version
        val cached = blurBase
        if (blurBaseVersion == version && cached != null && !cached.isRecycled) return cached
        val edge = maxOf(src.width, src.height)
        val bmp = if (edge <= BLUR_BASE_EDGE) {
            src.copy(Bitmap.Config.ARGB_8888, true) ?: src
        } else {
            val ratio = BLUR_BASE_EDGE.toFloat() / edge
            val w = (src.width * ratio).roundToInt().coerceAtLeast(1)
            val h = (src.height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(src, w, h, true) ?: src
        }
        blurBase = bmp
        blurBaseVersion = version
        return bmp
    }


    private fun boxBlurFractional(pix: IntArray, w: Int, h: Int, radius: Float, passes: Int) {
        if (radius <= 0.01f || w < 3 || h < 3) return
        val ri = radius.toInt()
        val frac = radius - ri
        val tmp = IntArray(w * h)
        val div = 2 * ri + 1 + 2 * frac

        repeat(passes) {

            for (y in 0 until h) {
                val o = y * w
                var a = 0; var r = 0; var g = 0; var b = 0
                for (i in -ri..ri) {
                    val p = pix[o + i.coerceIn(0, w - 1)]
                    a += (p ushr 24) and 0xFF
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                }
                for (x in 0 until w) {
                    val pl = pix[o + (x - ri - 1).coerceIn(0, w - 1)]
                    val pr = pix[o + (x + ri + 1).coerceIn(0, w - 1)]
                    val oa = a + (((pl ushr 24) and 0xFF) + ((pr ushr 24) and 0xFF)) * frac
                    val or = r + (((pl shr 16) and 0xFF) + ((pr shr 16) and 0xFF)) * frac
                    val og = g + (((pl shr 8) and 0xFF) + ((pr shr 8) and 0xFF)) * frac
                    val ob = b + ((pl and 0xFF) + (pr and 0xFF)) * frac
                    tmp[o + x] = ((oa / div).toInt() shl 24) or
                        ((or / div).toInt() shl 16) or
                        ((og / div).toInt() shl 8) or
                        (ob / div).toInt()
                    val po = pix[o + (x - ri).coerceIn(0, w - 1)]
                    val pi = pix[o + (x + ri + 1).coerceIn(0, w - 1)]
                    a += ((pi ushr 24) and 0xFF) - ((po ushr 24) and 0xFF)
                    r += ((pi shr 16) and 0xFF) - ((po shr 16) and 0xFF)
                    g += ((pi shr 8) and 0xFF) - ((po shr 8) and 0xFF)
                    b += (pi and 0xFF) - (po and 0xFF)
                }
            }

            for (x in 0 until w) {
                var a = 0; var r = 0; var g = 0; var b = 0
                for (i in -ri..ri) {
                    val p = tmp[i.coerceIn(0, h - 1) * w + x]
                    a += (p ushr 24) and 0xFF
                    r += (p shr 16) and 0xFF
                    g += (p shr 8) and 0xFF
                    b += p and 0xFF
                }
                for (y in 0 until h) {
                    val pl = tmp[(y - ri - 1).coerceIn(0, h - 1) * w + x]
                    val pr = tmp[(y + ri + 1).coerceIn(0, h - 1) * w + x]
                    val oa = a + (((pl ushr 24) and 0xFF) + ((pr ushr 24) and 0xFF)) * frac
                    val or = r + (((pl shr 16) and 0xFF) + ((pr shr 16) and 0xFF)) * frac
                    val og = g + (((pl shr 8) and 0xFF) + ((pr shr 8) and 0xFF)) * frac
                    val ob = b + ((pl and 0xFF) + (pr and 0xFF)) * frac
                    pix[y * w + x] = ((oa / div).toInt() shl 24) or
                        ((or / div).toInt() shl 16) or
                        ((og / div).toInt() shl 8) or
                        (ob / div).toInt()
                    val po = tmp[(y - ri).coerceIn(0, h - 1) * w + x]
                    val pi = tmp[(y + ri + 1).coerceIn(0, h - 1) * w + x]
                    a += ((pi ushr 24) and 0xFF) - ((po ushr 24) and 0xFF)
                    r += ((pi shr 16) and 0xFF) - ((po shr 16) and 0xFF)
                    g += ((pi shr 8) and 0xFF) - ((po shr 8) and 0xFF)
                    b += (pi and 0xFF) - (po and 0xFF)
                }
            }
        }
    }
}

@Composable
fun WallpaperBackground() {
    val state by WallpaperManager.state.collectAsState()
    val rendered by WallpaperManager.render.collectAsState()


    var front by remember {
        mutableStateOf(if (WallpaperManager.state.value.hasImage) WallpaperManager.shownImage else null)
    }
    var frontVersion by remember {
        mutableIntStateOf(if (WallpaperManager.state.value.hasImage) WallpaperManager.shownVersion else -1)
    }
    var back by remember { mutableStateOf<ImageBitmap?>(null) }
    val backAlpha = remember { Animatable(0f) }


    val layerAlpha = remember {
        Animatable(
            if (WallpaperManager.state.value.hasImage && WallpaperManager.shownImage != null) 1f else 0f,
        )
    }


    val ready = rendered?.image?.takeIf { rendered?.version == state.version }

    LaunchedEffect(state.hasImage, state.version, ready) {
        if (!state.hasImage) {

            back = null
            backAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        if (ready == null) {


            if (front == null) {
                val fallback = kotlinx.coroutines.withContext(Dispatchers.IO) { WallpaperManager.loadBitmap() }
                if (fallback != null && front == null) front = fallback
            }
            return@LaunchedEffect
        }
        if (front == null) {


            front = ready
            frontVersion = state.version
            WallpaperManager.markShown(state.version, ready)
            return@LaunchedEffect
        }
        if (state.version == frontVersion) {

            front = ready
            return@LaunchedEffect
        }
        if (ready === front) {
            frontVersion = state.version
            return@LaunchedEffect
        }
        back = ready
        backAlpha.snapTo(0f)
        backAlpha.animateTo(1f, tween(220, easing = FastOutSlowInEasing))

        front = ready
        frontVersion = state.version
        WallpaperManager.markShown(state.version, ready)
        back = null
    }


    LaunchedEffect(state.hasImage, front) {
        if (state.hasImage) {
            if (front != null) {
                layerAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            } else {

                layerAlpha.snapTo(0f)
            }
        } else {
            if (layerAlpha.value > 0f) {
                layerAlpha.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            }
            front = null
            frontVersion = -1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = layerAlpha.value },
    ) {
        front?.let { bmp -> WallpaperLayer(bmp, state.dim) }
        back?.let { bmp ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backAlpha.value },
            ) {
                WallpaperLayer(bmp, state.dim)
            }
        }
    }
}


@Composable
private fun WallpaperLayer(bitmap: ImageBitmap, dim: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (dim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dim)),
            )
        }
    }
}
