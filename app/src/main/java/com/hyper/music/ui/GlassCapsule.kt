package com.hyper.music.ui

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.material.GlassShape

/**
 * 轻量胶囊玻璃形状（自研保留组件）。
 *
 * miuix 0.9.5 起 GlassShape 由 glass 包移入 material 包，且不再内置 Capsule 便捷函数，
 * 这里用官方 GlassShape 以四角 50% 的圆角实现等价的胶囊形状，避免引入额外冗余依赖。
 */
@Stable
fun GlassCapsule(): Shape = GlassShape(
    CornerSize(50),
    CornerSize(50),
    CornerSize(50),
    CornerSize(50),
)
