package dev.nausheen.smartspend.data.network

import dev.nausheen.smartspend.data.model.ScanResult
import okhttp3.MultipartBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ScanApi {
    @Multipart
    @POST("scan")
    suspend fun scan(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part,
    ): ScanResult
}
