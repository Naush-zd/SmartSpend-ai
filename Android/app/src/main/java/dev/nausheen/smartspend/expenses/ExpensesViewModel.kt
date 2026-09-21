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
        // Only show the full-screen loader on the first load (empty list). A
        // silent refresh keeps the current list visible while re-fetching.
        val showLoader = _state.value.expenses.isEmpty()
        _state.value = _state.value.copy(loading = showLoader, error = null)
        viewModelScope.launch {
            repository.list(token)
                .onSuccess { _state.value = ExpensesUiState(expenses = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Failed to load") }
        }
    }

    fun add(token: String?, title: String, amount: Double, category: String, note: String?) {
        if (token == null) return
        viewModelScope.launch {
            repository.create(token, ExpenseIn(title, amount, category, note))
                .onSuccess { created -> _state.value = _state.value.copy(expenses = listOf(created) + _state.value.expenses) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Failed to add") }
        }
    }

    fun delete(token: String?, id: String) {
        if (token == null) return
        // Optimistically drop the row; no loader, no full refresh.
        _state.value = _state.value.copy(expenses = _state.value.expenses.filterNot { it.id == id })
        viewModelScope.launch {
            repository.delete(token, id)
        }
    }
}
