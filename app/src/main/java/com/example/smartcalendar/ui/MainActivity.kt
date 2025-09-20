package com.example.smartcalendar.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartcalendar.data.repo.RoomEventRepository
import com.example.smartcalendar.data.settings.SettingsRepository
import com.example.smartcalendar.notifications.ReminderScheduler
import com.example.smartcalendar.presentation.calendar.CalendarPresenter
import com.example.smartcalendar.presentation.calendar.CalendarScreen
import com.example.smartcalendar.ui.theme.SmartCalendarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureNotificationChannel()
        requestPostNotificationsIfNeeded()
        requestExactAlarmPermissionIfNeeded()

        val repo = RoomEventRepository(this)
        val settings = SettingsRepository(this)

        val presenter = CalendarPresenter(
            repo = repo,
            getDefaultReminderMinutes = { settings.getDefaultReminderMinutesOnce() },
            scheduleReminder = { id, title, start, end, minutes ->
                ReminderScheduler.schedule(this, id, title, start, end, minutesBefore = minutes)
            },
            cancelReminder = { id ->
                ReminderScheduler.cancel(this, id)
            }
        )
        setContent {
            SmartCalendarTheme {
                Surface {
                    CalendarScreen(presenter)
                }
            }
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm: NotificationManager = getSystemService(NotificationManager::class.java)
            val id = "smartcalendar_events"
            val channel = NotificationChannel(
                id,
                "События календаря",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)

            // Если уже разрешено – выходим
            if (alarmManager.canScheduleExactAlarms()) return

            // Иначе открываем системное окно
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}
