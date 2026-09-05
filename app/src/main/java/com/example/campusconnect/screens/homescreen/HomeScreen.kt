package com.example.campusconnect.screens.homescreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.screens.homescreennavigation.HomeScreenNavGraph

@Composable
fun HomeScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {

    val navController = rememberNavController()
    Scaffold(
        bottomBar = { HomeBottomNavigationBar(navController) }
    ) { innerPadding ->
        // Each destination inside HomeScreenNavGraph owns its own TopAppBar/Scaffold,
        // so we only need to reserve the bottom-bar inset here, not repaint the content.
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            HomeScreenNavGraph(
                navController = navController,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}
