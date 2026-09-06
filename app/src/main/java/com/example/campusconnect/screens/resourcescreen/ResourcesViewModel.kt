package com.example.campusconnect.screens.resourcescreen


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.campusconnect.data.model.Resource

data class ResourcesUiState(
    val resources: List<Resource> = emptyList(),
    val isLoading: Boolean = false
)

class ResourcesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        ResourcesUiState(
            resources = listOf(
                Resource(
                    id = 1,
                    title = "DBMS Notes",
                    subject = "DBMS",
                    type = "PDF",
                    description = "Complete database management system notes.",
                    url = "https://example.com/dbms"
                ),
                Resource(
                    id = 2,
                    title = "Data Structures Notes",
                    subject = "Data Structures",
                    type = "PDF",
                    description = "Notes covering arrays, linked lists, trees and graphs.",
                    url = "https://example.com/dsa"
                ),
                Resource(
                    id = 3,
                    title = "Android Development Tutorial",
                    subject = "MAD",
                    type = "Video",
                    description = "Jetpack Compose and Android development tutorial.",
                    url = "https://example.com/android"
                ),
                Resource(
                    id = 4,
                    title = "Computer Networks Question Bank",
                    subject = "Computer Networks",
                    type = "Document",
                    description = "Important questions for exam preparation.",
                    url = "https://example.com/cn"
                )
            )
        )
    )

    val uiState: StateFlow<ResourcesUiState> = _uiState.asStateFlow()

    fun filterResources(subject: String) {
        // Can later be connected to a repository/database.
    }
}