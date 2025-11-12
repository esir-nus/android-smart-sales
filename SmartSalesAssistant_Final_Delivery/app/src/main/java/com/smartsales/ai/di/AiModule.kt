package com.smartsales.ai.di

import android.content.Context
import androidx.room.Room
import com.smartsales.ai.AiFeatureGraph
import com.smartsales.ai.DefaultAiFeatureGraph
import com.smartsales.ai.chat.AiChatService
import com.smartsales.ai.chat.impl.DashscopeAiChatService
import com.smartsales.ai.data.local.AiSessionDao
import com.smartsales.ai.data.local.AiSessionDatabase
import com.smartsales.ai.export.ExportManager
import com.smartsales.ai.export.SimpleExportManager
import com.smartsales.ai.history.AiSessionRepository
import com.smartsales.ai.history.impl.RoomAiSessionRepository
import com.smartsales.ai.media.DefaultMediaPreprocessor
import com.smartsales.ai.media.MediaPreprocessor
import com.smartsales.ai.oss.NoopOssRepository
import com.smartsales.ai.oss.OssRepository
import com.smartsales.ai.tingwu.NoopTingwuCoordinator
import com.smartsales.ai.tingwu.TingwuCoordinator
import com.smartsales.data.network.dashscope.DashscopeCredentialProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAiChatService(impl: DashscopeAiChatService): AiChatService

    @Binds
    @Singleton
    abstract fun bindAiSessionRepository(impl: RoomAiSessionRepository): AiSessionRepository

    @Binds
    @Singleton
    abstract fun bindExportManager(impl: SimpleExportManager): ExportManager

    @Binds
    @Singleton
    abstract fun bindMediaPreprocessor(impl: DefaultMediaPreprocessor): MediaPreprocessor

    @Binds
    @Singleton
    abstract fun bindTingwuCoordinator(impl: NoopTingwuCoordinator): TingwuCoordinator

    @Binds
    @Singleton
    abstract fun bindOssRepository(impl: NoopOssRepository): OssRepository

    @Binds
    @Singleton
    abstract fun bindDashscopeCredentialProvider(
        impl: BuildConfigDashscopeCredentialProvider,
    ): DashscopeCredentialProvider
}

@Module
@InstallIn(SingletonComponent::class)
object AiGraphModule {
    @Provides
    @Singleton
    fun provideAiFeatureGraph(
        chatService: AiChatService,
        sessionRepository: AiSessionRepository,
        exportManager: ExportManager,
        tingwuCoordinator: TingwuCoordinator,
        ossRepository: OssRepository,
    ): AiFeatureGraph {
        return DefaultAiFeatureGraph(
            chatService = chatService,
            sessionRepository = sessionRepository,
            exportManager = exportManager,
            tingwuCoordinator = tingwuCoordinator,
            ossRepository = ossRepository,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AiLocalModule {
    @Provides
    @Singleton
    fun provideAiSessionDatabase(
        @ApplicationContext context: Context,
    ): AiSessionDatabase {
        return Room.databaseBuilder(
            context,
            AiSessionDatabase::class.java,
            AiSessionDatabase.DATABASE_NAME,
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAiSessionDao(database: AiSessionDatabase): AiSessionDao = database.aiSessionDao()
}
