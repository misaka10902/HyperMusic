package top.yukonga.miuix.kmp.interfaces

@Suppress("ExperimentalAnnotationRetention")
@RequiresOptIn(
    message = "This ScrollBar API is experimental. It has known issues and may change without notice.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
annotation class ExperimentalScrollBarApi
