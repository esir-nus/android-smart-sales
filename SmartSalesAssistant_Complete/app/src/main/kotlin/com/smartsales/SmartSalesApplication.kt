package com.smartsales

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartSalesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any app-wide configurations here
        initializeQwenConfig()
    }
    
    private fun initializeQwenConfig() {
        // Load API keys from BuildConfig
        com.smartsales.data.remote.api.QwenConfig.dashscopeApiKey = BuildConfig.DASHSCOPE_API_KEY
        com.smartsales.data.remote.api.QwenConfig.tingwuApiKey = BuildConfig.TINGWU_API_KEY
    }
}
