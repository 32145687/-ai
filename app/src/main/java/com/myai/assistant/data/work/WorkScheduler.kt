package com.myai.assistant.data.work

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager 配置和任务调度器
 * 负责初始化 WorkManager 并调度后台任务
 */
@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workerFactory: HiltWorkerFactory
) : Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * 初始化并启动所有周期性后台任务
     * 应在 Application onCreate 中调用
     */
    fun scheduleAllWorkers() {
        scheduleMemoryConsolidationWorker()
        // 未来可以添加其他后台任务
        // scheduleReminderNotificationWorker()
        // scheduleSyncWorker()
    }

    /**
     * 调度记忆巩固任务
     * 每天凌晨 2 点执行，需要网络连接（用于向量化）
     */
    private fun scheduleMemoryConsolidationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 需要网络调用 Embedding API
            .setRequiresBatteryNotLow(true) // 电量充足时执行
            .setRequiresCharging(false) // 不强制充电
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MemoryConsolidationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            initialDelay = 2, // 首次延迟 2 小时执行（凌晨 2 点）
            initialDelayTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "memory_consolidation_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * 取消所有后台任务
     * 用于用户注销或重置应用时
     */
    fun cancelAllWorkers() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
