package com.smartsales.tingwutest

import com.smartsales.data.aicore.AiCoreConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 文件：tingwuTestApp/src/main/java/com/smartsales/tingwutest/TingwuTestAiCoreOverrides.kt
// 模块：:tingwuTestApp
// 说明：为Tingwu测试App提供真实HTTP配置
// 作者：创建于 2025-11-17
@Module
@InstallIn(SingletonComponent::class)
object TingwuTestAiCoreOverrides {
    @Provides
    @Singleton
    fun provideAiCoreConfig(): AiCoreConfig = AiCoreConfig(
        preferFakeAiChat = true,
        preferFakeTingwu = false,
        preferFakeExport = true,
        dashscopeEnableStreaming = false,
        dashscopeMaxRetries = 0,
        dashscopeRequestTimeoutMillis = 10_000,
        enableTingwuHttpLogging = true,
        tingwuVerboseLogging = true,
        tingwuPollIntervalMillis = 2_000,
        tingwuPollTimeoutMillis = 120_000
    )
}
