package com.smartsales.data.network.oss

import android.content.Context
import com.smartsales.data.network.AiApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for OSS dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object OssModule {
    @Provides
    @Singleton
    fun provideOssConfig(
        @ApplicationContext context: Context,
    ): OssConfig {
        return OssConfig(context)
    }

    @Provides
    @Singleton
    fun provideOssManager(ossConfig: OssConfig): OssManager {
        return OssManager(ossConfig)
    }

    /**
     * Initialize OSS configuration from BuildConfig
     * Should be called in Application.onCreate()
     */
    fun initializeOssConfig(
        endpoint: String,
        bucket: String,
        accessKeyId: String,
        accessKeySecret: String,
    ) {
        AiApiConfig.OSS.ENDPOINT = endpoint
        AiApiConfig.OSS.BUCKET = bucket
        AiApiConfig.OSS.ACCESS_KEY_ID = accessKeyId
        AiApiConfig.OSS.ACCESS_KEY_SECRET = accessKeySecret
    }
}
