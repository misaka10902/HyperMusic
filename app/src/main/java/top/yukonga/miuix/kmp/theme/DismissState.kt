


package top.yukonga.miuix.kmp.theme

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup


val LocalDismissState = staticCompositionLocalOf<(() -> Unit)?> { null }
