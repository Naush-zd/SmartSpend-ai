package dev.nausheen.smartspend.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nausheen.smartspend.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Authenticated : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/** Whether a persisted session has been restored yet, so the UI can show a
 *  splash while Supabase loads the session from storage on launch. */
enum class SessionGate { Loading, SignedIn, SignedOut }

class AuthViewModel : ViewModel() {
    private val auth = SupabaseProvider.client.auth

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** Derived from Supabase's sessionStatus flow. Initializing -> Loading;
     *  Authenticated -> SignedIn; everything else -> SignedOut. */
    val gate: StateFlow<SessionGate> = auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> SessionGate.SignedIn
                is SessionStatus.Initializing -> SessionGate.Loading
                else -> SessionGate.SignedOut
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionGate.Loading)

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

    fun signOut() {
        viewModelScope.launch {
            runCatching { auth.signOut() }
            _state.value = AuthUiState.Idle
        }
    }
}
