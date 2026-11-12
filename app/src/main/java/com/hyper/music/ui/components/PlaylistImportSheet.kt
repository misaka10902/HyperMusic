package com.hyper.music.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.PlaylistImportResult
import com.hyper.music.music.PlaylistImporter
import com.hyper.music.music.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.NativeTextField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.hyper.music.ui.effects.miuix.LocalActiveBackdrop
import top.yukonga.miuix.kmp.glass.GlassTabRow
import top.yukonga.miuix.kmp.glass.GlassTabRowDefaults

private val ImportTabTitles = listOf("链接", "文件", "手动")


@Composable
fun PlaylistImportSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onImported: (name: String, songs: List<Song>, skipped: Int) -> Unit,
    pendingSongs: List<Song> = emptyList(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MiuixTheme.colorScheme

    var tab by remember { mutableStateOf(0) }
    var linkText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (visible) {
            tab = 0
            linkText = ""
            nameText = ""
            busy = false
            status = null
            error = null
        }
    }

    val finish: (PlaylistImportResult) -> Unit = { res ->
        val merged = (res.songs + pendingSongs).distinctBy { it.id }
        if (merged.isEmpty()) {
            busy = false
            status = null
            error = "没有导入到任何歌曲"
        } else {
            busy = false
            status = null
            onImported(res.name, merged, res.skipped)
            onDismiss()
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            error = null
            status = "读取文件中…"
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                } ?: throw IllegalStateException("无法读取该文件")
                val res = PlaylistImporter.importFromJson(text) { done, total ->
                    status = "匹配网易云歌曲 $done/$total"
                }
                finish(res)
            } catch (e: Throwable) {
                busy = false
                status = null
                error = e.message ?: "导入失败"
            }
        }
    }

    HyperSheet(
        visible = visible,
        onDismiss = { if (!busy) onDismiss() },
        followIme = true,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader("添加歌单")
            Spacer(Modifier.height(10.dp))


            GlassTabRow(
                tabs = ImportTabTitles,
                selectedIndex = tab,
                onSelect = { if (!busy) tab = it },
                backdrop = LocalActiveBackdrop.current,
                modifier = Modifier.fillMaxWidth(),
                colors = GlassTabRowDefaults.primaryColors(),
            )

            Spacer(Modifier.height(18.dp))



            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (tab) {
                0 -> {
                    ImportTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        hint = "输入歌单链接…",
                        leadingIcon = MiuixIcons.Link,
                    )
                    ImportButton(
                        text = if (busy) "获取中…" else "获取歌单",
                        enabled = linkText.isNotBlank() && !busy,
                    ) {
                        scope.launch {
                            busy = true
                            error = null
                            status = "获取歌单…"
                            try {
                                val res = PlaylistImporter.importFromLink(linkText) { done, total ->
                                    status = "匹配网易云歌曲 $done/$total"
                                }
                                finish(res)
                            } catch (e: Throwable) {
                                busy = false
                                status = null
                                error = e.message ?: "导入失败"
                            }
                        }
                    }
                }

                1 -> {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = !busy) {
                                fileLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/plain",
                                        "application/octet-stream",
                                        "*/*",
                                    ),
                                )
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            imageVector = MiuixIcons.UploadCloud,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            colorFilter = ColorFilter.tint(scheme.onSurfaceSecondary),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "点击选择 JSON 文件",
                            color = scheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "支持本应用导出的歌单 JSON",
                            color = scheme.onSurfaceSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }

                else -> {
                    ImportTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        hint = "输入歌单名称…",
                        leadingIcon = MiuixIcons.Edit,
                    )
                    ImportButton(
                        text = "创建歌单",
                        enabled = nameText.isNotBlank() && !busy,
                    ) {
                        val name = nameText.trim()
                        if (name.isNotBlank()) {
                            onImported(name, pendingSongs, 0)
                            onDismiss()
                        }
                    }
                }
                }
            }

            val hint = error ?: status
            if (hint != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (error == null) {
                        InfiniteProgressIndicator(
                            color = scheme.onSurfaceSecondary,
                            size = 16.dp,
                            strokeWidth = 2.dp,
                            orbitingDotSize = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = hint,
                        color = if (error != null) scheme.error else scheme.onSurfaceSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}


@Composable
private fun ImportTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.secondaryContainer)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 16.dp, end = 10.dp)
                .size(20.dp),
            tint = scheme.onSurfaceSecondary,
        )
        NativeTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).height(56.dp),
            hint = hint,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = scheme.onSurface,
            ),
            singleLine = true,
        )
    }
}


@Composable
private fun ImportButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColorsPrimary(),
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
