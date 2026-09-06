package com.example.campusconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.campusconnect.navigation.NavGraph
import com.example.campusconnect.screens.assignmentscreen.AssignmentsScreen
import com.example.campusconnect.screens.resourcescreen.ResourcesScreen
import com.example.campusconnect.screens.timetablescreen.TimetableScreen
import com.example.campusconnect.ui.theme.CampusConnectTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CampusConnectApp()
//            TimetableScreen()
//            AssignmentsScreen()
//            ResourcesScreen()
        }
    }
}

@Composable
fun CampusConnectApp() {

    // This is a @Composable function, so we can call it here
    val systemDarkTheme = isSystemInDarkTheme()

    // Remember the user's manual choice
    var isDarkTheme by rememberSaveable {
        mutableStateOf(systemDarkTheme)
    }

    CampusConnectTheme(
        darkTheme = isDarkTheme
    ) {
        NavGraph(
            darkTheme = isDarkTheme,
            onToggleTheme = {
                isDarkTheme = !isDarkTheme
            }
        )
    }
}