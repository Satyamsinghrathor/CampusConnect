package com.example.campusconnect.screens.assignmentscreen


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.campusconnect.data.model.Assignment

data class AssignmentsUiState(
    val assignments: List<Assignment> = emptyList(),
    val isLoading: Boolean = false
)

class AssignmentsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        AssignmentsUiState(
            assignments = listOf(
                Assignment(
                    id = 1,
                    title = "Database Design",
                    subject = "DBMS",
                    dueDate = "10 Sep 2026",
                    description = "Design an ER diagram for the given system."
                ),
                Assignment(
                    id = 2,
                    title = "Sorting Algorithms",
                    subject = "Data Structures",
                    dueDate = "12 Sep 2026",
                    description = "Implement and analyze sorting algorithms."
                ),
                Assignment(
                    id = 3,
                    title = "Android UI Design",
                    subject = "MAD",
                    dueDate = "15 Sep 2026",
                    description = "Create a responsive Compose UI."
                )
            )
        )
    )

    val uiState: StateFlow<AssignmentsUiState> = _uiState.asStateFlow()

    fun toggleAssignment(id: Int) {
        _uiState.value = _uiState.value.copy(
            assignments = _uiState.value.assignments.map { assignment ->
                if (assignment.id == id) {
                    assignment.copy(
                        isCompleted = !assignment.isCompleted
                    )
                } else {
                    assignment
                }
            }
        )
    }
}