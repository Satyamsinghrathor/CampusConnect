package com.example.campusconnect.screens.profilesetupscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GateStatus { Checking, ProfileComplete, ProfileMissing, SignedOut, Error }

class ProfileGateViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _status = MutableStateFlow(GateStatus.Checking)
    val status: StateFlow<GateStatus> = _status.asStateFlow()

    init {
        check()
    }

    /** Looks up users/{uid}: a document means the profile is complete. */
    fun check() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _status.value = GateStatus.SignedOut
            return
        }
        viewModelScope.launch {
            _status.value = GateStatus.Checking
            userRepository.getProfile(uid)
                .onSuccess { profile ->
                    _status.value = if (profile != null) {
                        GateStatus.ProfileComplete
                    } else {
                        GateStatus.ProfileMissing
                    }
                }
                .onFailure { _status.value = GateStatus.Error }
        }
    }

    fun signOut() {
        auth.signOut()
        _status.value = GateStatus.SignedOut
    }
}

/**
 * Shown right after authentication. Decides where the user goes next:
 * Dashboard when users/{uid} exists, Profile Setup when it doesn't.
 */
@Composable
fun ProfileGateScreen(
    onProfileComplete: () -> Unit,
    onProfileMissing: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ProfileGateViewModel = viewModel()
) {
    val status by viewModel.status.collectAsState()

    LaunchedEffect(status) {
        when (status) {
            GateStatus.ProfileComplete -> onProfileComplete()
            GateStatus.ProfileMissing -> onProfileMissing()
            GateStatus.SignedOut -> onSignedOut()
            else -> Unit
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (status == GateStatus.Error) {
                Text(
                    text = "Couldn't check your profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Check your internet connection and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = viewModel::check) { Text("Retry") }
                TextButton(onClick = viewModel::signOut) { Text("Log out") }
            } else {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Getting things ready…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
