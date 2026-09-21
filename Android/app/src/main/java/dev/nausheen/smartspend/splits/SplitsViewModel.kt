package dev.nausheen.smartspend.splits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nausheen.smartspend.data.SplitRepository
import dev.nausheen.smartspend.data.model.Split
import dev.nausheen.smartspend.data.model.SplitIn
import dev.nausheen.smartspend.data.model.SplitMemberIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SplitsUiState(
    val loading: Boolean = false,
    val splits: List<Split> = emptyList(),
    val error: String? = null,
)

class SplitsViewModel(
    private val repository: SplitRepository = SplitRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(SplitsUiState())
    val state: StateFlow<SplitsUiState> = _state.asStateFlow()

    fun load(token: String?) {
        if (token == null) { _state.value = _state.value.copy(error = "Not signed in"); return }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.list(token)
                .onSuccess { _state.value = SplitsUiState(splits = it) }
                .onFailure { _state.value = SplitsUiState(error = it.message ?: "Failed to load") }
        }
    }

    fun create(token: String?, title: String, total: Double, members: List<SplitMemberIn>) {
        if (token == null) return
        viewModelScope.launch {
            repository.create(token, SplitIn(title = title, totalAmount = total, members = members))
                .onSuccess { load(token) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Failed to create") }
        }
    }

    fun markPaid(token: String?, memberId: String) {
        if (token == null) return
        viewModelScope.launch {
            repository.markPaid(token, memberId).onSuccess { load(token) }
        }
    }
}
