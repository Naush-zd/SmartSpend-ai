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
        val showLoader = _state.value.splits.isEmpty()
        _state.value = _state.value.copy(loading = showLoader, error = null)
        viewModelScope.launch {
            repository.list(token)
                .onSuccess { _state.value = SplitsUiState(splits = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Failed to load") }
        }
    }

    fun create(token: String?, title: String, total: Double, members: List<SplitMemberIn>) {
        if (token == null) return
        viewModelScope.launch {
            repository.create(token, SplitIn(title = title, totalAmount = total, members = members))
                .onSuccess { created -> _state.value = _state.value.copy(splits = listOf(created) + _state.value.splits) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Failed to create") }
        }
    }

    fun markPaid(token: String?, memberId: String) {
        if (token == null) return
        // Update the member in place; no loader, no full refresh.
        _state.value = _state.value.copy(
            splits = _state.value.splits.map { split ->
                split.copy(members = split.members.map { m ->
                    if (m.id == memberId) m.copy(isPaid = true) else m
                })
            },
        )
        viewModelScope.launch {
            repository.markPaid(token, memberId)
        }
    }
}
