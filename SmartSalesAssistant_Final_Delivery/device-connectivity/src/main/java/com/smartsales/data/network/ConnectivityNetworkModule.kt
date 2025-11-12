package com.smartsales.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.smartsales.data.network.api.GadgetApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GadgetClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConnectivityGson

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityNetworkModule {
    @Provides
    @Singleton
    @ConnectivityGson
    fun provideConnectivityGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()
    }

    @Provides
    @Singleton
    @GadgetClient
    fun provideGadgetOkHttpClient(): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        return OkHttpClient.Builder()
            .connectTimeout(ConnectivityApiConfig.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(ConnectivityApiConfig.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(ConnectivityApiConfig.WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    @GadgetClient
    fun provideGadgetRetrofit(
        @GadgetClient okHttpClient: OkHttpClient,
        @ConnectivityGson gson: Gson,
    ): Retrofit {
        val defaultUrl = ConnectivityApiConfig.buildBaseUrl("192.168.1.1")

        return Retrofit.Builder()
            .baseUrl(defaultUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGadgetApi(
        @GadgetClient retrofit: Retrofit,
    ): GadgetApi {
        return retrofit.create(GadgetApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGadgetApiFactory(
        @GadgetClient okHttpClient: OkHttpClient,
        @ConnectivityGson gson: Gson,
    ): GadgetApiFactory {
        return GadgetApiFactory(okHttpClient, gson)
    }
}

class GadgetApiFactory
    @Inject
    constructor(
        @GadgetClient private val okHttpClient: OkHttpClient,
        @ConnectivityGson private val gson: Gson,
    ) {
        fun create(
            ipAddress: String,
            port: Int = ConnectivityApiConfig.DEFAULT_PORT,
        ): GadgetApi {
            val retrofit =
                Retrofit.Builder()
                    .baseUrl(ConnectivityApiConfig.buildBaseUrl(ipAddress, port))
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()

            return retrofit.create(GadgetApi::class.java)
        }
    }
