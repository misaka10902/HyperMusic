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

/** 一次渲染结果：换图（version）改变时 version 变，调节模糊时 version 不变。 */
data class RenderedWallpaper(val version: Int, val image: ImageBitmap?)

object WallpaperManager {
    private const val PREFS = "wallpaper_prefs"
    private const val KEY_BLUR = "blur"
    private const val KEY_DIM = "dim"
    private const val KEY_CARD_OPACITY = "card_opacity"
    private const val KEY_CARD_BLUR = "card_blur"
    private const val FILE_NAME = "wallpaper.jpg"
    private const val MAX_EDGE = 1024

    /** 模糊计算用的基底长边：壁纸本来就是模糊的，在小图上卷积既快又平滑。 */
    private const val BLUR_BASE_EDGE = 360

    const val MAX_BLUR = 15f
    const val MAX_DIM = 0.85f
    const val MIN_CARD_OPACITY = 0.3f
    const val MAX_CARD_BLUR = 50f

    private val _state = MutableStateFlow(WallpaperState())
    val state: StateFlow<WallpaperState> = _state

    /**
     * 卡片只关心这三个值。单独拆出来是为了让「拖动壁纸模糊滑块」不会把每一张卡片
     * 都重组一遍（卡片数量多，全量订阅 state 会明显掉帧）。
     */
    val cardOpacityFlow = state.map { it.cardOpacity }.distinctUntilChanged()
    val cardBlurFlow = state.map { it.cardBlur }.distinctUntilChanged()
    val hasImageFlow = state.map { it.hasImage }.distinctUntilChanged()

    /**
     * 唯一的渲染结果流。
     *
     * 之前是每个 `WallpaperBackground` 各自 `produceState` 重算：根层一份、每个页面各一份，
     * 拖模糊滑块时会有 2~3 个全图模糊同时抢 Default 线程，表现为一顿一顿。
     * 现在全应用只有这一份，用 collectLatest 丢掉过期的中间值，滑块跟手。
     */
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

    /**
     * 上一次**真正画到屏幕上**的图与版本。
     *
     * `WallpaperBackground` 被销毁重建时（切页、从设置页返回、系统回收后恢复），
     * 组合过程中的 `remember` 是全新的；如果只依赖渲染管线的 publish，会有一段时间
     * 「没有版本可显示」，直接露出 surface 白底。这里留一份已上屏结果，重建时立刻恢复。
     */
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

    /**
     * 写盘防抖：滑块每帧都会 set，直接每帧 apply() 会把主线程拖住，表现为一顿一顿。
     * 松开手 260ms 后落盘一次即可。
     */
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
            // 换图后基底缓存作废。
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

    /**
     * 生成用于绘制的壁纸位图：固定基底降采样 + 三次 box blur（≈ 高斯）。
     *
     * 全部在 CPU 上完成，绘制层**不挂 RenderEffect**：壁纸层会被 `layerBackdrop`
     * 录进 display list，`Modifier.blur` 在录制回放时会让部分 GPU 驱动崩在 native 层。
     *
     * 基底（长边 [BLUR_BASE_EDGE]）按版本缓存一次，调节滑块时只在已缩小的位图上卷积，
     * 单帧成本降到毫秒级，拖动才连续。
     */
    fun renderForDraw(src: ImageBitmap, radiusDp: Float, density: Float): ImageBitmap {
        if (radiusDp <= 0.5f) return src
        return runCatching {
            val srcBmp = src.asAndroidBitmap()
            val base = blurBaseFor(srcBmp)
            val w = base.width
            val h = base.height
            // 直接取像素做卷积再建位图：省掉一次整图 copy + setPixels，
            // 拖动滑块时每帧的计算量能压到几毫秒。
            val pix = IntArray(w * h)
            base.getPixels(pix, 0, w, 0, 0, w, h)
            val scale = w.toFloat() / srcBmp.width.coerceAtLeast(1)
            val radiusPx = radiusDp * density
            // 半径保留小数：取整会让 0~15dp 只剩七八个档位，拖动时一跳一跳。
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

    /**
     * 分数半径的盒式模糊（三次 ≈ 高斯），原地修改像素数组。
     *
     * 整数半径会让模糊强度只有「几个档」，拖动滑块时一跳一跳；这里把小数部分
     * 作为窗口两端外侧像素的权重，强度因此连续变化，且成本与整数版相同。
     */
    private fun boxBlurFractional(pix: IntArray, w: Int, h: Int, radius: Float, passes: Int) {
        if (radius <= 0.01f || w < 3 || h < 3) return
        val ri = radius.toInt()
        val frac = radius - ri
        val tmp = IntArray(w * h)
        val div = 2 * ri + 1 + 2 * frac

        repeat(passes) {
            // 横向：pix -> tmp
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
            // 纵向：tmp -> pix
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

    // 双图层交叉淡化：front 永远是一张已经画满全屏的图，back 从 0 淡到 1 之后才顶替 front。
    // 任何时刻都不会出现「两层都是空的」那一帧，所以换图、切页、组件重建都不会露白。
    //
    // front 的初值取自「上次真正上屏」的那一份：切换 Tab / 从设置页返回时组合会重建，
    // 若只依赖渲染管线的 publish，重建后要等新结果出来才有图，这中间就是一片白。
    var front by remember {
        mutableStateOf(if (WallpaperManager.state.value.hasImage) WallpaperManager.shownImage else null)
    }
    var frontVersion by remember {
        mutableIntStateOf(if (WallpaperManager.state.value.hasImage) WallpaperManager.shownVersion else -1)
    }
    var back by remember { mutableStateOf<ImageBitmap?>(null) }
    val backAlpha = remember { Animatable(0f) }

    // 整层不透明度：只负责「纯色 ↔ 壁纸」的过渡，换图不经过它。
    // 换图时整层始终是 1，只让 back 层在新旧图之间淡化，所以连续换图也不会整体闪。
    val layerAlpha = remember {
        Animatable(
            if (WallpaperManager.state.value.hasImage && WallpaperManager.shownImage != null) 1f else 0f,
        )
    }

    // 只有「渲染结果与当前 state 同版本」才算可用；换图后位图还没算好时不切换。
    val ready = rendered?.image?.takeIf { rendered?.version == state.version }

    LaunchedEffect(state.hasImage, state.version, ready) {
        if (!state.hasImage) {
            // 这里不动 front：淡出还指望它，等整层淡到 0 之后再清（见下面那个 effect）。
            back = null
            backAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        if (ready == null) {
            // 渲染管线还没吐出结果（冷启动首帧 / 计算被后续操作取消）：
            // 先同步取一张原图垫上，宁可短暂「不模糊」，也不要整片白。
            if (front == null) {
                val fallback = kotlinx.coroutines.withContext(Dispatchers.IO) { WallpaperManager.loadBitmap() }
                if (fallback != null && front == null) front = fallback
            }
            return@LaunchedEffect
        }
        if (front == null) {
            // 从「没有壁纸」变成「有壁纸」：直接把图放上，整层淡入交给 layerAlpha，
            // 这样纯色与壁纸是叠化关系，而不是先空一帧再跳出。
            front = ready
            frontVersion = state.version
            WallpaperManager.markShown(state.version, ready)
            return@LaunchedEffect
        }
        if (state.version == frontVersion) {
            // 同一个版本只是模糊/压暗换了：直接替换，跟手，不播淡化。
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
        // 淡入完成后才让新图成为前景；动画被取消（又换了图）时不会走到这里。
        front = ready
        frontVersion = state.version
        WallpaperManager.markShown(state.version, ready)
        back = null
    }

    // 整层淡入 / 淡出。必须等真的有一张图可画再淡入，否则会先淡出一片空白。
    LaunchedEffect(state.hasImage, front) {
        if (state.hasImage) {
            if (front != null) {
                layerAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            } else {
                // 还没有图可画，保持透明，等 front 就位后由上面的分支淡入。
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

/** 单层壁纸：图 + 压暗遮罩。压暗跟着层一起淡入，避免淡化时亮度先跳一下。 */
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
