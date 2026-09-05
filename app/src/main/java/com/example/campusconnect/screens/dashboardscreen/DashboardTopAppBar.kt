package com.example.campusconnect.screens.dashboardscreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopAppBar(modifier: Modifier = Modifier) {


    TopAppBar(
        title = { Text("Campus Connect") },
        navigationIcon = {
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Localized description"
                )
            }
        },

        actions = {
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = "icon button")
            }
        }

    )
}