package com.example.campusconnect.screens.homescreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.campusconnect.navigation.NavRoutes

@Composable
fun HomeBottomNavigationBar(navController: NavHostController) {

    val navItems = listOf(
        NavItem("Home", Icons.Default.Home, NavRoutes.Dashboard),
        NavItem("Events", Icons.Default.CalendarMonth, NavRoutes.Event),
        NavItem("Notices", Icons.Default.Announcement, NavRoutes.Notice),
        NavItem("Profile", Icons.Default.Person, NavRoutes.Profile)
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        navItems.forEach { navItem ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == navItem.routes::class.qualifiedName
            } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(navItem.routes) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(navItem.icon, contentDescription = navItem.title) },
                label = { Text(navItem.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val routes: NavRoutes
)
