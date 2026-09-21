package dev.nausheen.smartspend.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nausheen.smartspend.data.ExpenseRepository
import dev.nausheen.smartspend.data.ScanRepository
import dev.nausheen.smartspend.data.SplitRepository
import dev.nausheen.smartspend.data.model.ExpenseIn
import dev.nausheen.smartspend.data.model.ScanResult
import dev.nausheen.smartspend.data.model.SplitIn
import dev.nausheen.smartspend.data.model.SplitMemberIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Success(val result: ScanResult) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel(
    private val repository: ScanRepository = ScanRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val splitRepository: SplitRepository = SplitRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    /** One-shot confirmation text shown after an add-to-expense / split action. */
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    /** True while an add-to-expense / split request is running, so the buttons
     *  can disable and a second tap can't create a duplicate record. */
    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    fun scan(context: Context, uri: Uri, accessToken: String?) {
        if (accessToken.isNullOrBlank()) {
            _state.value = ScanUiState.Error("Not signed in")
            return
        }
        _state.value = ScanUiState.Loading
        viewModelScope.launch {
            repository.scan(context, uri, accessToken)
                .onSuccess { _state.value = ScanUiState.Success(it) }
                .onFailure { _state.value = ScanUiState.Error(it.message ?: "Scan failed") }
        }
    }

    /** Turn the scanned receipt into a single expense (merchant + total + the
     *  most common item category). */
    fun addToExpenses(result: ScanResult, accessToken: String?) {
        if (accessToken.isNullOrBlank() || _actionInProgress.value) return
        val amount = result.totalAmount ?: result.items.sumOf { it.amount }
        val category = result.items.mapNotNull { it.category }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Other"
        val title = result.merchantName ?: "Scanned receipt"
        _actionInProgress.value = true
        viewModelScope.launch {
            expenseRepository.create(accessToken, ExpenseIn(title, amount, category, note = "From receipt scan"))
                .onSuccess { _actionMessage.value = "Added to expenses" }
                .onFailure { _actionMessage.value = "Could not add: ${it.message}" }
            _actionInProgress.value = false
        }
    }

    /** Create an even 2-way split from the receipt total, linked to the saved
     *  receipt row via receiptId. */
    fun splitReceipt(result: ScanResult, accessToken: String?) {
        if (accessToken.isNullOrBlank() || _actionInProgress.value) return
        val total = result.totalAmount ?: result.items.sumOf { it.amount }
        val share = total / 2
        val body = SplitIn(
            title = result.merchantName ?: "Scanned receipt",
            totalAmount = total,
            receiptId = result.receiptId,
            members = listOf(
                SplitMemberIn("Me", share),
                SplitMemberIn("Friend", share),
            ),
        )
        _actionInProgress.value = true
        viewModelScope.launch {
            splitRepository.create(accessToken, body)
                .onSuccess { _actionMessage.value = "Split created in Splits tab" }
                .onFailure { _actionMessage.value = "Could not split: ${it.message}" }
            _actionInProgress.value = false
        }
    }

    fun clearActionMessage() { _actionMessage.value = null }

    fun reset() {
        _state.value = ScanUiState.Idle
        _actionMessage.value = null
        _actionInProgress.value = false
    }
}
