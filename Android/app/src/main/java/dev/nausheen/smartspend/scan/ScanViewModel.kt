package dev.nausheen.smartspend.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nausheen.smartspend.data.ScanRepository
import dev.nausheen.smartspend.data.model.ScanResult
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
) : ViewModel() {

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

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

    fun reset() { _state.value = ScanUiState.Idle }
}
