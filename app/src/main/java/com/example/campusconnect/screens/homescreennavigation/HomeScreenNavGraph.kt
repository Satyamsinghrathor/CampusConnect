package com.example.campusconnect.screens.homescreennavigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.campusconnect.navigation.NavRoutes
import com.example.campusconnect.screens.dashboardscreen.DashboardScreen
import com.example.campusconnect.screens.eventscreen.EventDetailScreen
import com.example.campusconnect.screens.eventscreen.EventScreen
import com.example.campusconnect.screens.noticescreen.NoticeDetailScreen
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

        composable<NavRoutes.EventDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<NavRoutes.EventDetail>()
            EventDetailScreen(navController = navController, eventId = route.eventId)
        }

        composable<NavRoutes.Profile> {
            ProfileScreen(
                navController = navController,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                rootNavController = rootNavController
            )
        }

        composable<NavRoutes.NoticeDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<NavRoutes.NoticeDetail>()
            NoticeDetailScreen(navController = navController, noticeId = route.noticeId)
        }

        composable<NavRoutes.Notice> {
            NoticeScreen(navController)
        }
    }
}