package com.smartsales.data.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.smartsales.data.network.api.DashscopeApi
import com.smartsales.data.network.api.TingwuApi
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

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DashscopeClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TingwuClient

@Module
@InstallIn(SingletonComponent::class)
object AiNetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    @DashscopeClient
    fun provideDashscopeOkHttpClient(
        @ApplicationContext context: Context,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val apiKey = getApiKey(context, "DASHSCOPE_API_KEY")

        return OkHttpClient.Builder()
            .connectTimeout(AiApiConfig.DASHCOPE_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(AiApiConfig.DASHCOPE_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(AiApiConfig.DASHCOPE_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(DashscopeAuthInterceptor(apiKey))
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    @TingwuClient
    fun provideTingwuOkHttpClient(
        @ApplicationContext context: Context,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val apiKey = getApiKey(context, "TINGWU_API_KEY")

        return OkHttpClient.Builder()
            .connectTimeout(AiApiConfig.TINGWU_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(AiApiConfig.TINGWU_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(AiApiConfig.TINGWU_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(TingwuAuthInterceptor(apiKey))
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    @DashscopeClient
    fun provideDashscopeRetrofit(
        @DashscopeClient okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AiApiConfig.DASHSCOPE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @TingwuClient
    fun provideTingwuRetrofit(
        @TingwuClient okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AiApiConfig.TINGWU_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

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

    private fun getApiKey(context: Context, keyName: String): String {
        return try {
            val buildConfigClass = Class.forName("${context.packageName}.BuildConfig")
            val field = buildConfigClass.getField(keyName)
            val value = field.get(null) as? String
            if (!value.isNullOrBlank()) value else ""
        } catch (ignored: Exception) {
            ""
        }
    }
}
