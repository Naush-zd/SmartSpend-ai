package dev.nausheen.smartspend.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nausheen.smartspend.data.ExpenseRepository
import dev.nausheen.smartspend.data.model.Expense
import dev.nausheen.smartspend.data.model.ExpenseIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val loading: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val error: String? = null,
)

class ExpensesViewModel(
    private val repository: ExpenseRepository = ExpenseRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(ExpensesUiState())
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    fun load(token: String?) {
        if (token == null) { _state.value = _state.value.copy(error = "Not signed in"); return }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.list(token)
                .onSuccess { _state.value = ExpensesUiState(expenses = it) }
                .onFailure { _state.value = ExpensesUiState(error = it.message ?: "Failed to load") }
        }
    }

    fun add(token: String?, title: String, amount: Double, category: String, note: String?) {
        if (token == null) return
        viewModelScope.launch {
            repository.create(token, ExpenseIn(title, amount, category, note))
                .onSuccess { load(token) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Failed to add") }
        }
    }

    fun delete(token: String?, id: String) {
        if (token == null) return
        viewModelScope.launch {
            repository.delete(token, id).onSuccess { load(token) }
        }
    }
}
