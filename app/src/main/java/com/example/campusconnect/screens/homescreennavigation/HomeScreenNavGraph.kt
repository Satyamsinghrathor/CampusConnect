package com.example.campusconnect.screens.homescreennavigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.campusconnect.navigation.NavRoutes
import com.example.campusconnect.screens.dashboardscreen.DashboardScreen
import com.example.campusconnect.screens.eventscreen.EventScreen
import com.example.campusconnect.screens.noticescreen.NoticeScreen
import com.example.campusconnect.screens.profilescreen.ProfileScreen

@Composable
fun HomeScreenNavGraph(
    navController: NavHostController,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    rootNavController: NavHostController
) {

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Dashboard
    ) {

        composable<NavRoutes.Dashboard> {
            DashboardScreen(navController)
        }

        composable<NavRoutes.Event> {
            EventScreen(navController)
        }

        composable<NavRoutes.Profile> {
            ProfileScreen(
                navController = navController,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                rootNavController = rootNavController
            )
        }

        composable<NavRoutes.Notice> {
            NoticeScreen(navController)
        }
    }
}
