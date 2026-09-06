package com.example.campusconnect.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.screens.homescreen.HomeScreen
import com.example.campusconnect.screens.loginsignupscreen.LoginScreen
import com.example.campusconnect.screens.loginsignupscreen.RegisterScreen

@Composable
fun NavGraph(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Login
    ) {


        composable<NavRoutes.Login> {

            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Home) {
                        popUpTo<NavRoutes.Login> {
                            inclusive = true
                        }
                    }
                },
                onSignupClick = {
                    navController.navigate(NavRoutes.Register)
                }
            )
        }

        composable<NavRoutes.Register> {

            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.Home) {
                        popUpTo<NavRoutes.Register> {
                            inclusive = true
                        }
                    }
                },
                onLoginClick = {
                    navController.navigate(NavRoutes.Login) {
                        popUpTo<NavRoutes.Register> {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<NavRoutes.Home> {
            HomeScreen(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                rootNavController = navController
            )
        }
    }
}
