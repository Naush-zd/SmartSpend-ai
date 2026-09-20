package dev.nausheen.smartspend.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nausheen.smartspend.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Authenticated : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {
    private val auth = SupabaseProvider.client.auth

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) = run(email, password, signUp = false)
    fun signUp(email: String, password: String) = run(email, password, signUp = true)

    private fun run(email: String, password: String, signUp: Boolean) {
        if (email.isBlank() || password.length < 6) {
            _state.value = AuthUiState.Error("Enter a valid email and a 6+ char password")
            return
        }
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                if (signUp) {
                    auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                } else {
                    auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
                _state.value = AuthUiState.Authenticated
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun accessToken(): String? = auth.currentAccessTokenOrNull()
}
