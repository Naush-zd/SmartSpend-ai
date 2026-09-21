package dev.nausheen.smartspend.data

import android.content.Context
import android.net.Uri
import dev.nausheen.smartspend.data.model.ScanResult
import dev.nausheen.smartspend.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ScanRepository(
    private val api: dev.nausheen.smartspend.data.network.SmartSpendApi = NetworkModule.api,
) {
    /** Reads the image at [uri], sends it to the backend /scan endpoint. */
    suspend fun scan(context: Context, uri: Uri, accessToken: String): Result<ScanResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Could not read the selected image")
                val body = bytes.toRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "receipt.jpg", body)
                api.scan("Bearer $accessToken", part)
            }
        }
}
