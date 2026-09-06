package com.example.campusconnect.data.model



data class Assignment(
    val id: Int,
    val title: String,
    val subject: String,
    val dueDate: String,
    val description: String = "",
    val isCompleted: Boolean = false
)