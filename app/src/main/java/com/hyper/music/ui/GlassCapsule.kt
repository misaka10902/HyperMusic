package com.hyper.music.ui

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Stable
import top.yukonga.miuix.kmp.material.GlassShape


@Stable
fun GlassCapsule(): GlassShape = GlassShape(
    CornerSize(50),
    CornerSize(50),
    CornerSize(50),
    CornerSize(50),
)
