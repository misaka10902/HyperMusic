


package top.yukonga.miuix.kmp.squircle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Path
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported


val LocalSquircleEnabled = staticCompositionLocalOf { true }


@Composable
@ReadOnlyComposable
fun isSquircleEnabled(): Boolean = LocalSquircleEnabled.current && isRuntimeShaderSupported()
