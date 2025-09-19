package com.example.smartcalendar.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.smartcalendar.presentation.calendar.CalendarScreen
import com.example.smartcalendar.presentation.calendar.CalendarPresenter
import com.example.smartcalendar.data.repo.RoomEventRepository
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = RoomEventRepository(this)
        val presenter = CalendarPresenter(repo)

        setContent {
            MaterialTheme {
                Surface {
                    CalendarScreen(presenter)
                }
            }
        }
    }
}