package dev.nausheen.smartspend.data

import dev.nausheen.smartspend.data.model.Expense
import dev.nausheen.smartspend.data.model.ExpenseIn
import dev.nausheen.smartspend.data.network.NetworkModule
import dev.nausheen.smartspend.data.network.SmartSpendApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExpenseRepository(
    private val api: SmartSpendApi = NetworkModule.api,
) {
    suspend fun list(token: String): Result<List<Expense>> =
        withContext(Dispatchers.IO) { runCatching { api.listExpenses("Bearer $token") } }

    suspend fun create(token: String, body: ExpenseIn): Result<Expense> =
        withContext(Dispatchers.IO) { runCatching { api.createExpense("Bearer $token", body) } }

    suspend fun delete(token: String, id: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.deleteExpense("Bearer $token", id) } }
}
