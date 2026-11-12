package com.hyper.music

import android.app.Application
import com.hyper.music.music.ApiBridge
import com.hyper.music.music.MusicLibrary
import com.hyper.music.util.CrashLogger

class HyperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashLogger.install(this)
        MusicLibrary.init(this)
        ApiBridge.init(this)
    }

    companion object {
        lateinit var instance: HyperApp
            private set
    }
}
