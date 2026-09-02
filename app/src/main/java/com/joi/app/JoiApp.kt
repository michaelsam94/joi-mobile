package com.joi.app

import android.app.Application
import com.joi.app.di.AppContainer

class JoiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
