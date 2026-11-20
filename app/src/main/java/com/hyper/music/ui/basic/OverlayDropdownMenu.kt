package com.hyper.music.ui.basic

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.glassPopupAnchorValue
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout


@Composable
@Suppress("UNUSED_PARAMETER")
fun OverlayDropdownMenu(
    entry: DropdownEntry,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors? = null,
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    hyperBackdrop: Backdrop? = null,
) {
    OverlayDropdownMenu(
        entries = remember(entry) { listOf(entry) },
        title = title,
        modifier = modifier,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        dropdownColors = dropdownColors,
        startAction = startAction,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        maxHeight = maxHeight,
        enabled = enabled,
        renderInRootScaffold = renderInRootScaffold,
        collapseOnSelection = collapseOnSelection,
        onExpandedChange = onExpandedChange,
        hyperBackdrop = hyperBackdrop,
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun OverlayDropdownMenu(
    entries: List<DropdownEntry>,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors? = null,
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    hyperBackdrop: Backdrop? = null,
) {

    val items = remember(entries) { entries.filter { it.enabled }.flatMap { it.items } }
    val selectedIndex = remember(items) { items.indexOfFirst { it.selected } }
    val sizing = remember(maxHeight) {
        if (maxHeight != null) GlassPopupSizing(maxHeight = maxHeight)
        else GlassPopupSizing()
    }
    val haptic = LocalHapticFeedback.current
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)

    var expanded by remember { mutableStateOf(false) }
    val mounted = remember { mutableStateOf(false) }
    val glassAnchor = rememberGlassPopupAnchor()
    var rowBounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val popupAnchor = remember(rowBounds, density) {
        with(density) {
            val insetHorizontal = 12.dp.toPx()
            val insetVertical = 8.dp.toPx()
            Rect(
                left = rowBounds.left + insetHorizontal,
                top = rowBounds.top + insetVertical,
                right = rowBounds.right - insetHorizontal,
                bottom = rowBounds.bottom - insetVertical,
            )
        }
    }
    val actualEnabled = enabled && items.isNotEmpty()
    val actionColor = if (actualEnabled) {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    } else {
        MiuixTheme.colorScheme.disabledOnSecondaryVariant
    }

    val setExpanded: (Boolean) -> Unit = { next ->
        if (expanded != next) {
            expanded = next
            if (next) mounted.value = true
            currentOnExpandedChange?.invoke(next)
        }
    }

    BasicComponent(
        title = title,
        summary = summary,
        titleColor = titleColor,
        summaryColor = summaryColor,
        modifier = modifier.onGloballyPositioned { rowBounds = it.boundsInWindow() },
        startAction = startAction,
        endActions = {
            Row(
                modifier = Modifier.glassPopupAnchorValue(glassAnchor),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val value = items.getOrNull(selectedIndex)?.text

                if (!value.isNullOrEmpty()) {
                    Text(
                        text = value,
                        modifier = Modifier.padding(end = 8.dp).weight(1f, fill = false),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = actionColor,
                        textAlign = TextAlign.End,
                    )
                }
                DropdownArrowEndAction(actionColor = actionColor)
            }
        },
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        onClick = {
            if (actualEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                setExpanded(!expanded)
            }
        },
        role = Role.DropdownList,
        holdDownState = mounted.value,
        enabled = actualEnabled,
    )

    if (!mounted.value) return

    PopupLayout(
        visible = mounted,
        enableWindowDim = false,
        enableBackHandler = false,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        renderInRootScaffold = renderInRootScaffold,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassDropdownPopup(
                show = expanded,
                onDismissRequest = { setExpanded(false) },
                anchorBounds = popupAnchor,
                backdrop = hyperBackdrop,
                anchor = glassAnchor,
                sizing = sizing,
            ) {
                items.forEachIndexed { index, item ->
                    GlassPopupItem(
                        text = item.text,
                        summary = item.summary,
                        selected = index == selectedIndex,
                        enabled = item.enabled,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            if (collapseOnSelection) setExpanded(false)
                            item.onClick?.invoke()
                        },
                    )
                }
            }
        }
    }


    LaunchedEffect(expanded) {
        if (!expanded && mounted.value) {
            delay(260)
            mounted.value = false
        }
    }
}
