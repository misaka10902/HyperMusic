


package top.yukonga.miuix.kmp.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.utils.MiuixIndication
import top.yukonga.miuix.kmp.utils.MiuixOverscrollFactory


@Composable
fun MiuixTheme(
    controller: ThemeController,
    textStyles: TextStyles = MiuixTheme.textStyles,
    content: @Composable () -> Unit,
) {
    ProvideDisplayCutoutFix()
    val rawColors = controller.currentColors()
    val miuixColors = remember { rawColors.copy() }.apply { updateColorsFrom(rawColors) }
    val miuixTextStyles = remember { textStyles.copy() }.apply { updateTextStylesFrom(textStyles) }
    val miuixIndication = remember(miuixColors.onBackground) { MiuixIndication(color = miuixColors.onBackground) }
    CompositionLocalProvider(
        LocalColors provides miuixColors,
        LocalTextStyles provides miuixTextStyles,
        LocalIndication provides miuixIndication,
        LocalContentColor provides miuixColors.onBackground,
        LocalColorSchemeMode provides controller.colorSchemeMode,
        LocalOverscrollFactory provides MiuixOverscrollFactory,
    ) {
        content()
    }
}


@Composable
fun MiuixTheme(
    colors: Colors = MiuixTheme.colorScheme,
    textStyles: TextStyles = MiuixTheme.textStyles,
    content: @Composable () -> Unit,
) {
    ProvideDisplayCutoutFix()
    val miuixColors = remember { colors.copy() }.apply { updateColorsFrom(colors) }
    val miuixTextStyles = remember { textStyles.copy() }.apply { updateTextStylesFrom(textStyles) }
    val miuixIndication = remember(miuixColors.onBackground) { MiuixIndication(color = miuixColors.onBackground) }
    CompositionLocalProvider(
        LocalColors provides miuixColors,
        LocalTextStyles provides miuixTextStyles,
        LocalIndication provides miuixIndication,
        LocalOverscrollFactory provides MiuixOverscrollFactory,
    ) {
        content()
    }
}

object MiuixTheme {
    val colorScheme: Colors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current

    val textStyles: TextStyles
        @Composable @ReadOnlyComposable
        get() = LocalTextStyles.current

    val colorSchemeMode: ColorSchemeMode?
        @Composable @ReadOnlyComposable
        get() = LocalColorSchemeMode.current

    val isDynamicColor: Boolean
        @Composable @ReadOnlyComposable
        get() = when (colorSchemeMode) {
            ColorSchemeMode.MonetSystem,
            ColorSchemeMode.MonetLight,
            ColorSchemeMode.MonetDark,
            -> true

            else -> false
        }
}
