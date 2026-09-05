package com.example.campusconnect.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.screens.homescreen.HomeScreen

@Composable
fun NavGraph(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home
    ) {

        composable<NavRoutes.Home> {
            HomeScreen(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}
