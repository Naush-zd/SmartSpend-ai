package dev.nausheen.smartspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LineItem(
    val name: String,
    val amount: Double,
    val category: String? = null,
)

@Serializable
data class ScanResult(
    @SerialName("merchant_name") val merchantName: String? = null,
    @SerialName("receipt_date") val receiptDate: String? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    val currency: String = "INR",
    val items: List<LineItem> = emptyList(),
    @SerialName("is_anomaly") val isAnomaly: Boolean = false,
    @SerialName("anomaly_reason") val anomalyReason: String? = null,
)
