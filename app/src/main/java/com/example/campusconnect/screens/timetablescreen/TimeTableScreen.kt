package com.example.campusconnect.screens.timetablescreen


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ClassPeriod(
    val subject: String,
    val faculty: String,
    val room: String,
    val startTime: String,
    val endTime: String
)

@Composable
fun TimetableScreen() {

    val days = listOf(
        "Mon",
        "Tue",
        "Wed",
        "Thu",
        "Fri",
        "Sat"
    )

    var selectedDay by remember {
        mutableStateOf("Mon")
    }

    val timetable = mapOf(
        "Mon" to listOf(
            ClassPeriod(
                "Data Structures",
                "Dr. Sharma",
                "Room 204",
                "9:00 AM",
                "10:00 AM"
            ),
            ClassPeriod(
                "Operating Systems",
                "Prof. Kumar",
                "Lab 1",
                "10:15 AM",
                "11:15 AM"
            ),
            ClassPeriod(
                "Database Management",
                "Dr. Mehta",
                "Room 301",
                "12:00 PM",
                "1:00 PM"
            )
        ),

        "Tue" to listOf(
            ClassPeriod(
                "Computer Networks",
                "Prof. Singh",
                "Room 205",
                "9:00 AM",
                "10:00 AM"
            ),
            ClassPeriod(
                "Android Development",
                "Dr. Verma",
                "Lab 2",
                "10:15 AM",
                "11:15 AM"
            )
        ),

        "Wed" to listOf(
            ClassPeriod(
                "Artificial Intelligence",
                "Dr. Rao",
                "Room 302",
                "9:00 AM",
                "10:00 AM"
            ),
            ClassPeriod(
                "Data Structures",
                "Dr. Sharma",
                "Room 204",
                "11:00 AM",
                "12:00 PM"
            )
        ),

        "Thu" to listOf(
            ClassPeriod(
                "Operating Systems",
                "Prof. Kumar",
                "Room 205",
                "9:00 AM",
                "10:00 AM"
            ),
            ClassPeriod(
                "Database Management",
                "Dr. Mehta",
                "Room 301",
                "10:15 AM",
                "11:15 AM"
            )
        ),

        "Fri" to listOf(
            ClassPeriod(
                "Computer Networks",
                "Prof. Singh",
                "Room 205",
                "9:00 AM",
                "10:00 AM"
            ),
            ClassPeriod(
                "Android Development",
                "Dr. Verma",
                "Lab 2",
                "11:00 AM",
                "1:00 PM"
            )
        ),

        "Sat" to listOf(
            ClassPeriod(
                "Project Work",
                "Dr. Sharma",
                "Lab 1",
                "9:00 AM",
                "11:00 AM"
            )
        )
    )

    Scaffold() {
        innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Text(
                text = "Timetable",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your weekly class schedule",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                days.forEach { day ->

                    DayButton(
                        day = day,
                        selected = selectedDay == day,
                        onClick = {
                            selectedDay = day
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val classes = timetable[selectedDay] ?: emptyList()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(classes) { classPeriod ->

                    ClassCard(classPeriod)
                }
            }
        }
    }
}