package com.hyper.music.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.hyper.music.music.CoverLoader
import com.hyper.music.music.CoverSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object CoverSaver {
    suspend fun save(context: Context, url: String?, name: String): Boolean = withContext(Dispatchers.IO) {
        val target = url?.takeIf { it.isNotBlank() } ?: return@withContext false
        val bitmap = runCatching { CoverLoader.load(target, CoverSize.LARGE.decode) }
            .getOrNull()?.asAndroidBitmap() ?: return@withContext false
        val safe = name.replace(Regex("""[/\\:*?"<>|]"""), "_").take(80).ifBlank { "cover" }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$safe.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HyperMusic")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        val ok = runCatching {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                ?: false
        }.getOrDefault(false)
        if (!ok) {
            resolver.delete(uri, null, null)
        } else {
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }
        ok
    }
}

@Composable
fun Modifier.saveCoverOnLongPress(url: String?, name: String): Modifier {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return this.pointerInput(url, name) {
        detectTapGestures(
            onLongPress = {
                scope.launch {
                    val ok = CoverSaver.save(context, url, name)
                    Toast.makeText(
                        context,
                        if (ok) "已保存封面到 Pictures/HyperMusic" else "封面保存失败",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
    }
}
