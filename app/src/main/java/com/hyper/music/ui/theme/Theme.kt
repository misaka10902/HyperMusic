package com.hyper.music.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lerpColors
import top.yukonga.miuix.kmp.theme.lightColorScheme

private const val THEME_COLOR_KEY = "theme_color"

private const val THEME_COLOR_MIKU = "miku"

private const val THEME_COLOR_HYPEROS = "hyperos"

private fun hyperOsLightColors(): Colors = lightColorScheme(
    primary = Color(0xFF3482FF),
    primaryVariant = Color(0xFF3482FF),
    disabledPrimary = Color(0xFFC2D9FF),
    disabledOnPrimary = Color(0xFFF3F8FF),
    disabledPrimaryButton = Color(0xFFC2D9FF),
    disabledOnPrimaryButton = Color(0xFFFFFFFF),
    disabledPrimarySlider = Color(0xFFB8CFF5),
    primaryContainer = Color(0xFF5D9BFF),
    onPrimaryContainer = Color.White,
    tertiaryContainer = Color(0xFFEAF2FF),
    onTertiaryContainer = Color(0xFF3482FF),
    sliderKeyPointForeground = Color(0xFF6EB5FF),
)

private fun hyperOsDarkColors(): Colors = darkColorScheme(
    primary = Color(0xFF277AF7),
    primaryVariant = Color(0xFF0073DD),
    disabledPrimary = Color(0xFF253E64),
    disabledOnPrimary = Color(0xFF677993),
    disabledPrimaryButton = Color(0xFF253E64),
    disabledOnPrimaryButton = Color(0xFF677893),
    disabledPrimarySlider = Color(0xFF44587C),
    primaryContainer = Color(0xFF338FE4),
    onPrimaryContainer = Color.White,
    tertiaryContainer = Color(0xFF2B3B54),
    onTertiaryContainer = Color(0xFF4788ff),
    sliderKeyPointForeground = Color(0xFF5DAAFF),
)

private fun mikuLightColors(): Colors = lightColorScheme(
    primary = Color(0xFF39C5BB),
    primaryVariant = Color(0xFF39C5BB),
    disabledPrimary = Color(0xFFB2E6E1),
    disabledOnPrimary = Color(0xFFE6F8F5),
    disabledPrimaryButton = Color(0xFFB2E6E1),
    disabledOnPrimaryButton = Color(0xFFFFFFFF),
    disabledPrimarySlider = Color(0xFF9BDAD3),
    primaryContainer = Color(0xFF5FD6CD),
    onPrimaryContainer = Color.White,
    tertiaryContainer = Color(0xFFE6F8F5),
    onTertiaryContainer = Color(0xFF39C5BB),
    sliderKeyPointForeground = Color(0xFF52CFC4),
)

private fun mikuDarkColors(): Colors = darkColorScheme(
    primary = Color(0xFF2FB8AE),
    primaryVariant = Color(0xFF1AA89E),
    disabledPrimary = Color(0xFF1F4A47),
    disabledOnPrimary = Color(0xFF6FA8A3),
    disabledPrimaryButton = Color(0xFF1F4A47),
    disabledOnPrimaryButton = Color(0xFF6FA8A3),
    disabledPrimarySlider = Color(0xFF2E5F5B),
    primaryContainer = Color(0xFF3AC4BA),
    onPrimaryContainer = Color.White,
    tertiaryContainer = Color(0xFF1F4A47),
    onTertiaryContainer = Color(0xFF52CFC4),
    sliderKeyPointForeground = Color(0xFF40C9BE),
)

@Composable
fun CourseHyperTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE) }
    val themeMode = remember { mutableStateOf(prefs.getString("theme_mode", "system") ?: "system") }
    val themeColor = remember { mutableStateOf(prefs.getString(THEME_COLOR_KEY, THEME_COLOR_MIKU) ?: THEME_COLOR_MIKU) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "theme_mode" -> {
                    themeMode.value = prefs.getString("theme_mode", "system") ?: "system"
                }
                THEME_COLOR_KEY -> {
                    themeColor.value = prefs.getString(THEME_COLOR_KEY, THEME_COLOR_MIKU) ?: THEME_COLOR_MIKU
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val controller = remember(themeMode.value, themeColor.value) {
        val (lightColors, darkColors) = when (themeColor.value) {
            THEME_COLOR_MIKU -> mikuLightColors() to mikuDarkColors()
            THEME_COLOR_HYPEROS -> hyperOsLightColors() to hyperOsDarkColors()
            else -> lightColorScheme() to darkColorScheme()
        }
        ThemeController(
            when (themeMode.value) {
                "light" -> ColorSchemeMode.Light
                "dark" -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.System
            },
            lightColors = lightColors,
            darkColors = darkColors,
        )
    }

    val targetColors = controller.currentColors()

    val animatedColors = remember { targetColors.copy() }

    LaunchedEffect(targetColors) {
        val from = animatedColors.copy()
        val progress = Animatable(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 320,
                easing = FastOutSlowInEasing,
            ),
        ) {
            animatedColors.lerpColors(from, targetColors, value)
        }
    }

    MiuixTheme(
        colors = animatedColors,
        content = content
    )
}
