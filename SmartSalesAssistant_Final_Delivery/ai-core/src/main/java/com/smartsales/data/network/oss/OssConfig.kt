package com.smartsales.data.network.oss

import android.content.Context
import com.smartsales.data.network.AiApiConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OSS Configuration
 * Provides OSS credentials and settings from BuildConfig
 */
@Singleton
class OssConfig
    @Inject
    constructor(
        val context: Context,
    ) {
        val endpoint: String = AiApiConfig.OSS.ENDPOINT
        val bucket: String = AiApiConfig.OSS.BUCKET
        val accessKeyId: String = AiApiConfig.OSS.ACCESS_KEY_ID
        val accessKeySecret: String = AiApiConfig.OSS.ACCESS_KEY_SECRET
    }
