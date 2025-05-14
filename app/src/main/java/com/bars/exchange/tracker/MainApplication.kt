package com.bars.exchange.tracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    // You can add Timber initialization or other app-wide setup here if needed
    override fun onCreate() {
        super.onCreate()
        // Example: if (BuildConfig.DEBUG) { Timber.plant(Timber.DebugTree()) }
    }
}
