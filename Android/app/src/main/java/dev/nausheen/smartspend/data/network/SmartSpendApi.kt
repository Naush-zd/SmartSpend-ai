package dev.nausheen.smartspend.data.network

import dev.nausheen.smartspend.data.model.Expense
import dev.nausheen.smartspend.data.model.ExpenseIn
import dev.nausheen.smartspend.data.model.ScanResult
import dev.nausheen.smartspend.data.model.Split
import dev.nausheen.smartspend.data.model.SplitIn
import dev.nausheen.smartspend.data.model.SplitMember
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface SmartSpendApi {
    @Multipart
    @POST("scan")
    suspend fun scan(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part,
    ): ScanResult

    // expenses
    @GET("expenses")
    suspend fun listExpenses(@Header("Authorization") bearer: String): List<Expense>

    @POST("expenses")
    suspend fun createExpense(
        @Header("Authorization") bearer: String,
        @Body body: ExpenseIn,
    ): Expense

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
    )

    // splits
    @GET("splits")
    suspend fun listSplits(@Header("Authorization") bearer: String): List<Split>

    @POST("splits")
    suspend fun createSplit(
        @Header("Authorization") bearer: String,
        @Body body: SplitIn,
    ): Split

    @PATCH("splits/members/{id}/paid")
    suspend fun markMemberPaid(
        @Header("Authorization") bearer: String,
        @Path("id") memberId: String,
    ): SplitMember
}
