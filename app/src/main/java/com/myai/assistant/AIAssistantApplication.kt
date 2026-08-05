package com.myai.assistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIAssistantApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize application components here
    }
}
