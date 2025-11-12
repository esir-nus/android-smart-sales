package com.smartsales.aitest.di

import com.smartsales.data.network.dashscope.DashscopeCredentialProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiTestDashscopeModule {
    @Binds
    @Singleton
    abstract fun bindDashscopeCredentialProvider(
        impl: BuildConfigDashscopeCredentialProvider,
    ): DashscopeCredentialProvider
}
