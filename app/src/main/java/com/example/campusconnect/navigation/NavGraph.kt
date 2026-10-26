package com.example.campusconnect.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.screens.homescreen.HomeScreen
import com.example.campusconnect.screens.loginsignupscreen.LoginScreen
import com.example.campusconnect.screens.loginsignupscreen.RegisterScreen
import com.example.campusconnect.screens.profilesetupscreen.ProfileGateScreen
import com.example.campusconnect.screens.profilesetupscreen.ProfileSetupScreen

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
                    navController.navigate(NavRoutes.ProfileGate) {
                        popUpTo(0) {
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
                    navController.navigate(NavRoutes.ProfileGate) {
                        popUpTo(0) {
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

        composable<NavRoutes.ProfileGate> {
            ProfileGateScreen(
                onProfileComplete = {
                    navController.navigate(NavRoutes.Home) {
                        popUpTo<NavRoutes.ProfileGate> {
                            inclusive = true
                        }
                    }
                },
                onProfileMissing = {
                    navController.navigate(NavRoutes.ProfileSetup) {
                        popUpTo<NavRoutes.ProfileGate> {
                            inclusive = true
                        }
                    }
                },
                onSignedOut = {
                    navController.navigate(NavRoutes.Login) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<NavRoutes.ProfileSetup> {
            ProfileSetupScreen(
                onProfileSaved = {
                    navController.navigate(NavRoutes.Home) {
                        popUpTo<NavRoutes.ProfileSetup> {
                            inclusive = true
                        }
                    }
                },
                onSignOut = {
                    navController.navigate(NavRoutes.Login) {
                        popUpTo(0) {
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
