package com.example.smartpark.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Represents the different states the authentication UI can be in.
 * Sealed class = finite set of possible states (the compiler enforces we handle them all).
 */
sealed class AuthUiState {
    object Idle : AuthUiState()                     // Nothing happening yet
    object Loading : AuthUiState()                  // Waiting for Firebase response
    data class Authenticated(val user: FirebaseUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Private mutable state, exposed as read-only StateFlow to the UI
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Check if a user is already logged in when the app starts
    init {
        auth.currentUser?.let { user ->
            _uiState.value = AuthUiState.Authenticated(user)
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                } ?: run {
                    _uiState.value = AuthUiState.Error("Login failed: no user returned")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown login error")
            }
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.let { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                } ?: run {
                    _uiState.value = AuthUiState.Error("Registration failed: no user returned")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown registration error")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _uiState.value = AuthUiState.Idle
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}