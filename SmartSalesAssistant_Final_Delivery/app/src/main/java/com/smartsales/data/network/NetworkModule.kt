package com.smartsales.data.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.smartsales.data.network.api.DashscopeApi
import com.smartsales.data.network.api.GadgetApi
import com.smartsales.data.network.api.TingwuApi
import com.smartsales.data.network.interceptor.CustomLoggingInterceptor
import com.smartsales.data.network.interceptor.DashscopeAuthInterceptor
import com.smartsales.data.network.interceptor.TingwuAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifiers for different API clients
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DashscopeClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TingwuClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GadgetClient

/**
 * Network Module
 * 
 * Provides all network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    // ===== GSON =====
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()
    }
    
    // ===== LOGGING INTERCEPTOR =====
    
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    @Provides
    @Singleton
    fun provideCustomLoggingInterceptor(): CustomLoggingInterceptor {
        return CustomLoggingInterceptor()
    }
    
    // ===== OKHTTP CLIENTS =====
    
    /**
     * Base OkHttp Client
     */
    private fun createBaseOkHttpClient(
        loggingInterceptor: CustomLoggingInterceptor,
        connectTimeout: Long = ApiConfig.CONNECT_TIMEOUT,
        readTimeout: Long = ApiConfig.READ_TIMEOUT,
        writeTimeout: Long = ApiConfig.WRITE_TIMEOUT
    ): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
    }
    
    /**
     * Dashscope OkHttp Client
     */
    @Provides
    @Singleton
    @DashscopeClient
    fun provideDashscopeOkHttpClient(
        @ApplicationContext context: Context,
        loggingInterceptor: CustomLoggingInterceptor
    ): OkHttpClient {
        // Get API key from local.properties or BuildConfig
        val apiKey = getApiKey(context, "DASHSCOPE_API_KEY")
        
        return createBaseOkHttpClient(loggingInterceptor)
            .addInterceptor(DashscopeAuthInterceptor(apiKey))
            .build()
    }
    
    /**
     * Tingwu OkHttp Client
     */
    @Provides
    @Singleton
    @TingwuClient
    fun provideTingwuOkHttpClient(
        @ApplicationContext context: Context,
        loggingInterceptor: CustomLoggingInterceptor
    ): OkHttpClient {
        // Get API key from local.properties or BuildConfig
        val apiKey = getApiKey(context, "TINGWU_API_KEY")
        
        return createBaseOkHttpClient(loggingInterceptor)
            .addInterceptor(TingwuAuthInterceptor(apiKey))
            .build()
    }
    
    /**
     * Gadget OkHttp Client (No auth needed)
     */
    @Provides
    @Singleton
    @GadgetClient
    fun provideGadgetOkHttpClient(
        loggingInterceptor: CustomLoggingInterceptor
    ): OkHttpClient {
        return createBaseOkHttpClient(
            loggingInterceptor,
            connectTimeout = 10_000L,
            readTimeout = 30_000L,
            writeTimeout = 30_000L
        ).build()
    }
    
    // ===== RETROFIT INSTANCES =====
    
    /**
     * Dashscope Retrofit
     */
    @Provides
    @Singleton
    @DashscopeClient
    fun provideDashscopeRetrofit(
        @DashscopeClient okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.DASHSCOPE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Tingwu Retrofit
     */
    @Provides
    @Singleton
    @TingwuClient
    fun provideTingwuRetrofit(
        @TingwuClient okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.TINGWU_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Gadget Retrofit (Dynamic base URL)
     * 
     * Note: Base URL will be updated at runtime when device IP is discovered
     */
    @Provides
    @Singleton
    @GadgetClient
    fun provideGadgetRetrofit(
        @GadgetClient okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        // Default URL - will be updated when connecting to device
        val defaultUrl = ApiConfig.getGadgetBaseUrl("192.168.1.1")
        
        return Retrofit.Builder()
            .baseUrl(defaultUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    // ===== API SERVICES =====
    
    @Provides
    @Singleton
    fun provideDashscopeApi(
        @DashscopeClient retrofit: Retrofit
    ): DashscopeApi {
        return retrofit.create(DashscopeApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideTingwuApi(
        @TingwuClient retrofit: Retrofit
    ): TingwuApi {
        return retrofit.create(TingwuApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideGadgetApi(
        @GadgetClient retrofit: Retrofit
    ): GadgetApi {
        return retrofit.create(GadgetApi::class.java)
    }
    
    // ===== HELPER FUNCTIONS =====
    
    /**
     * Get API key from resources or BuildConfig
     * 
     * Priority:
     * 1. BuildConfig (generated from local.properties)
     * 2. Hardcoded default (for testing only)
     */
    private fun getApiKey(context: Context, keyName: String): String {
        // Try to get from BuildConfig
        try {
            val buildConfigClass = Class.forName("${context.packageName}.BuildConfig")
            val field = buildConfigClass.getField(keyName)
            val value = field.get(null) as? String
            if (!value.isNullOrBlank()) {
                return value
            }
        } catch (e: Exception) {
            // BuildConfig field not found
        }
        
        // Fallback to empty string (will cause auth failure - user needs to add key)
        return ""
    }
}

/**
 * Gadget API Factory
 * 
 * Creates Gadget API instance with custom IP address
 */
class GadgetApiFactory @javax.inject.Inject constructor(
    @GadgetClient private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    /**
     * Create Gadget API for specific IP address
     */
    fun createApi(ipAddress: String, port: Int = ApiConfig.GADGET_DEFAULT_PORT): GadgetApi {
        val baseUrl = ApiConfig.getGadgetBaseUrl(ipAddress, port)
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        return retrofit.create(GadgetApi::class.java)
    }
}