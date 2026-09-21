package dev.nausheen.smartspend.data

import dev.nausheen.smartspend.data.model.Split
import dev.nausheen.smartspend.data.model.SplitIn
import dev.nausheen.smartspend.data.network.NetworkModule
import dev.nausheen.smartspend.data.network.SmartSpendApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SplitRepository(
    private val api: SmartSpendApi = NetworkModule.api,
) {
    suspend fun list(token: String): Result<List<Split>> =
        withContext(Dispatchers.IO) { runCatching { api.listSplits("Bearer $token") } }

    suspend fun create(token: String, body: SplitIn): Result<Split> =
        withContext(Dispatchers.IO) { runCatching { api.createSplit("Bearer $token", body) } }

    suspend fun markPaid(token: String, memberId: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.markMemberPaid("Bearer $token", memberId); Unit } }
}
