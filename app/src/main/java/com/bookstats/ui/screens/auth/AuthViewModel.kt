package com.bookstats.ui.screens.auth

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstats.data.repository.AuthRepository
import com.bookstats.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the auth screen.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSignUpMode: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * Events from the auth screen.
 */
sealed interface AuthEvent {
    data object NavigateToHome : AuthEvent
}

/**
 * ViewModel for the authentication screen.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()
    
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)
    
    init {
        // Navigate to home if already authenticated
        viewModelScope.launch {
            authRepository.authState.collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    _events.emit(AuthEvent.NavigateToHome)
                }
            }
        }
    }
    
    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }
    
    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }
    
    fun updateConfirmPassword(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }
    
    fun toggleMode() {
        _uiState.update { 
            it.copy(
                isSignUpMode = !it.isSignUpMode, 
                errorMessage = null,
                successMessage = null,
                confirmPassword = ""
            ) 
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun signInWithEmail() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }

        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.signInWithEmail(state.email, state.password)
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.NavigateToHome)
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = error.message ?: "Sign in failed"
                        ) 
                    }
                }
            )
        }
    }
    
    fun signUpWithEmail() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }

        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords don't match") }
            return
        }

        if (state.password.length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(errorMessage = "Password must be at least $MIN_PASSWORD_LENGTH characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.signUpWithEmail(state.email, state.password)
            
            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Check your email to confirm your account!",
                            isSignUpMode = false
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = error.message ?: "Sign up failed"
                        ) 
                    }
                }
            )
        }
    }
    
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.signInWithGoogle(activityContext)
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.NavigateToHome)
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = error.message ?: "Google sign in failed"
                        ) 
                    }
                }
            )
        }
    }
    
    fun resetPassword() {
        val email = _uiState.value.email

        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email to reset password") }
            return
        }

        if (!isValidEmail(email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.resetPassword(email)
            
            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Password reset email sent!"
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Password reset failed"
                        )
                    }
                }
            )
        }
    }

    /**
     * Validates email format using Android's built-in pattern matcher.
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
