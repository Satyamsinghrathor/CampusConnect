package com.example.campusconnect.screens.loginsignupscreen


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.auth.firebase.customauth.Login
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

class LoginViewModel : ViewModel() {

    private val login = Login()

    private val _uiState = MutableStateFlow(
        LoginUiState(
            isLoggedIn = login.isLoggedIn()
        )
    )

    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(
        email: String,
        password: String
    ) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Email and password cannot be empty"
            )
            return
        }

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = login.login(email, password)

            result
                .onSuccess {
                    _uiState.value = LoginUiState(
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun loginWithGoogle(idToken: String) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = login.loginWithGoogle(idToken)

            result
                .onSuccess {
                    _uiState.value = LoginUiState(
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }
}