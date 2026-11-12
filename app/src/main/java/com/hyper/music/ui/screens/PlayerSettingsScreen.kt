package com.hyper.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hyper.music.music.MusicLibrary
import com.hyper.music.ui.basic.OverlayDropdownMenu
import com.hyper.music.ui.basic.rememberDropdownColors
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun PlayerSettingsScreen(
    onBack: () -> Unit,
    backdrop: LayerBackdrop,
) {
    val haptic = LocalHapticFeedback.current
    val settings by MusicLibrary.settings.collectAsState()
    val dropdownColors = rememberDropdownColors()
    val advancedMaterial = LocalAdvancedMaterialEnabled.current

    val dropdownBackdrop = if (advancedMaterial) backdrop else null
    val cardShape = RoundedCornerShape(20.dp)

    DetailPage(
        title = "播放界面",
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
                    OverlayDropdownMenu(
                        title = "专辑封面形状",
                        entry = remember(settings.coverCircular) {
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem(
                                        text = "圆形",
                                        selected = settings.coverCircular,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setCoverCircular(true)
                                        }
                                    ),
                                    DropdownItem(
                                        text = "圆角矩形",
                                        selected = !settings.coverCircular,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setCoverCircular(false)
                                        }
                                    ),
                                )
                            )
                        },
                        collapseOnSelection = true,
                        hyperBackdrop = dropdownBackdrop,
                        dropdownColors = dropdownColors,
                    )
                    OverlayDropdownMenu(
                        title = "标题位置",
                        entry = remember(settings.titleCentered) {
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem(
                                        text = "居中",
                                        selected = settings.titleCentered,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setTitleCentered(true)
                                        }
                                    ),
                                    DropdownItem(
                                        text = "左对齐",
                                        selected = !settings.titleCentered,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setTitleCentered(false)
                                        }
                                    ),
                                )
                            )
                        },
                        collapseOnSelection = true,
                        hyperBackdrop = dropdownBackdrop,
                        dropdownColors = dropdownColors,
                    )
                    OverlayDropdownMenu(
                        title = "歌词位置",
                        entry = remember(settings.lyricCentered) {
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem(
                                        text = "居中",
                                        selected = settings.lyricCentered,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setLyricCentered(true)
                                        }
                                    ),
                                    DropdownItem(
                                        text = "左对齐",
                                        selected = !settings.lyricCentered,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            MusicLibrary.setLyricCentered(false)
                                        }
                                    ),
                                )
                            )
                        },
                        collapseOnSelection = true,
                        hyperBackdrop = dropdownBackdrop,
                        dropdownColors = dropdownColors,
                    )
                    SliderPreference(
                        value = settings.lyricFontSize,
                        onValueChange = { MusicLibrary.setLyricFontSize(it) },
                        title = "歌词字体大小",
                        valueText = "${settings.lyricFontSize.toInt()}sp",
                        valueRange = 14f..36f,
                        steps = 21,
                    )
                    SliderPreference(
                        value = settings.lyricFontWeight.toFloat(),
                        onValueChange = { MusicLibrary.setLyricFontWeight(it.toInt()) },
                        title = "歌词字重",
                        valueText = weightLabel(settings.lyricFontWeight),
                        valueRange = 300f..900f,
                        steps = 5,
                    )
                    SwitchPreference(
                        title = "歌词翻译",
                        checked = settings.showLyricsTranslation,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            MusicLibrary.setShowLyricsTranslation(it)
                        }
                    )
                }
            }
        }
    }
}

private fun weightLabel(weight: Int): String = when (weight) {
    300 -> "细"
    400 -> "常规"
    500 -> "中等"
    600 -> "半粗"
    700 -> "粗"
    800 -> "特粗"
    900 -> "极粗"
    else -> "$weight"
}
