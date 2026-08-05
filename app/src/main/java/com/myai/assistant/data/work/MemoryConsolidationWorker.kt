package com.myai.assistant.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myai.assistant.domain.service.MemoryConsolidationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 记忆巩固后台任务
 * 使用 WorkManager 定期执行记忆清理、合并和衰减
 */
@HiltWorker
class MemoryConsolidationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val memoryConsolidationService: MemoryConsolidationService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            memoryConsolidationService.runConsolidation()
            Result.success()
        } catch (e: Exception) {
            // 失败后指数退避重试
            Result.retry()
        }
    }
}
