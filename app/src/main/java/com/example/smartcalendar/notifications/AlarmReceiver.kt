package com.example.smartcalendar.notifications

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.smartcalendar.data.model.RepeatType
import com.example.smartcalendar.data.repo.RoomEventRepository
import com.example.smartcalendar.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Событие"
        val id = intent.getLongExtra("event_id", 0L).toInt()
        val start = intent.getLongExtra("startMillis", 0L)
        val end = intent.getLongExtra("endMillis", 0L)
        val channelId = "smartcalendar_events"

        // Форматируем интервал времени, например "Сегодня, 19:00–20:00"
        val whenText = formatWhen(start, end)

        // PendingIntent для открытия приложения по тапу
        val contentIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(Intent(context, MainActivity::class.java))
            getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)   // можно заменить на свой
            .setContentTitle(title)
            .setContentText(whenText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(whenText))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Открыть", contentIntent)            // кнопка действия
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        NotificationManagerCompat.from(context).notify(id, notification)

        CoroutineScope(Dispatchers.IO).launch {
            val repo = RoomEventRepository(context)   // поправь, если имя у тебя другое
            val e = repo.getById(id.toLong()) ?: return@launch
            if (e.repeatType != RepeatType.NONE) {
                ReminderScheduler.schedule(
                    context = context,
                    eventId = e.id,
                    title = e.title,
                    startMillis = e.startMillis,
                    endMillis = e.endMillis,
                    minutesBefore = e.reminderMinutes,   // ← правильное имя параметра
                    repeatType = e.repeatType,
                    repeatInterval = e.repeatInterval,
                    repeatUntilMillis = e.repeatUntilMillis,
                    repeatDaysMask = e.repeatDaysMask
                )
            }
        }
    }

    private fun formatWhen(startMillis: Long, endMillis: Long): String {
        return try {
            val zone = ZoneId.systemDefault()
            val start = Instant.ofEpochMilli(startMillis).atZone(zone)
            val end = Instant.ofEpochMilli(endMillis).atZone(zone)

            val today = LocalDate.now(zone)
            val dayLabel = when (start.toLocalDate()) {
                today -> "Сегодня"
                today.plusDays(1) -> "Завтра"
                else -> DateTimeFormatter.ofPattern("d MMMM", Locale("ru")).format(start)
            }
            val tf = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            "$dayLabel, ${tf.format(start)}–${tf.format(end)}"
        } catch (_: Throwable) {
            // на всякий случай fall-back
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(startMillis)
        }
    }
}
