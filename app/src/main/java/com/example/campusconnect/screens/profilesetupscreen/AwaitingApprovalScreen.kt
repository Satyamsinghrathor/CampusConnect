package com.example.campusconnect.screens.profilesetupscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect.data.model.ApprovalStatus
import com.example.campusconnect.data.model.UserProfile
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AwaitingApprovalUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val hasError: Boolean = false
)

class AwaitingApprovalViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(AwaitingApprovalUiState())
    val uiState: StateFlow<AwaitingApprovalUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            userRepository.getProfile(uid)
                .onSuccess { profile ->
                    _uiState.update { it.copy(isLoading = false, profile = profile) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}

/** Shown to teachers, staff, HODs and admins until their account is approved. */
@Composable
fun AwaitingApprovalScreen(
    onApproved: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AwaitingApprovalViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile

    LaunchedEffect(profile?.status) {
        if (profile?.status == ApprovalStatus.ACTIVE) onApproved()
    }

    val rejected = profile?.status == ApprovalStatus.REJECTED
    val role = profile?.userRole

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (rejected) "Request not approved" else "Waiting for approval",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    state.hasError -> "Couldn't check your status. Check your connection and try again."
                    rejected -> "Your request for a ${role?.label.orEmpty()} account wasn't approved. " +
                        "Please contact your college if you think this is a mistake."
                    role != null -> "Your ${role.label} account has to be approved by " +
                        "${role.approverDescription} before you can use CampusConnect. " +
                        "Check back once they've reviewed it."
                    else -> "Checking your account…"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = viewModel::refresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check again")
                }
            }
            TextButton(
                onClick = {
                    viewModel.signOut()
                    onSignOut()
                }
            ) {
                Text("Log out")
            }
        }
    }
}
