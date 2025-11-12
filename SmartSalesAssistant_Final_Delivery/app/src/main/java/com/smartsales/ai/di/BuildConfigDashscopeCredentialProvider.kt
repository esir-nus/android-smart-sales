package com.smartsales.ai.di

import com.smartsales.BuildConfig
import com.smartsales.data.network.dashscope.DashscopeCredentialProvider
import com.smartsales.data.network.dashscope.DashscopeCredentials
import javax.inject.Inject

class BuildConfigDashscopeCredentialProvider
    @Inject
    constructor() : DashscopeCredentialProvider {
        override suspend fun getCredentials(): DashscopeCredentials {
            return DashscopeCredentials(
                appKey = BuildConfig.DASHSCOPE_APP_KEY,
                token = BuildConfig.DASHSCOPE_API_KEY,
            )
        }
    }
