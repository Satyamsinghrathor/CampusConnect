package com.example.campusconnect.screens.dashboardscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController


@Composable
 fun DashboardScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            DashboardTopAppBar()
        }
    ) {
        innerPadding ->

        Column(modifier = Modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text("Good Morning , Satyam")

            Spacer(Modifier.height(30.dp))

            LatestNotice()

            Spacer(Modifier.height(30.dp))

            Text("Upcoming Events")
            Spacer(Modifier.height(30.dp))

            LazyRow() {
                items(5){

                    UpcomingEventCard("College Fest" , "2 Oct 2026" , Modifier)

                }
            }

        }


    }
}