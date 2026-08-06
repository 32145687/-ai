package com.myai.assistant

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.myai.assistant.data.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AIAssistantApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workScheduler: WorkScheduler

    override fun onCreate() {
        super.onCreate()
        
        // 初始化 WorkManager
        WorkManager.initialize(this, workScheduler.workManagerConfiguration)
        
        // 调度所有后台任务
        workScheduler.scheduleAllWorkers()
    }

    override val workManagerConfiguration: Configuration
        get() = workScheduler.workManagerConfiguration
}
