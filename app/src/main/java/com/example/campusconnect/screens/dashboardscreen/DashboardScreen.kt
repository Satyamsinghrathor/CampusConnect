package com.example.campusconnect.screens.dashboardscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/** Simple in-memory placeholder until events come from a real data source. */
private data class DashboardEvent(val name: String, val date: String)

private val sampleUpcomingEvents = listOf(
    DashboardEvent("College Fest", "2 Oct 2026"),
    DashboardEvent("Hackathon Finals", "9 Oct 2026"),
    DashboardEvent("Sports Meet", "15 Oct 2026"),
    DashboardEvent("Alumni Talk", "21 Oct 2026"),
    DashboardEvent("Cultural Night", "30 Oct 2026")
)

@Composable
fun DashboardScreen(navController: NavHostController) {
    Scaffold(
        topBar = { DashboardTopAppBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Good Morning, Satyam",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(24.dp))

            LatestNotice(
                title = "Internal Test Next Week",
                date = "Posted 2 days ago"
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Upcoming Events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(sampleUpcomingEvents) { event ->
                    UpcomingEventCard(
                        eventName = event.name,
                        date = event.date,
                        modifier = Modifier
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
