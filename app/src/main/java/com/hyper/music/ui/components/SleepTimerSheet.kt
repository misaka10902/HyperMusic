package com.hyper.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.music.SleepTimer
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.glass.GlassShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.text.font.FontWeight
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween

@Composable
fun SleepTimerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(10) }
    var seconds by remember { mutableIntStateOf(0) }
    var extendToEnd by remember { mutableStateOf(false) }

    val timerState by SleepTimer.state.collectAsState()
    val active = timerState.active
    val remainingMs = timerState.remainingMs

    val collapse by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "sleepTimerCollapse",
    )
    val expandedHeight = NumberPickerDefaults.ItemHeight * 3 + 20.dp
    val collapsedHeight = 64.dp
    val cardHeight = expandedHeight + (collapsedHeight - expandedHeight) * collapse
    val pickerAlpha = (1f - collapse / 0.2f).coerceIn(0f, 1f)
    val countDownAlpha = 1f - pickerAlpha

    HyperSheet(visible = visible, onDismiss = onDismiss, title = "睡眠定时", immersive = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(GlassShape(28.dp))
                    .background(scheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .graphicsLayer { alpha = pickerAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TimeColumn(value = hours, range = 0..23, enabled = !active, onValueChange = { hours = it }, modifier = Modifier.weight(1f))
                    Colon()
                    TimeColumn(value = minutes, range = 0..59, enabled = !active, onValueChange = { minutes = it }, modifier = Modifier.weight(1f))
                    Colon()
                    TimeColumn(value = seconds, range = 0..59, enabled = !active, onValueChange = { seconds = it }, modifier = Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = countDownAlpha },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatSleepTime(remainingMs),
                        color = scheme.onSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassShape(20.dp))
                    .background(scheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "延长到单首歌曲播完",
                    color = scheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = extendToEnd, onCheckedChange = { extendToEnd = it })
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                text = if (active) "停止" else "开始",
                onClick = {
                    if (active) {
                        SleepTimer.cancel()
                    } else {
                        SleepTimer.start(hours, minutes, seconds, extendToEnd)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

private fun formatSleepTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L).toInt()
    val h = totalSec / 3600
    val m = totalSec / 60 % 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
private fun TimeColumn(
    value: Int,
    range: IntRange,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NumberPicker(
        value = value,
        onValueChange = onValueChange,
        range = range,
        visibleItemCount = 3,
        wrapAround = true,
        enabled = enabled,
        label = { "%02d".format(it) },
        modifier = modifier,
    )
}

@Composable
private fun Colon() {
    Text(
        text = ":",
        color = MiuixTheme.colorScheme.onSurface,
        fontSize = 28.sp,
        modifier = Modifier.width(16.dp),
        textAlign = TextAlign.Center,
    )
}
