package com.myai.assistant.di

import android.content.Context
import androidx.room.Room
import com.myai.assistant.data.local.database.AppDatabase
import com.myai.assistant.data.manager.SettingsManager
import com.myai.assistant.data.remote.api.LlmApiService
import com.myai.assistant.domain.service.AiModelProvider
import com.myai.assistant.domain.service.CloudAiModelProvider
import com.myai.assistant.domain.service.EmbeddingService
import com.myai.assistant.domain.service.MemoryConsolidationService
import com.myai.assistant.domain.service.MemoryExtractionService
import com.myai.assistant.domain.service.RagService
import com.myai.assistant.domain.service.VectorSearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/") // Default base URL, can be changed per request
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLlmApiService(retrofit: Retrofit): LlmApiService {
        return retrofit.create(LlmApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development; use proper migrations in production
            .build()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun providePersonaDao(database: AppDatabase) = database.personaDao()

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase) = database.messageDao()

    @Provides
    @Singleton
    fun provideMemoryDao(database: AppDatabase) = database.memoryDao()

    @Provides
    @Singleton
    fun provideReminderDao(database: AppDatabase) = database.reminderDao()

    @Provides
    @Singleton
    fun provideUserBehaviorPatternDao(database: AppDatabase) = database.userBehaviorPatternDao()

    // ==================== 记忆系统服务 ====================

    @Provides
    @Singleton
    fun provideEmbeddingService(
        apiService: LlmApiService,
        settingsManager: SettingsManager
    ): EmbeddingService {
        return EmbeddingService(apiService, settingsManager)
    }

    @Provides
    @Singleton
    fun provideVectorSearchService(
        memoryDao: com.myai.assistant.data.local.dao.MemoryDao,
        embeddingService: EmbeddingService
    ): VectorSearchService {
        return VectorSearchService(memoryDao, embeddingService)
    }

    @Provides
    @Singleton
    fun provideRagService(
        vectorSearchService: VectorSearchService,
        memoryDao: com.myai.assistant.data.local.dao.MemoryDao
    ): RagService {
        return RagService(vectorSearchService, memoryDao)
    }

    @Provides
    @Singleton
    fun provideAiModelProvider(
        apiService: LlmApiService,
        settingsManager: SettingsManager,
        ragService: RagService
    ): AiModelProvider {
        return CloudAiModelProvider(apiService, settingsManager, ragService)
    }

    @Provides
    @Singleton
    fun provideMemoryExtractionService(
        aiModelProvider: AiModelProvider
    ): MemoryExtractionService {
        return MemoryExtractionService(aiModelProvider)
    }

    @Provides
    @Singleton
    fun provideMemoryConsolidationService(
        memoryDao: com.myai.assistant.data.local.dao.MemoryDao,
        vectorSearchService: VectorSearchService
    ): MemoryConsolidationService {
        return MemoryConsolidationService(memoryDao, vectorSearchService)
    }
}
