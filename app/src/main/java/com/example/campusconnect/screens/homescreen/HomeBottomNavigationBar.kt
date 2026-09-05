package com.example.campusconnect.screens.homescreen

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.campusconnect.navigation.NavRoutes

@Composable
fun HomeBottomNavigationBar(navController: NavHostController ) {

    val navItems = listOf(
        NavItem("Home", Icons.Default.Home, NavRoutes.Dashboard),
        NavItem(
            "Events",
            Icons.Default.CalendarMonth,
            NavRoutes.Event
        ),
        NavItem(
            "Notices",
            Icons.Default.Announcement,
            NavRoutes.Notice
        ),
        NavItem(
            "Profile",
            Icons.Default.Person,
            NavRoutes.Profile
        )
    )

    val backStackEntry by navController.currentBackStackEntryAsState()

    NavigationBar {

        navItems.forEach {
            navItem ->
            val isSelected =
                backStackEntry?.destination?.route ==
                        navItem.routes::class.qualifiedName

            Log.d("seleceted name", "HomeBottomNavigationBar: ${navItem.routes::class.qualifiedName}")
            NavigationBarItem(
                selected = isSelected,
                onClick =  {
                    navController.navigate(navItem.routes) {

                        popUpTo(
                            navController.graph.findStartDestination().id
                        ) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(navItem.icon , contentDescription = navItem.title) },
                label = { Text(navItem.title) }
            )

        }
    }
}


data class NavItem(
    val title: String,
    val icon: ImageVector,
    val routes: NavRoutes
)