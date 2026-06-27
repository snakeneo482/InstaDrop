package com.instadrop.app

import android.app.Application
import com.instadrop.app.data.ServiceLocator

/**
 * Application entry point. Wires up the [ServiceLocator] so the (single)
 * Activity / ViewModel can pull shared singletons without a DI framework.
 */
class InstaDropApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
