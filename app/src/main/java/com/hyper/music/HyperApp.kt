package com.hyper.music

import android.app.Application
import com.hyper.music.music.ApiBridge
import com.hyper.music.music.AudioFocusMonitor
import com.hyper.music.music.MusicLibrary

class HyperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        MusicLibrary.init(this)
        AudioFocusMonitor.init(this)
        ApiBridge.init(this)
    }

    companion object {
        lateinit var instance: HyperApp
            private set
    }
}
