package com.hyper.music.util

import android.content.Context
import java.io.File

object CrashLogger {

    private const val FILE_NAME = "crash.log"

    @Volatile
    var lastCrash: String = ""
        private set

    fun install(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            lastCrash = runCatching { file.readText() }.getOrDefault("")
            runCatching { file.delete() }
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                file.writeText(
                    buildString {
                        appendLine("thread: ${thread.name}")
                        appendLine(throwable.toString())
                        throwable.stackTrace.take(45).forEach { appendLine("    at $it") }
                        var cause = throwable.cause
                        var depth = 0
                        while (cause != null && depth < 4) {
                            appendLine("caused by: $cause")
                            cause.stackTrace.take(25).forEach { appendLine("    at $it") }
                            cause = cause.cause
                            depth++
                        }
                    }.take(20_000)
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
