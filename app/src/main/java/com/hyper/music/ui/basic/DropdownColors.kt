package com.hyper.music.ui.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hyper.music.ui.effects.miuix.LocalAdvancedMaterialEnabled
import com.hyper.music.ui.utils.isAppDarkTheme
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberDropdownColors(): DropdownColors {
    val advancedMaterial = LocalAdvancedMaterialEnabled.current
    val isLightTheme = !isAppDarkTheme()
    val pillColor = if (isLightTheme) Color(0xFFE8E8E8) else Color(0xFF333333)
    return if (advancedMaterial) {
        DropdownDefaults.dropdownColors(
            containerColor = Color.Transparent,
            selectedContainerColor = pillColor,
        )
    } else {
        val container = MiuixTheme.colorScheme.surfaceContainer
        DropdownDefaults.dropdownColors(
            containerColor = container,
            selectedContainerColor = pillColor,
        )
    }
}
