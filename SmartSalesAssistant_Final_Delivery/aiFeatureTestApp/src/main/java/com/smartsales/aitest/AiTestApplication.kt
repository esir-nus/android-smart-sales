package com.smartsales.aitest

import android.app.Application
import com.smartsales.data.network.oss.OssModule
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize OSS configuration from BuildConfig
        OssModule.initializeOssConfig(
            endpoint = BuildConfig.ALI_OSS_ENDPOINT,
            bucket = BuildConfig.ALI_OSS_BUCKET,
            accessKeyId = BuildConfig.ALI_OSS_ACCESS_KEY_ID,
            accessKeySecret = BuildConfig.ALI_OSS_ACCESS_KEY_SECRET,
        )
    }
}
