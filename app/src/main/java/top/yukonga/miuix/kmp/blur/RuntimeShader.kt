


@file:Suppress("FunctionName")

package top.yukonga.miuix.kmp.blur

import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import top.yukonga.miuix.kmp.shader.asBrush as coreAsBrush
import top.yukonga.miuix.kmp.shader.asComposeShader as coreAsComposeShader
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported as coreIsRuntimeShaderSupported


typealias RuntimeShader = top.yukonga.miuix.kmp.shader.RuntimeShader


fun RuntimeShader(shaderString: String): RuntimeShader = top.yukonga.miuix.kmp.shader.RuntimeShader(shaderString)


fun RuntimeShader.asComposeShader(): Shader = coreAsComposeShader()


fun RuntimeShader.asBrush(): ShaderBrush = coreAsBrush()


fun isRuntimeShaderSupported(): Boolean = coreIsRuntimeShaderSupported()
