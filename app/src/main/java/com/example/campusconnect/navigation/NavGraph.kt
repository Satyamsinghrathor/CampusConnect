package com.example.campusconnect.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.screens.homescreen.HomeScreen
import com.example.campusconnect.screens.loginsignupscreen.LoginScreen
import com.example.campusconnect.ui.screens.register.SignUpScreen

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


        composable< NavRoutes.Login> {

            LoginScreen(
                onLoginClick = { email, password ->
                    navController.navigate(NavRoutes.Home) {
                        popUpTo< NavRoutes.Login> {
                            inclusive = true
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate(NavRoutes.Register)
                }
            )
        }

        composable< NavRoutes.Register> {
            SignUpScreen(
                onRegisterClick = { name, email, password ->

                    // Registration logic here

                    navController.navigate(NavRoutes.Home) {
                        popUpTo< NavRoutes.Register> {
                            inclusive = true
                        }
                    }
                },
                onLoginClick = {
                    navController.navigate(NavRoutes.Login)
                }
            )
        }
        composable<NavRoutes.Home> {
            HomeScreen(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}
