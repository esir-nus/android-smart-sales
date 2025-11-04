package com.smartsales.data.remote.api

import com.smartsales.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for Smart Sales Gadget HTTP API
 */
interface GadgetApi {
    
    @GET("/api/files/list")
    suspend fun getFileList(): Response<FileListResponse>
    
    @GET("/api/files/download")
    @Streaming
    suspend fun downloadFile(
        @Query("filename") filename: String
    ): Response<ResponseBody>
    
    @Multipart
    @POST("/api/display/image")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<UploadResponse>
    
    @POST("/api/display/text")
    suspend fun updateDisplayText(
        @Body request: TextDisplayRequest
    ): Response<BaseResponse>
    
    @GET("/api/device/status")
    suspend fun getDeviceStatus(): Response<DeviceStatusResponse>
    
    @DELETE("/api/files/delete")
    suspend fun deleteFile(
        @Query("filename") filename: String
    ): Response<BaseResponse>
}
