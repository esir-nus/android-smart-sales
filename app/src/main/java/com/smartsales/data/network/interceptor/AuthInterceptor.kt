package com.smartsales.data.network.interceptor

import com.smartsales.data.network.ApiConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Authentication Interceptor
 * 
 * Adds API key to requests automatically
 */
class AuthInterceptor(
    private val apiKey: String,
    private val headerName: String = "Authorization"
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Build new request with auth header
        val newRequest = originalRequest.newBuilder()
            .header(headerName, "Bearer $apiKey")
            .build()
        
        return chain.proceed(newRequest)
    }
}

/**
 * Dashscope Auth Interceptor
 */
class DashscopeAuthInterceptor(apiKey: String) : AuthInterceptor(
    apiKey = apiKey,
    headerName = ApiConfig.HEADER_AUTHORIZATION
)

/**
 * Tingwu Auth Interceptor
 */
class TingwuAuthInterceptor(apiKey: String) : AuthInterceptor(
    apiKey = apiKey,
    headerName = ApiConfig.HEADER_AUTHORIZATION
)