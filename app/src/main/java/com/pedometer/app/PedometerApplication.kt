package com.pedometer.app

import android.app.Application
import com.pedometer.app.di.AppGraph

class PedometerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Build the dependency graph once, up front; service and UI rely on it.
        AppGraph.init(this)
    }
}
