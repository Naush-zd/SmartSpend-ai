package dev.nausheen.smartspend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
    @SerialName("expense_date") val expenseDate: String? = null,
)

@Serializable
data class ExpenseIn(
    val title: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
)
