package com.paruyr.fluencytask

import android.app.Application
import com.paruyr.fluencytask.di.bluetoothModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FluencyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Start Koin for Dependency Injection
        startKoin {
            // Android context
            androidContext(this@FluencyApplication)
            // Modules to load
            modules(bluetoothModule)
        }
    }
}