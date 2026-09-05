package com.example.campusconnect.screens.profilescreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },

                navigationIcon = { Icon(Icons.Default.KeyboardArrowLeft , contentDescription = "back button") },
                actions = {
                    Icon(Icons.Default.Person , "person")
                },

                )
        }
    ) {
            innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Text("Satyam")
        }
    }

}