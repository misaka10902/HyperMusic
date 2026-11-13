package com.hyper.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 歌曲条目的等宽分列行。
 * 每列 weight(1f) 均分；最后一行为奇数时用空白列占位，保证左右视觉对称。
 * columns = 1 时退化为原始单列满宽，手机端行为不变。
 */
@Composable
fun <T> SongPairRow(
    pair: List<T>,
    startIndex: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    content: @Composable (index: Int, item: T) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(columns) { col ->
            Box(modifier = Modifier.weight(1f)) {
                pair.getOrNull(col)?.let { item -> content(startIndex + col, item) }
            }
        }
    }
}
