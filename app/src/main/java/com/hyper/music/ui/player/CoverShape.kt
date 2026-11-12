

package com.hyper.music.ui.player

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.squircle.squircleClip


internal val CoverRoundedCorner = 22.dp


internal val PlayPageCoverCorner = CoverRoundedCorner + 6.dp


internal const val CoverSizeFraction = 0.92f


internal val CoverCircleCorner = 999.dp


@Composable
internal fun Modifier.coverFrame(circular: Boolean, corner: Dp = CoverRoundedCorner): Modifier =
    if (circular) {
        this.clip(CircleShape)
    } else {
        this.squircleClip(corner)
    }
