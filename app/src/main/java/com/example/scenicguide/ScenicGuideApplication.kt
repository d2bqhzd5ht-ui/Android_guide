package com.example.scenicguide

import android.app.Application
import com.example.scenicguide.core.di.AppContainer

class ScenicGuideApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
