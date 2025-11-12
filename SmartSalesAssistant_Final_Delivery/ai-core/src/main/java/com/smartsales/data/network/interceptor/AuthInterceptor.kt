package com.smartsales.data.network.interceptor

import com.smartsales.data.network.AiApiConfig
import okhttp3.Interceptor
import okhttp3.Response

open class AuthInterceptor(
    private val apiKey: String,
    private val headerName: String = AiApiConfig.HEADER_AUTHORIZATION,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest =
            originalRequest.newBuilder()
                .header(headerName, AiApiConfig.buildBearerAuth(apiKey))
                .build()
        return chain.proceed(newRequest)
    }
}

class DashscopeAuthInterceptor(apiKey: String) : AuthInterceptor(apiKey)

class TingwuAuthInterceptor(apiKey: String) : AuthInterceptor(apiKey)
