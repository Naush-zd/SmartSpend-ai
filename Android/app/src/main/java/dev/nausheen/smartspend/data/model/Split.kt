package dev.nausheen.smartspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SplitMember(
    val id: String,
    val name: String,
    @SerialName("amount_owed") val amountOwed: Double,
    @SerialName("is_paid") val isPaid: Boolean = false,
)

@Serializable
data class Split(
    val id: String,
    val title: String,
    @SerialName("total_amount") val totalAmount: Double,
    val settled: Boolean = false,
    @SerialName("split_members") val members: List<SplitMember> = emptyList(),
)

@Serializable
data class SplitMemberIn(
    val name: String,
    @SerialName("amount_owed") val amountOwed: Double,
)

@Serializable
data class SplitIn(
    val title: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("receipt_id") val receiptId: String? = null,
    val members: List<SplitMemberIn>,
)
