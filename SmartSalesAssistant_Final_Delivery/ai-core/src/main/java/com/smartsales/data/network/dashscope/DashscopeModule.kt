package com.smartsales.data.network.dashscope

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DashscopeModule {
    @Provides
    @Singleton
    fun provideDashscopeStreamingSdk(): MockDashscopeStreamingSdk {
        return MockDashscopeStreamingSdk()
    }

    @Provides
    @Singleton
    fun provideDashscopeChatClient(): DashscopeChatClient {
        return MockDashscopeChatClient()
    }
}
