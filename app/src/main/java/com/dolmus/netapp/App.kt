package com.dolmus.netapp

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(applicationContext)
    }
}