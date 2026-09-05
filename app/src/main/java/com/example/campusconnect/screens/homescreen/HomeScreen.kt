package com.example.campusconnect.screens.homescreen

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.screens.homescreennavigation.HomeScreenNavGraph

@Composable
fun HomeScreen() {

    val navController = rememberNavController()
    Scaffold(
        bottomBar = { HomeBottomNavigationBar(navController) }
    ) { innerPadding ->

        HomeScreenNavGraph(navController)


    }
}