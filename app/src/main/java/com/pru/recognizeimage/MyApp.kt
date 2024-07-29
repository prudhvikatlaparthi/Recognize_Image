package com.pru.recognizeimage

import android.app.Application
import android.content.Context

private var appContext_: Context? = null
val appContext: Context
    get() = appContext_ ?: throw IllegalStateException(
        "Application context not initialized yet."
    )
class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        appContext_ = this
    }
}
