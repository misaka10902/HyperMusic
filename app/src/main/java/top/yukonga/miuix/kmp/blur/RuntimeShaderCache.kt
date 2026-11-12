


package top.yukonga.miuix.kmp.blur

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized


sealed interface RuntimeShaderCache {


    fun obtainRuntimeShader(key: String, string: String): RuntimeShader
}


val LocalRuntimeShaderCache = staticCompositionLocalOf<RuntimeShaderCache> {
    RuntimeShaderCacheImpl()
}

@OptIn(InternalCoroutinesApi::class)
internal class RuntimeShaderCacheImpl : RuntimeShaderCache {

    private val lock = SynchronizedObject()
    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader = synchronized(lock) {
        runtimeShaders.getOrPut(key) { RuntimeShader(string) }
    }
}
