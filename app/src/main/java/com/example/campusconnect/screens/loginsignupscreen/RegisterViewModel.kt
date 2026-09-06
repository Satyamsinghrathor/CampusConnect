package com.example.campusconnect.screens.loginsignupscreen


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.auth.firebase.customauth.Register
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val error: String? = null
)

class RegisterViewModel : ViewModel() {

    private val register = Register()

    private val _uiState = MutableStateFlow(RegisterUiState())

    val uiState: StateFlow<RegisterUiState> =
        _uiState.asStateFlow()

    fun register(
        email: String,
        password: String,
        confirmPassword: String
    ) {

        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Email cannot be empty"
            )
            return
        }

        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Password cannot be empty"
            )
            return
        }

        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                error = "Passwords do not match"
            )
            return
        }

        viewModelScope.launch {

            _uiState.value = RegisterUiState(
                isLoading = true
            )

            val result = register.register(
                email = email,
                password = password
            )

            result
                .onSuccess {
                    _uiState.value = RegisterUiState(
                        isRegistered = true
                    )
                }
                .onFailure { exception ->

                    _uiState.value = RegisterUiState(
                        isLoading = false,
                        error = getFirebaseErrorMessage(exception)
                    )
                }
        }
    }

    private fun getFirebaseErrorMessage(
        exception: Throwable
    ): String {

        return when {
            exception.message?.contains(
                "already in use",
                ignoreCase = true
            ) == true -> {
                "An account with this email already exists"
            }

            exception.message?.contains(
                "badly formatted",
                ignoreCase = true
            ) == true -> {
                "Invalid email address"
            }

            exception.message?.contains(
                "password",
                ignoreCase = true
            ) == true -> {
                "Password must be at least 6 characters"
            }

            else -> {
                exception.message ?: "Registration failed"
            }
        }
    }
}