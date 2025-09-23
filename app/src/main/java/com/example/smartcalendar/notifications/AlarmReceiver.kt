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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_OPEN = "reminder.OPEN"
        const val ACTION_SNOOZE_10 = "reminder.SNOOZE_10"
        const val ACTION_SNOOZE_30 = "reminder.SNOOZE_30"
        const val ACTION_DONE = "reminder.DONE"

        const val ACTION_EVENTS_CHANGED = "com.example.smartcalendar.ACTION_EVENTS_CHANGED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("event_id", -1L).toInt()
        val title = intent.getStringExtra("title") ?: "Событие"

        when (intent.action) {
            ACTION_SNOOZE_10 -> {
                val id = intent.getLongExtra("event_id", -1L)
                if (id > 0) snoozeEvent(context, id, 10)
                return
            }

            ACTION_SNOOZE_30 -> {
                val id = intent.getLongExtra("event_id", -1L)
                if (id > 0) snoozeEvent(context, id, 30)
                return
            }

            ACTION_DONE -> {
                val id = intent.getLongExtra("event_id", -1L)
                NotificationManagerCompat.from(context).cancel(id.toInt())
                return
            }
        }





//        val title = intent.getStringExtra("title") ?: "Событие"
//        val id = intent.getLongExtra("event_id", 0L).toInt()
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
            .addAction(0, "Отложить 10м", actionPi(context, ACTION_SNOOZE_10, id.toLong(), title))
            .addAction(0, "Отложить 30м", actionPi(context, ACTION_SNOOZE_30, id.toLong(), title))
            .addAction(0, "Готово", actionPi(context, ACTION_DONE, id.toLong(), title))// кнопка действия
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
    private fun actionPi(context: Context, action: String, id: Long, title: String) =
        PendingIntent.getBroadcast(
            context,
            (id.toInt() * 10 + action.hashCode() and 0x7fffffff),
            Intent(context, AlarmReceiver::class.java).apply {
                this.action = action
                putExtra("event_id", id)
                putExtra("title", title)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun snoozeEvent(context: Context, eventId: Long, minutes: Int) {
        val nm = NotificationManagerCompat.from(context)

        GlobalScope.launch(Dispatchers.IO) {
            val repo = RoomEventRepository(context)   // подставь свой репозиторий, если имя иное
            val e = repo.getById(eventId) ?: run {
                withContext(Dispatchers.Main) { nm.cancel(eventId.toInt()) }
                return@launch
            }

            val delta = minutes * 60_000L

            if (e.repeatType == RepeatType.NONE) {
                // ⬅️ Сдвигаем САМО событие: и начало, и конец + delta
                val moved = e.copy(
                    startMillis = e.startMillis + delta,
                    endMillis   = e.endMillis   + delta
                )
                repo.save(moved)

                // Переставляем обычное напоминание относительно НОВОГО старта
                runCatching { ReminderScheduler.cancel(context, moved.id) }
                runCatching {
                    ReminderScheduler.schedule(
                        context       = context,
                        eventId       = moved.id,
                        title         = moved.title,
                        startMillis   = moved.startMillis,
                        endMillis     = moved.endMillis,
                        minutesBefore = moved.reminderMinutes
                    )
                }
            } else {
                // Повторяющиеся: саму серию не трогаем, ставим разовый snooze-триггер
                val dur = e.endMillis - e.startMillis
                val oneShotStart = System.currentTimeMillis() + delta
                runCatching {
                    ReminderScheduler.schedule(
                        context       = context,
                        eventId       = e.id,
                        title         = e.title,
                        startMillis   = oneShotStart,
                        endMillis     = oneShotStart + dur,
                        minutesBefore = 0  // сработать ровно через N минут
                    )
                }
            }

            // Закрыть уведомление
            withContext(Dispatchers.Main) { nm.cancel(eventId.toInt()) }

            // 🔔 Сообщить UI, чтобы он обновил список
            context.sendBroadcast(
                Intent(ACTION_EVENTS_CHANGED).apply {
                    `package` = context.packageName   // ограничиваем рассылку только нашим приложением
                }
            )
        }
    }
}
