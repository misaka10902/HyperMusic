package com.hyper.music.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hyper.music.music.WallpaperManager
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun WallpaperScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by WallpaperManager.state.collectAsState()
    val cardShape = RoundedCornerShape(20.dp)
    val buttonShape = RoundedCornerShape(16.dp)

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = WallpaperManager.setImage(context, uri)
            Toast.makeText(
                context,
                if (ok) "壁纸已设置" else "图片读取失败",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    DetailPage(
        title = "更换背景",
        subtitle = "建议配合调整显示模式以获得更佳的视觉效果",
        onBack = onBack,
        backdrop = backdrop,
        header = {},
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                    insideMargin = PaddingValues(0.dp),
                    colors = cardGlassColors(),
                ) {
                    ArrowPreference(
                        title = "选择壁纸",
                        summary = if (state.hasImage) "已设置，点击可更换" else "在系统相册里选择一张图片",
                        onClick = { picker.launch("image/*") },
                    )
                }
            }
        }
        if (state.hasImage) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {

                    Card(
                        cornerRadius = 20.dp,
                        modifier = Modifier.fillMaxWidth().cardGlass(cardShape),
                        insideMargin = PaddingValues(0.dp),
                        colors = cardGlassColors(),
                    ) {
                        SliderPreference(
                            value = state.blur,
                            onValueChange = { WallpaperManager.setBlur(it) },
                            title = "壁纸模糊",
                            valueText = "${state.blur.toInt()}dp",
                            valueRange = 0f..WallpaperManager.MAX_BLUR,
                            steps = 24,
                        )
                        SliderPreference(
                            value = state.dim,
                            onValueChange = { WallpaperManager.setDim(it) },
                            title = "壁纸压暗",
                            valueText = "${(state.dim * 100).toInt()}%",
                            valueRange = 0f..WallpaperManager.MAX_DIM,
                            steps = 16,
                        )
                        SliderPreference(
                            value = state.cardOpacity,
                            onValueChange = { WallpaperManager.setCardOpacity(it) },
                            title = "卡片不透明度",
                            valueText = "${(state.cardOpacity * 100).toInt()}%",
                            valueRange = 0f..1f,
                            steps = 0,
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        WallpaperManager.clear()
                        Toast.makeText(context, "壁纸已删除", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .cardGlass(buttonShape),
                    colors = ButtonDefaults.buttonColors(color = Color.Transparent),
                ) {
                    Text(text = "删除壁纸", color = MiuixTheme.colorScheme.error)
                }
            }
        }
    }
}
