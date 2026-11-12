package com.hyper.music.music

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

data class WallpaperState(
    val hasImage: Boolean = false,
    val blur: Float = 0f,
    val dim: Float = 0.2f,

    val cardOpacity: Float = 0.4f,
    val version: Int = 0,
)

object WallpaperManager {
    private const val PREFS = "wallpaper_prefs"
    private const val KEY_BLUR = "blur"
    private const val KEY_DIM = "dim"
    private const val KEY_CARD_OPACITY = "card_opacity"
    private const val FILE_NAME = "wallpaper.jpg"
    private const val MAX_EDGE = 1024
    const val MAX_BLUR = 25f
    const val MAX_DIM = 0.85f

    private val _state = MutableStateFlow(WallpaperState())
    val state: StateFlow<WallpaperState> = _state

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    @Volatile
    private var cachedBitmap: ImageBitmap? = null
    @Volatile
    private var cachedVersion: Int = -1

    private fun file(): File = File(appContext.filesDir, FILE_NAME)

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = WallpaperState(
            hasImage = file().exists(),
            blur = prefs.getFloat(KEY_BLUR, 0f).coerceIn(0f, MAX_BLUR),
            dim = prefs.getFloat(KEY_DIM, 0.2f).coerceIn(0f, MAX_DIM),
            cardOpacity = prefs.getFloat(KEY_CARD_OPACITY, 0.4f).coerceIn(0f, 1f),
        )
    }

    fun setBlur(value: Float) {
        val v = value.coerceIn(0f, MAX_BLUR)
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_BLUR, v).apply()
        _state.value = _state.value.copy(blur = v)
    }

    fun setDim(value: Float) {
        val v = value.coerceIn(0f, MAX_DIM)
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_DIM, v).apply()
        _state.value = _state.value.copy(dim = v)
    }

    fun setCardOpacity(value: Float) {
        val v = value.coerceIn(0f, 1f)
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_CARD_OPACITY, v).apply()
        _state.value = _state.value.copy(cardOpacity = v)
    }

    suspend fun setImage(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
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
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun WallpaperBackground(
    topBlurEnabled: Boolean = true,
    topBlurHeight: Dp = 320.dp,
) {
    val state by WallpaperManager.state.collectAsState()
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = state.version) {
        value = withContext(Dispatchers.IO) { WallpaperManager.loadBitmap() }
    }

    val blurModifier = if (state.blur > 0.01f) Modifier.blur(state.blur.dp) else Modifier
    val baseScale = 1f + state.blur / 200f

    val target = if (state.hasImage) image else null
    Crossfade(
        targetState = target,
        animationSpec = tween(450),
        label = "wallpaperCrossfade",
    ) { bmp ->
        if (bmp != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = baseScale
                            scaleY = baseScale
                        }
                        .then(blurModifier),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = state.dim)),
                )
            }
        }
    }
}

@Composable
fun WallpaperPageBox(
    modifier: Modifier = Modifier,
    topBlurEnabled: Boolean = true,
    topBlurHeight: Dp = 320.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        WallpaperBackground(
            topBlurEnabled = topBlurEnabled,
            topBlurHeight = topBlurHeight,
        )
        content()
    }
}
